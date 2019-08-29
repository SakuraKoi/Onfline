package sakura.kooi.Onfline.spigot.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import sakura.kooi.Onfline.spigot.OnflineSpigot;
import sakura.kooi.Utils.Bukkit.TitleUtils;

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
			TitleUtils.sendTitle(player, OnflineSpigot.getInstance().getMessagePremiumTitle(), "", 10, 100, 10);
			player.sendMessage(OnflineSpigot.getInstance().getMessagePremiumMessage());
			Bukkit.getScheduler().runTask(OnflineSpigot.getInstance(), new CommandCaller(player.getName(), OnflineSpigot.getInstance().getCommandsPermium()));
		} else {
			TitleUtils.sendTitle(player, OnflineSpigot.getInstance().getMessageCrackedTitle(), "", 10, 100, 10);
			player.sendMessage(OnflineSpigot.getInstance().getMessageCrackedMessage());
		}
		OnflineSpigot.getInstance().getMessageChannel().replyUpdated(player);
		OnflineSpigot.log("&a已向玩家 "+player.getName()+" 发送正版验证回执, 回传信息ing...");
	}

}
