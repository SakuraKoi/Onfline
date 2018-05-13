package ldcr.Onfline.bungeecord;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;

public class SessionManager {
	private MysqlConnection conn = null;
	private final HashMap<String, LoginSource> premiumMap = new HashMap<String, LoginSource>();
	private final HashSet<InetAddress> requestCheck = new HashSet<InetAddress>();
	private final HashSet<InetAddress> waitingCheck = new HashSet<InetAddress>();
	private final HashMap<ProxiedPlayer, Boolean> waitingPremium = new HashMap<ProxiedPlayer, Boolean>();
	private final String USER_TABLE_NAME = "Onfline_user";
	public SessionManager(final String mysqlServer, final String mysqlPort, final String mysqlDatabase, final String mysqlUser, final String mysqlPassword) throws SQLException {
		if (conn!=null) {
			disconnect();
		}
		OnflineBungeecord.log("&a正在连接Mysql数据库 "+mysqlServer+":"+mysqlPort+" ...");
		conn = new MysqlConnection(mysqlServer, mysqlUser, mysqlPort, mysqlPassword, mysqlDatabase);
		if (conn.isConnection()) {
			conn.createTable(USER_TABLE_NAME, "player", "premium", "uuid");
		} else throw new SQLException("Failed connect Database");
	}
	public void disconnect() {
		if (conn!=null) {
			if (conn.isConnection()) {
				OnflineBungeecord.log("&e正在关闭数据库连接...");
				conn.closeConnection();
				OnflineBungeecord.log("&a已与数据库断线.");
			}
		}
	}
	public void requestCheckPremium(final ProxiedPlayer player) {
		final LoginSource session = getSession(player);
		if (session.isPermium()) {
			player.sendMessage(new TextComponent(OnflineBungeecord.instance.alreadyPremium));
			return;
		}
		OnflineBungeecord.log("&d玩家 "+player.getName()+" 请求认证正版");
		requestCheck.add(player.getAddress().getAddress());
		player.disconnect(new TextComponent(OnflineBungeecord.instance.requestKickMessage));
	}
	public boolean isRequestingPremium(final InetAddress address) {
		if (requestCheck.contains(address)) return true;
		return false;
	}
	public void startCheckPremium(final InetAddress address) {
		requestCheck.remove(address);
		waitingCheck.add(address);
	}
	public boolean isCheckingPremium(final ProxiedPlayer player) {
		if (waitingCheck.contains(player.getAddress().getAddress())) return true;
		return false;
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
			final LinkedList<HashMap<String,Object>> datas = conn.getValues(USER_TABLE_NAME, "uuid", session.getUuid().toString(), "player", "premium");
			if (datas.size()>1) {
				for (final HashMap<String,Object> data : datas) {
					final Object playerObj = data.get("player");
					if (playerObj==null) {
						continue;
					}
					if (player.getName().toLowerCase().equals(playerObj.toString())) {
						continue;
					}
					if ("true".equals(data.get("premium").toString())) {
						BungeeChannelMessage.requestUnPremium(playerObj.toString());
						delSession(playerObj.toString());
						OnflineBungeecord.log("&e正版玩家 "+player.getName()+" 已改名, 删除旧ID ["+playerObj.toString()+"] 的正版权限...");
					}
				}
			}
			session.setPermium(true);
			updateSession(session);
			OnflineBungeecord.log("&b玩家 "+player.getName()+"[ "+getLoginSource(player).getUuid().toString()+" ] 认证正版成功");
		} else {
			OnflineBungeecord.log("&c玩家 "+player.getName()+" 认证正版失败");
		}
	}
	public void updateUUID(final InitialHandler connection) {
		final LoginSource session = getLoginSource(connection);
		session.setUuid(connection.getUniqueId());
		updateSession(session);
	}
	private void delSession(final String player) {
		premiumMap.remove(player.toLowerCase());
		conn.deleteValue(USER_TABLE_NAME, "player", player.toLowerCase());
	}
	private LoginSource getSession(final ProxiedPlayer connection) {
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
	public LoginSource getLoginSource(final InitialHandler connection) {
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
	public LoginSource getLoginSource(final ProxiedPlayer player) {
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
	public LoginSource getLoginSource(final String username) {
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
		if (conn.isExists(USER_TABLE_NAME, "player", session.getPlayer())) {
			conn.setValue(USER_TABLE_NAME, "player", session.getPlayer(), "premium", session.isPermium()? "true": "false");
			conn.setValue(USER_TABLE_NAME, "player", session.getPlayer(), "uuid", session.getUuid().toString());
		} else {
			conn.intoValue(USER_TABLE_NAME, session.getPlayer(), session.isPermium()? "true": "false", session.getUuid().toString());
		}
	}
	private LoginSource loadSession(final String username) {
		if (conn.isExists(USER_TABLE_NAME, "player", username)) {
			final HashMap<String,Object> result = conn.getValue(USER_TABLE_NAME, "player", username, "premium", "uuid");
			return new LoginSource(username, result.get("premium").toString().equals("true"), result.get("uuid").toString());
		}
		return null;
	}
}
