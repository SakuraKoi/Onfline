package ldcr.Onfline.bungeecord.task;

import ldcr.Onfline.bungeecord.OnflineBungeecord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;

public class CheckPremiumTask implements Runnable {
	private final PostLoginEvent e;
	public CheckPremiumTask(final PostLoginEvent e) {
		this.e = e;
		ProxyServer.getInstance().getScheduler().runAsync(OnflineBungeecord.instance, this);
	}

	@Override
	public void run() {
		if (e.getPlayer().getPendingConnection().isOnlineMode()) {
			OnflineBungeecord.getSession().passCheckPremium(e.getPlayer());
		} else {
			OnflineBungeecord.getSession().failedCheckPremium(e.getPlayer());
		}
	}

}
