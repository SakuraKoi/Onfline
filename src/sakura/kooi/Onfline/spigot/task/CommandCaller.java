package sakura.kooi.Onfline.spigot.task;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandCaller implements Runnable {
	private final String player;
	private final List<String> commands;
	public CommandCaller(final String player, final List<String> commands) {
		this.player = player;
		this.commands = commands;
	}

	@Override
	public void run() {
		final CommandSender console = Bukkit.getConsoleSender();
		for (final String command : commands) {
			Bukkit.dispatchCommand(console, command.replace("%player%", player));
		}
	}

}
