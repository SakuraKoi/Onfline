package ldcr.Onfline.spigot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class OnflineSpigot extends JavaPlugin {
	public static OnflineSpigot instance;
	public SpigotChannelMessage messageChannel;

	public String premiumTitle;
	public String premiumMessage;
	public String crackedTitle;
	public String crackedMessage;
	public List<String> permiumCommands;
	public List<String> unPremiumCommands;

	private static CommandSender console;
	@Override
	public void onEnable() {
		instance = this;
		console = Bukkit.getConsoleSender();
		messageChannel = new SpigotChannelMessage();
		getServer().getMessenger().registerOutgoingPluginChannel(this, "OnflineBungeecord");
		getServer().getMessenger().registerIncomingPluginChannel(this, "OnflineBungeecord", messageChannel);
		getCommand("onfline").setExecutor(new SpigotOnflineCommand());
		loadConfig();
		log("&b欢迎使用 Onfline 正版认证系统~");
	}
	public void loadConfig() {
		final File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			saveDefaultConfig();
			log("&e正在初始化配置文件...");
		}
		YamlConfiguration config;
		try {
			config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		premiumTitle = config.getString("PremiumTitle", "&a正版认证成功").replace('&', '§').replace("§§", "&");
		premiumMessage = config.getString("PremiumMessage", "&b&l正版认证 &7>> &b恭喜您已经成功认证正版~").replace('&', '§').replace("§§", "&");
		crackedTitle = config.getString("CrackedTitle", "&c正版认证失败").replace('&', '§').replace("§§", "&");
		crackedMessage = config.getString("CrackedMessage", "&b&l正版认证 &7>> &c正版认证失败 &7(可能的原因: 您当前使用盗版登录启动游戏)").replace('&', '§').replace("§§", "&");
		permiumCommands = config.getStringList("PremiumCommands");
		unPremiumCommands = config.getStringList("UnPremiumCommands");
	}
	public static void log(final String... messages) {
		for (final String message : messages) {
			console.sendMessage("§a§lOnfline §7>> "+message.replace('&', '§').replace("§§", "&"));
		}
	}
}
