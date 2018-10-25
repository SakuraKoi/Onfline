package ldcr.Onfline.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class ChannelMessager {
	private ChannelMessager() {}
	public static void requestPremium(final ProxiedPlayer player, final Server server, final boolean isPremium) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("requestPremium");
		output.writeUTF(player.getName());
		output.writeBoolean(isPremium);
		server.sendData("OnflineBungeecord", output.toByteArray());
	}
	public static void requestUnPremium(final Server server, final String player) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("requestUnPremium");
		output.writeUTF(player);
		final byte[] data = output.toByteArray();
		server.sendData("OnflineBungeecord", data);
	}

}
