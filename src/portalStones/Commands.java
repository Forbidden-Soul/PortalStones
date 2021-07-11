package portalStones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import portalStones.utilities.RecipeCreator;

public class Commands implements CommandExecutor, TabCompleter {
	private List<String> completions;

	private List<String> commands = Arrays.asList(new String[] { "on", "off", "reload", "recipe" });

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch (args.length) {
		case 0:
			sender.sendMessage("PortalStones created by: ForbiddenSoul");
			return true;
		case 1:
			switch (args[0].toLowerCase()) {
			case "reload":
				if (Main.plugin.getCommand("portalstones reload").getPermission() == null
						|| sender.hasPermission(Main.plugin.getCommand("portalstones reload").getPermission())) {
					if (Main.plugin.on) {
						Main.plugin.onDisable();
						Main.plugin.onEnable();
					} else {
						Main.plugin.onEnable();
						Main.plugin.onDisable();
					}
					sender.sendMessage("PortalStones successfully reloaded");
				} else {
					sender.sendMessage(Main.plugin.getCommand("portalstones reload").getPermissionMessage());
				}
				return true;
			case "on":
				if (Main.plugin.getCommand("portalstones on").getPermission() == null
						|| sender.hasPermission(Main.plugin.getCommand("portalstones on").getPermission())) {
					if (!Main.plugin.on) {
						Main.plugin.onEnable();
						sender.sendMessage("PortalStones has been turned " + ChatColor.GREEN + "ON");
					} else {
						sender.sendMessage("PortalStones is already turned " + ChatColor.GREEN + "ON");
					}
				} else {
					sender.sendMessage(Main.plugin.getCommand("portalstones on").getPermissionMessage());
				}
				return true;
			case "off":
				if (Main.plugin.getCommand("portalstones off").getPermission() == null
						|| sender.hasPermission(Main.plugin.getCommand("portalstones off").getPermission())) {
					if (Main.plugin.on) {
						Main.plugin.onDisable();
						sender.sendMessage("PortalStones has been turned " + ChatColor.RED + "OFF");
					} else {
						sender.sendMessage("PortalStones is already turned " + ChatColor.RED + "OFF");
					}
				} else {
					sender.sendMessage(Main.plugin.getCommand("portalstones.off").getPermissionMessage());
				}
				return true;
			case "recipe":
				if(sender instanceof Player) {
					new RecipeCreator((Player)sender);
				}
				return true;
			}			
			return false;
		}
		return false;
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		completions = new ArrayList<>();
		switch (args.length) {
		case 1:
			StringUtil.copyPartialMatches(args[0], commands, completions);
			break;
		}
		completions = (List<String>) completions.stream()
				.filter(x -> sender.hasPermission(Main.plugin.getCommand("portalstones " + x).getPermission()))
				.collect(Collectors.toList());
		return completions;
	}
}
