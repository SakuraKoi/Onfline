package ldcr.Onfline.spigot.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import ldcr.Onfline.spigot.OnflineSpigot;
import ldcr.Utils.Bukkit.TitleUtils;

public class PremiumUpdater implements Runnable {
	private final Player player;
	private final boolean isPremium;
	public PremiumUpdater(final Player player, final boolean isPremium) {
		this.player = player;
		this.isPremium = isPremium;
	}

	@Override
	public void run() {
		if (isPremium) {
			TitleUtils.sendTitle(player, OnflineSpigot.instance.premiumTitle, "", 10, 100, 10);
			player.sendMessage(OnflineSpigot.instance.premiumMessage);
			Bukkit.getScheduler().runTask(OnflineSpigot.instance, new CommandCaller(player.getName(), OnflineSpigot.instance.permiumCommands));
		} else {
			TitleUtils.sendTitle(player, OnflineSpigot.instance.crackedTitle, "", 10, 100, 10);
			player.sendMessage(OnflineSpigot.instance.crackedMessage);
		}
		OnflineSpigot.instance.messageChannel.replyUpdated(player);
		OnflineSpigot.log("&a已向玩家 "+player.getName()+" 发送正版验证回执, 回传信息ing...");
	}

}
