package com.danielgulic.havanaboats;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

public class BoatCommands implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("engine")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (player.isInsideVehicle() && (player.getVehicle() instanceof Boat)) {
					BoatController boatController = HavanaBoats.getBoatController();
					boolean engineToggle = boatController.toggleEngine(player.getVehicle().getUniqueId());
					sender.sendMessage(engineToggle ? ChatColor.GREEN + "Engine started" : ChatColor.RED + "Engine stopped");
				} else {
					sender.sendMessage(ChatColor.RED + "You must be in a boat to use this command!");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
			}
			return true;
		} else if (cmd.getName().equalsIgnoreCase("reloadhavanaboats")) {
			HavanaBoats.get().reloadConfig();
			sender.sendMessage(ChatColor.GREEN + "Reloaded Havana Boats configuration");
			return true;
		}

		return false;
	}
}
