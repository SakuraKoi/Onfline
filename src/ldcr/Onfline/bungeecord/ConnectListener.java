package ldcr.Onfline.bungeecord;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import ldcr.Onfline.bungeecord.task.CheckPremiumTask;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;

public class ConnectListener implements Listener {
	@EventHandler
	public void onPreLogin(final PreLoginEvent e) {
		if (!OnflineBungeecord.works) return;
		if (e.isCancelled()) return;
		if (OnflineBungeecord.getSession().isRequestingPremium(e.getConnection().getAddress().getAddress())) {
			OnflineBungeecord.getSession().startCheckPremium(e.getConnection().getAddress().getAddress());
			e.getConnection().setOnlineMode(true);
			OnflineBungeecord.log("&e开始验证连接 "+e.getConnection().getAddress().getAddress().getHostAddress()+" 的正版身份");
		}
	}
	@EventHandler
	public void onLogin(final LoginEvent e) {
		if (!OnflineBungeecord.works) return;
		if (e.getConnection().isOnlineMode()) {
			final InitialHandler initialHandler = (InitialHandler) e.getConnection();
			final String username = initialHandler.getLoginRequest().getData();
			OnflineBungeecord.getSession().updateUUID(initialHandler);
			try {
				final UUID offlineUUID = generateOfflineId(username);
				final Field idField = InitialHandler.class.getDeclaredField("uniqueId");
				idField.setAccessible(true);
				idField.set(e.getConnection(), offlineUUID);
			} catch (final Exception ex) {
				OnflineBungeecord.instance.getLogger().log(Level.SEVERE, "Failed to set offline uuid of " + username, ex);
			}
		}
	}
	private UUID generateOfflineId(final String playerName) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
	}
	@EventHandler
	public void afterLogin(final PostLoginEvent e) {
		if (!OnflineBungeecord.works) return;
		if (OnflineBungeecord.getSession().isCheckingPremium(e.getPlayer())) {
			new CheckPremiumTask(e);
		}
	}
	@EventHandler
	public void onQuit(final PlayerDisconnectEvent e) {
		if (!OnflineBungeecord.works) return;
		if (OnflineBungeecord.getSession().isUpdatingPremium(e.getPlayer())) {
			OnflineBungeecord.getSession().cancelUpdatePremium(e.getPlayer());
		}
	}

	@EventHandler
	public void onServerConnected(final ServerConnectedEvent event) {
		if (!OnflineBungeecord.works) return;
		final ProxiedPlayer player = event.getPlayer();
		if (OnflineBungeecord.getSession().isUpdatingPremium(player)) {
			BungeeChannelMessage.requestPremium(player, event.getServer(), OnflineBungeecord.getSession().getUpdatingPremiumState(player));
		}
	}

	@EventHandler
	public void onPluginMessage(final PluginMessageEvent pluginMessageEvent) {
		if (!OnflineBungeecord.works) return;
		final String channel = pluginMessageEvent.getTag();
		if (pluginMessageEvent.isCancelled() || !"OnflineBungeecord".equals(channel)) return;
		pluginMessageEvent.setCancelled(true);
		if (!(pluginMessageEvent.getSender() instanceof Server)) return;

		final byte[] data = Arrays.copyOf(pluginMessageEvent.getData(), pluginMessageEvent.getData().length);
		final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();

		ProxyServer.getInstance().getScheduler().runAsync(OnflineBungeecord.instance, new Runnable() {
			@Override
			public void run() {
				readMessage(forPlayer, data);
			}
		});
	}

	private void readMessage(final ProxiedPlayer forPlayer, final byte[] data) {
		final ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
		final String subChannel = dataInput.readUTF();
		if ("requestAuth".equals(subChannel)) {
			OnflineBungeecord.getSession().requestCheckPremium(forPlayer);
		} else if ("finishPremium".equals(subChannel)) {
			OnflineBungeecord.getSession().finishUpdatePremium(forPlayer);
		}
	}
}
