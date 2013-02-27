package de.dustplanet.silkspawnersecoaddon;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;
import de.dustplanet.util.SilkUtil;

public class SilkSpawnersEcoAddonListener implements Listener {
	private SilkSpawnersEcoAddon plugin;
	private SilkUtil su;
	
	/**
	 * This is the listener of the custom event
	 * @author xGhOsTkiLLeRx
	 */

	public SilkSpawnersEcoAddonListener(SilkSpawnersEcoAddon instance) {
		plugin = instance;
		su = SilkUtil.hookIntoSilkSpanwers();
	}

	@EventHandler
	public void onSpawnerChange(SilkSpawnersSpawnerChangeEvent event) {
		// Get information
		Player player = event.getPlayer();
		short entityID = event.getEntityID();
		// Get name and replace occuring spaces
		String name = su.getCreatureName(entityID).toLowerCase().replaceAll(" ", "");
		double price = plugin.defaultPrice;
		// Is a specific price listed, yes get it!
		if (plugin.config.contains(name)) {
			price = plugin.getConfig().getDouble(name);
		}
		// Maybe only the ID is delivered?
		else if (plugin.config.contains(Short.toString(entityID))) {
			price = plugin.config.getDouble(Short.toString(entityID));
		}
		// If price is 0 or player has free perm, stop here!
		if (price <= 0 || player.hasPermission("silkspawners.free")) return;
		// If he has the money, charge it
		if (plugin.economy.has(player.getName(), price)) {
			plugin.economy.withdrawPlayer(player.getName(), price);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getString("afford")).replace("%money%", Double.toString(price)));
		}
		// Else notify and cancel
		else {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getString("cantAfford")));
			event.setCancelled(true);
		}
	}
}