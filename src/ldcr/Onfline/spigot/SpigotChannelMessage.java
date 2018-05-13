package ldcr.Onfline.spigot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import ldcr.Onfline.spigot.task.ChannelReader;

public class SpigotChannelMessage implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(final String channel, final Player p, final byte[] message) {
		if (!"OnflineBungeecord".equals(channel)) return;
		Bukkit.getScheduler().runTaskAsynchronously(OnflineSpigot.instance, new ChannelReader(message));
	}

	public void replyUpdated(final Player player) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("finishPremium");
		player.sendPluginMessage(OnflineSpigot.instance, "OnflineBungeecord", output.toByteArray());
	}

	public void requestAuth(final Player player) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("requestAuth");
		player.sendPluginMessage(OnflineSpigot.instance, "OnflineBungeecord", output.toByteArray());
	}
}
