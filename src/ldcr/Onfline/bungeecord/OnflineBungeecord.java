package ldcr.Onfline.bungeecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class OnflineBungeecord extends Plugin {
	public static OnflineBungeecord instance;
	protected static boolean works = false;
	private static SessionManager session;

	private String mysqlServer;
	private String mysqlPort;
	private String mysqlDatabase;
	private String mysqlUser;
	private String mysqlPassword;

	protected String alreadyPremium;
	protected String requestKickMessage;

	private static CommandSender console;

	protected Configuration config;
	protected ConfigurationProvider cProvider;
	@Override
	public void onEnable() {
		instance = this;
		console = getProxy().getConsole();
		getDataFolder().mkdirs();
		if (!reload()) {
			log("&c加载失败, 请检查报错");
		}
		getProxy().getPluginManager().registerListener(this, new ConnectListener());
		getProxy().getPluginManager().registerCommand(this, new BungeeOnflineCommand());
		getProxy().registerChannel("OnflineBungeecord");
	}
	public boolean reload() {
		works = false;
		if (!loadConfig()) {
			works = false;
			log("&c配置文件加载失败");
			return false;
		}
		if (session!=null) {
			session.disconnect();
		}
		try {
			session = new SessionManager(mysqlServer, mysqlPort, mysqlDatabase, mysqlUser, mysqlPassword);
		} catch (final SQLException e) {
			log("&c连接数据库失败.");
			return false;
		}
		works = true;
		log("&b欢迎使用 Onfline 正版认证系统~");
		return true;
	}
	public static SessionManager getSession() {
		return session;
	}
	private boolean loadConfig() {
		final File config = new File(getDataFolder(), "config.yml");
		if (!config.exists()) {
			try {
				final InputStream in = getResourceAsStream("config.yml");
				final BufferedInputStream bin = new BufferedInputStream(in);
				final FileOutputStream fout = new FileOutputStream(config);
				final BufferedOutputStream bout = new BufferedOutputStream(fout);
				final byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = bin.read(buffer)) >= 0) {
					bout.write(buffer, 0, bytesRead);
				}
				bout.flush();
				fout.flush();
				bin.close();
				in.close();
				bout.close();
				fout.close();
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
			log("&e配置文件不存在... 判断为第一次启动, 请修改配置文件数据库地址");
			return false;
		}
		cProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
		try {
			this.config = cProvider.load(config);
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		mysqlServer = this.config.getString("mysql.server","localhost");
		mysqlPort = this.config.getString("mysql.port","3306");
		mysqlDatabase = this.config.getString("mysql.database","luckyprefix");
		mysqlUser = this.config.getString("mysql.user","root");
		mysqlPassword = this.config.getString("mysql.password","password");

		alreadyPremium = this.config.getString("alreadyPremium","&b&l正版认证 &7>> &a你已经认证正版了~").replace('&', '§').replace("§§", "&");
		requestKickMessage = this.config.getString("requestKickMessage", "&a&l请您以&c&l&n正版登录&a&l的方式重新登录服务器\n\n   &7(把本服务器当作一个正版服来连接)").replace('&', '§').replace("§§", "&");
		return true;
	}
	public static void log(final String... messages) {
		for (final String message : messages) {
			console.sendMessage(new TextComponent("§b§lOnfline §7>> "+message.replace('&', '§').replace("§§", "&")));
		}
	}
}
