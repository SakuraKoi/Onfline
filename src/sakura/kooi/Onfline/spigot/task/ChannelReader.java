package sakura.kooi.Onfline.spigot.task;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import sakura.kooi.Onfline.spigot.OnflineSpigot;

public class ChannelReader implements Runnable {
	private final byte[] message;
	public ChannelReader(final byte[] message) {
		this.message = message;
	}
	@Override
	public void run() {
		final ByteArrayDataInput in = ByteStreams.newDataInput(message);
		final String subchannel = in.readUTF();
		if ("requestPremium".equals(subchannel)) {
			final String player = in.readUTF();
			final boolean isPremium = in.readBoolean();
			@SuppressWarnings("deprecation")
			final OfflinePlayer offp = Bukkit.getOfflinePlayer(player);
			if (offp==null) return;
			if (!offp.isOnline()) return;
			final Player p = offp.getPlayer();
			Bukkit.getScheduler().runTaskLater(OnflineSpigot.getInstance(), new PremiumUpdater(p,isPremium), 40);
		} else if ("requestUnPremium".equals(subchannel)) {
			final String player = in.readUTF();
			Bukkit.getScheduler().runTask(OnflineSpigot.getInstance(), new CommandCaller(player, OnflineSpigot.getInstance().getCommandsUnPremium()));
		}
	}
}