package sakura.kooi.Onfline.spigot;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import sakura.kooi.Utils.Bukkit.command.CommandHandler;

public class SpigotOnflineCommand extends CommandHandler {

	public SpigotOnflineCommand() {
		super(OnflineSpigot.getInstance(), "§a§lOnfline");
	}

	@Override
	public void onCommand(final CommandSender sender, final String[] args) {
		if (!(sender instanceof Player)) {
			sendMessage(sender, "§c仅玩家可以进行正版认证!");
			return;
		}
		if (checkPermission(sender, "onfline.use")) return;
		OnflineSpigot.getInstance().getMessageChannel().requestAuth((Player) sender);
		OnflineSpigot.log("&e玩家 "+sender.getName()+" 请求进行正版验证");
	}

}
