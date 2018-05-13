package ldcr.Onfline.bungeecord;

import java.util.UUID;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;

public class LoginSource {
	private String player;
	private boolean premium;
	private UUID uuid;
	private PendingConnection connection;
	public LoginSource(final PendingConnection connection) {
		this.connection = connection;
		premium = connection.isOnlineMode();
		uuid = connection.getUniqueId();
		player = connection.getName().toLowerCase();
	}
	public LoginSource(final ProxiedPlayer player) {
		connection = player.getPendingConnection();
		premium = connection.isOnlineMode();
		uuid = connection.getUniqueId();
		this.player = player.getName();
	}
	public LoginSource(final InitialHandler connection) {
		this.connection = connection;
		premium = connection.isOnlineMode();
		uuid = connection.getUniqueId();
		player = connection.getLoginRequest().getData().toLowerCase();
	}
	public LoginSource(final String player, final boolean premium, final String uuid) {
		this.player = player.toLowerCase();
		this.premium = premium;
		this.uuid = UUID.fromString(uuid);
		connection = null;
	}

	public PendingConnection getConnection() {
		return connection;
	}

	public boolean isPermium() {
		return premium;
	}

	public void setPermium(final boolean permium) {
		premium = permium;
	}

	public String getPlayer() {
		return player;
	}

	public void setPlayer(final String player) {
		this.player = player.toLowerCase();
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(final UUID uuid) {
		this.uuid = uuid;
	}
	public void setConnection(final PendingConnection connection) {
		this.connection = connection;
	}
}
