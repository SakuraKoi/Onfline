package ldcr.Onfline.bungeecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import lombok.Cleanup;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class OnflineBungeecord extends Plugin {
	@Getter private static OnflineBungeecord instance;
	@Getter private static boolean isOnflineWorking = false;
	@Getter private static SessionManager sessionManager;

	private String mysqlServer;
	private String mysqlPort;
	private String mysqlDatabase;
	private String mysqlUser;
	private String mysqlPassword;

	@Getter private String messageAlreadyPremium;
	@Getter private String messageRequestKick;

	private static CommandSender console;

	protected Configuration config;
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
		isOnflineWorking = false;
		if (!loadConfig()) {
			isOnflineWorking = false;
			log("&c配置文件加载失败");
			return false;
		}
		if (sessionManager!=null) {
			sessionManager.disconnect();
		}
		try {
			sessionManager = new SessionManager(mysqlServer, mysqlPort, mysqlDatabase, mysqlUser, mysqlPassword);
		} catch (final SQLException e) {
			log("&c连接数据库失败.");
			return false;
		}
		isOnflineWorking = true;
		log("&b欢迎使用 Onfline 正版认证系统~");
		return true;
	}
	private boolean loadConfig() {
		final File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				@Cleanup final InputStream in = getResourceAsStream("config.yml");
				@Cleanup final BufferedInputStream bin = new BufferedInputStream(in);
				@Cleanup final FileOutputStream fout = new FileOutputStream(configFile);
				@Cleanup final BufferedOutputStream bout = new BufferedOutputStream(fout);
				final byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = bin.read(buffer)) >= 0) {
					bout.write(buffer, 0, bytesRead);
				}
				bout.flush();
				fout.flush();
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
			log("&e配置文件不存在... 判断为第一次启动, 请修改配置文件数据库地址");
			return false;
		}
		final ConfigurationProvider cProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
		try {
			config = cProvider.load(configFile);
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		mysqlServer = config.getString("mysql.server","localhost");
		mysqlPort = config.getString("mysql.port","3306");
		mysqlDatabase = config.getString("mysql.database","luckyprefix");
		mysqlUser = config.getString("mysql.user","root");
		mysqlPassword = config.getString("mysql.password","password");

		messageAlreadyPremium = config.getString("messageAlreadyPremium","&b&l正版认证 &7>> &a你已经认证正版了~").replace('&', '§').replace("§§", "&");
		messageRequestKick = config.getString("messageRequestKick", "&a&l请您以&c&l&n正版登录&a&l的方式重新登录服务器\n\n   &7(把本服务器当作一个正版服来连接)").replace('&', '§').replace("§§", "&");
		return true;
	}
	public static void log(final String... messages) {
		for (final String message : messages) {
			console.sendMessage(new TextComponent("§b§lOnfline §7>> "+message.replace('&', '§').replace("§§", "&")));
		}
	}
}
