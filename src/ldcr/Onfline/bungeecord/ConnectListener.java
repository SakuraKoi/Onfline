package ldcr.Onfline.bungeecord;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

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
		if (!OnflineBungeecord.isOnflineWorking()) return;
		if (e.isCancelled()) return;
		if (OnflineBungeecord.getSessionManager().isRequestingPremium(e.getConnection().getAddress().getAddress())) {
			OnflineBungeecord.getSessionManager().startCheckPremium(e.getConnection().getAddress().getAddress());
			e.getConnection().setOnlineMode(true);
		}
	}
	@EventHandler
	public void onLogin(final LoginEvent e) {
		if (!OnflineBungeecord.isOnflineWorking()) return;
		if (e.getConnection().isOnlineMode()) {
			final InitialHandler initialHandler = (InitialHandler) e.getConnection();
			final String player = initialHandler.getLoginRequest().getData();
			OnflineBungeecord.getSessionManager().updateUUID(initialHandler);
			try {
				final UUID offlineUUID = generateOfflineId(player);
				final Field idField = InitialHandler.class.getDeclaredField("uniqueId");
				idField.setAccessible(true);
				idField.set(e.getConnection(), offlineUUID);
			} catch (final Exception ex) {
				ex.printStackTrace();
				OnflineBungeecord.log("&c更新正版玩家 "+player+" 的UUID失败");
			}
		}
	}
	private UUID generateOfflineId(final String playerName) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
	}
	@EventHandler
	public void afterLogin(final PostLoginEvent e) {
		if (!OnflineBungeecord.isOnflineWorking()) return;
		if (OnflineBungeecord.getSessionManager().isCheckingPremium(e.getPlayer())) {
			new CheckPremiumTask(e);
		}
	}
	@EventHandler
	public void onQuit(final PlayerDisconnectEvent e) {
		if (!OnflineBungeecord.isOnflineWorking()) return;
		if (OnflineBungeecord.getSessionManager().isUpdatingPremium(e.getPlayer())) {
			OnflineBungeecord.getSessionManager().cancelUpdatePremium(e.getPlayer());
		}
	}

	@EventHandler
	public void onServerConnected(final ServerConnectedEvent event) {
		if (!OnflineBungeecord.isOnflineWorking()) return;
		final ProxiedPlayer player = event.getPlayer();
		if (OnflineBungeecord.getSessionManager().isUpdatingPremium(player)) {
			ChannelMessager.requestPremium(player, event.getServer(), OnflineBungeecord.getSessionManager().getUpdatingPremiumState(player));
		}
	}

	@EventHandler
	public void onPluginMessage(final PluginMessageEvent pluginMessageEvent) {
		if (!OnflineBungeecord.isOnflineWorking()) return;
		final String channel = pluginMessageEvent.getTag();
		if (pluginMessageEvent.isCancelled() || !"OnflineBungeecord".equals(channel)) return;
		pluginMessageEvent.setCancelled(true);
		if (!(pluginMessageEvent.getSender() instanceof Server)) return;
		
		final byte[] data = Arrays.copyOf(pluginMessageEvent.getData(), pluginMessageEvent.getData().length);
	
		final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();
		ProxyServer.getInstance().getScheduler().runAsync(OnflineBungeecord.getInstance(), () -> readMessage(forPlayer, data));
	}

	private void readMessage(final ProxiedPlayer forPlayer, final byte[] data) {
		final ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
		final String subChannel = dataInput.readUTF();
		if ("requestAuth".equals(subChannel)) {
			OnflineBungeecord.getSessionManager().requestCheckPremium(forPlayer);
		} else if ("finishPremium".equals(subChannel)) {
			OnflineBungeecord.getSessionManager().finishUpdatePremium(forPlayer);
		}
	}
}
