package ldcr.Onfline.spigot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpigotOnflineCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("§a§lOnfline §7>> §c仅玩家可以进行正版认证!");
			return true;
		}
		if (!sender.hasPermission("onfline.use")) {
			sender.sendMessage("§a§lOnfline §7>> §c你没有权限执行此命令");
			return true;
		}
		OnflineSpigot.instance.messageChannel.requestAuth((Player) sender);
		OnflineSpigot.log("&e玩家 "+sender.getName()+" 请求进行正版验证");
		return true;
	}

}
