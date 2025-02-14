package sakura.kooi.Onfline.bungeecord;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import sakura.kooi.Onfline.bungeecord.utils.LoginSource;
import sakura.kooi.Onfline.bungeecord.utils.mysql.MysqlDataSource;

public class SessionManager {
	private static final String USER_TABLE_NAME = "Onfline_user";

	private MysqlDataSource conn = null;
	private final HashMap<String, LoginSource> premiumMap = new HashMap<>();
	private final HashSet<InetAddress> requestCheck = new HashSet<>(); // TODO clear cache TODO check is same username
	private final HashSet<InetAddress> waitingCheck = new HashSet<>();
	private final HashMap<ProxiedPlayer, Boolean> waitingPremium = new HashMap<>();
	public SessionManager(final String mysqlServer, final String mysqlPort, final String mysqlDatabase, final String mysqlUser, final String mysqlPassword) throws SQLException {
		if (conn!=null) {
			disconnect();
		}
		OnflineBungeecord.log("&a正在连接Mysql数据库 "+mysqlServer+":"+mysqlPort+" ...");
		conn = new MysqlDataSource(mysqlServer, mysqlPort, mysqlUser, mysqlPassword, mysqlDatabase, OnflineBungeecord.getInstance());
		if (conn.isConnected()) {
			conn.createTable(USER_TABLE_NAME, "player", "premium", "uuid");
		} else throw new SQLException("Failed connect Database");
	}
	public void disconnect() {
		if (conn!=null && conn.isConnected()) {
			OnflineBungeecord.log("&e正在关闭数据库连接...");
			conn.disconnectDatabase();
			OnflineBungeecord.log("&a已与数据库断线.");
		}
	}
	public void requestCheckPremium(final ProxiedPlayer player) {
		LoginSource session;
		try {
			session = getSession(player);
		} catch (final SQLException e) {
			e.printStackTrace();
			player.sendMessage(new TextComponent("§b§lOnfline §7>> §c请求认证正版失败. 发生数据库错误, 请联系管理员."));
			OnflineBungeecord.log("&c玩家 "+player.getName()+" 请求认证正版失败, 发生数据库错误");
			return;
		}
		if (session.isPermium()) {
			player.sendMessage(new TextComponent(OnflineBungeecord.getInstance().getMessageAlreadyPremium()));
			return;
		}
		OnflineBungeecord.log("&d玩家 "+player.getName()+" 请求认证正版");
		requestCheck.add(player.getAddress().getAddress());
		player.disconnect(new TextComponent(OnflineBungeecord.getInstance().getMessageRequestKick()));
	}
	public boolean isRequestingPremium(final InetAddress address) {
		return requestCheck.contains(address);
	}
	public void startCheckPremium(final InetAddress address) {
		requestCheck.remove(address);
		waitingCheck.add(address);
	}
	public boolean isCheckingPremium(final ProxiedPlayer player) {
		return waitingCheck.contains(player.getAddress().getAddress());
	}
	public void failedCheckPremium(final ProxiedPlayer player) {
		waitingCheck.remove(player.getAddress().getAddress());
		waitingPremium.put(player, false);
	}
	public void passCheckPremium(final ProxiedPlayer player) {
		waitingCheck.remove(player.getAddress().getAddress());
		waitingPremium.put(player, true);
	}
	public void cancelUpdatePremium(final ProxiedPlayer player) {
		waitingPremium.remove(player);
	}
	public boolean isUpdatingPremium(final ProxiedPlayer player) {
		return waitingPremium.containsKey(player);
	}
	public boolean getUpdatingPremiumState(final ProxiedPlayer player) {
		return waitingPremium.get(player);
	}
	public void finishUpdatePremium(final ProxiedPlayer player) {
		if (!waitingPremium.containsKey(player)) return;
		final Boolean isSuccess = waitingPremium.get(player);
		waitingPremium.remove(player);
		if (isSuccess) {
			final LoginSource session = premiumMap.get(player.getName().toLowerCase());
			List<HashMap<String, Object>> datas;
			try {
				datas = conn.getValues(USER_TABLE_NAME, "uuid", session.getUuid().toString(), "player", "premium");
			} catch (final SQLException e) {
				e.printStackTrace();
				ChannelMessager.requestUnPremium(player.getServer(), player.getName());
				player.sendMessage(new TextComponent("§b§lOnfline §7>> §c认证正版失败. 发生数据库错误, 请联系管理员."));
				OnflineBungeecord.log("&c玩家 "+player.getName()+" 认证正版失败, 发生数据库错误");
				return;
			}
			if (datas.size()>1) {
				for (final HashMap<String,Object> data : datas) {
					final Object playerObj = data.get("player");
					if (playerObj==null || player.getName().equalsIgnoreCase(playerObj.toString())) {
						continue;
					}
					if ("true".equals(data.get("premium").toString())) {
						ChannelMessager.requestUnPremium(player.getServer(), playerObj.toString());
						delSession(playerObj.toString());
						OnflineBungeecord.log("&e正版玩家 "+player.getName()+" 已改名, 删除旧ID ["+playerObj.toString()+"] 的正版权限...");
					}
				}
			}
			session.setPermium(true);
			updateSession(session);
			OnflineBungeecord.log("&b玩家 "+player.getName()+"[ "+session.getUuid().toString()+" ] 认证正版成功");
		} else {
			OnflineBungeecord.log("&c玩家 "+player.getName()+" 认证正版失败");
		}
	}
	public void updateUUID(final InitialHandler connection) throws SQLException {
		final LoginSource session = getLoginSource(connection);
		session.setUuid(connection.getUniqueId());
		updateSession(session);
	}
	private void delSession(final String player) {
		premiumMap.remove(player.toLowerCase());
		try {
			conn.deleteValue(USER_TABLE_NAME, "player", player.toLowerCase());
		} catch (final SQLException e) {
			e.printStackTrace();
			OnflineBungeecord.log("&c发生数据库错误, 请检查日志");
		}
	}
	private LoginSource getSession(final ProxiedPlayer connection) throws SQLException {
		final String username = connection.getName();
		LoginSource session;
		if (premiumMap.containsKey(username.toLowerCase())) {
			session = premiumMap.get(username.toLowerCase());
		} else {
			session = loadSession(username);
			if (session==null) {
				session = new LoginSource(connection);
			}
			premiumMap.put(username.toLowerCase(), session);
		}
		return session;
	}
	public LoginSource getLoginSource(final InitialHandler connection) throws SQLException {
		final String username = connection.getLoginRequest().getData();
		LoginSource session;
		if (premiumMap.containsKey(username.toLowerCase())) {
			session = premiumMap.get(username.toLowerCase());
		} else {
			session = loadSession(username);
			if (session==null) {
				session = new LoginSource(connection);
			}
			premiumMap.put(username.toLowerCase(), session);
		}
		return session;
	}
	public LoginSource getLoginSource(final ProxiedPlayer player) throws SQLException {
		LoginSource session;
		final String username = player.getName().toLowerCase();
		if (premiumMap.containsKey(username)) {
			session = premiumMap.get(username);
		} else {
			session = loadSession(username);
			if (session==null) {
				session = new LoginSource(player);
			}
			premiumMap.put(username, session);
		}
		return session;
	}
	public LoginSource getLoginSource(final String username) throws SQLException {
		LoginSource session;
		if (premiumMap.containsKey(username)) {
			session = premiumMap.get(username);
		} else {
			session = loadSession(username);
			if (session==null) return null;
			premiumMap.put(username, session);
		}
		return session;
	}
	private void updateSession(final LoginSource session) {
		try {
			if (conn.isExists(USER_TABLE_NAME, "player", session.getPlayer())) {
				conn.setValue(USER_TABLE_NAME, "player", session.getPlayer(), "premium", session.isPermium()? "true": "false");
				conn.setValue(USER_TABLE_NAME, "player", session.getPlayer(), "uuid", session.getUuid().toString());
			} else {
				conn.intoValue(USER_TABLE_NAME, session.getPlayer(), session.isPermium()? "true": "false", session.getUuid().toString());
			}
		} catch (final SQLException e) {
			e.printStackTrace();
			OnflineBungeecord.log("&c发生数据库错误, 请检查日志");
		}
	}
	private LoginSource loadSession(final String username) throws SQLException {
		if (conn.isExists(USER_TABLE_NAME, "player", username)) {
			final Map<String,Object> result = conn.getValue(USER_TABLE_NAME, "player", username, "premium", "uuid");
			return new LoginSource(username, result.get("premium").toString().equals("true"), result.get("uuid").toString());
		}
		return null;
	}
}
