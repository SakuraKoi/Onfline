package ldcr.Onfline.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class BungeeChannelMessage {
	public static void requestPremium(final ProxiedPlayer player, final Server server, final boolean isPremium) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("requestPremium");
		output.writeUTF(player.getName());
		output.writeBoolean(isPremium);
		server.sendData("OnflineBungeecord", output.toByteArray());
	}
	public static void requestUnPremium(final String player) {
		final ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("requestUnPremium");
		output.writeUTF(player);
		output.writeLong(System.currentTimeMillis());
		final byte[] data = output.toByteArray();
		for (final ServerInfo server : ProxyServer.getInstance().getServers().values()) {
			server.sendData("OnflineBungeecord", data);
		}
	}

}
