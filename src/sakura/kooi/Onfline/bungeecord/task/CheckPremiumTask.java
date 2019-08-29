package sakura.kooi.Onfline.bungeecord.task;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import sakura.kooi.Onfline.bungeecord.OnflineBungeecord;

public class CheckPremiumTask implements Runnable {
	private final PostLoginEvent e;
	public CheckPremiumTask(final PostLoginEvent e) {
		this.e = e;
		ProxyServer.getInstance().getScheduler().runAsync(OnflineBungeecord.getInstance(), this);
	}

	@Override
	public void run() {
		if (e.getPlayer().getPendingConnection().isOnlineMode()) {
			OnflineBungeecord.getSessionManager().passCheckPremium(e.getPlayer());
		} else {
			OnflineBungeecord.getSessionManager().failedCheckPremium(e.getPlayer());
		}
	}

}
