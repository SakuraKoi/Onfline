package ldcr.Onfline.bungeecord;

import ldcr.Onfline.bungeecord.utils.LoginSource;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BungeeOnflineCommand extends Command {
	public BungeeOnflineCommand() {
		super("onfline-admin");
	}

	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if (!sender.hasPermission("onfline.admin")) {
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §6§l欢迎使用 Onfline 正版认证 §bBy.Ldcr"));
			return;
		}
		if (args.length==0) {
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §6§l欢迎使用 Onfline 正版认证 §bBy.Ldcr"));
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §e/onfline-admin reload           重载 Onfline正版认证"));
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §e/onfline-admin info <Player>    查询正版玩家UUID"));
			return;
		}
		switch (args[0].toLowerCase()) {
		case "reload": {
			if (OnflineBungeecord.getInstance().reload()) {
				sender.sendMessage(new TextComponent("§b§lOnfline §7>> §a正版认证重载成功~"));
			} else {
				sender.sendMessage(new TextComponent("§b§lOnfline §7>> §c正版认证重载失败, 请检查后台报错"));
			}
			return;
		}
		case "info": {
			if (args.length<2) {
				sender.sendMessage(new TextComponent("§b§lOnfline §7>> §e/onfline-admin info <Player>    查询正版玩家UUID"));
				return;
			}
			final String player = args[1].toLowerCase();
			final LoginSource loginSource = OnflineBungeecord.getSessionManager().getLoginSource(player);
			if (loginSource == null) {
				sender.sendMessage(new TextComponent("§b§lOnfline §7>> §c玩家 "+player+" 不是正版登录的"));
				return;
			}
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §a玩家 "+player+" 的UUID是 §b"+loginSource.getUuid().toString()));
			break;
		}
		default: {
			sender.sendMessage(new TextComponent("§b§lOnfline §7>> §c无效命令"));
		}
		}
	}

}
