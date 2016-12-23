package de.dustplanet.silkspawnersecoaddon.listeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;
import de.dustplanet.silkspawnersecoaddon.SilkSpawnersEcoAddon;
import de.dustplanet.util.SilkUtil;

/**
 * This is the listener of the custom event to charge the user.
 *
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersEcoSpawnerChangeListener implements Listener {
    /**
     * SilkSpawnersEcoAddon instance.
     */
    private SilkSpawnersEcoAddon plugin;
    /**
     * SilkUtil instance.
     */
    private SilkUtil su;

    /**
     * Creates a new SilkSpawnersEcoAddonListener.
     * SilkUtil is hooked with a static hook of SilkSpawners.
     *
     * @param instance of SilkSpawnersEcoAddon
     */
    public SilkSpawnersEcoSpawnerChangeListener(SilkSpawnersEcoAddon instance) {
        plugin = instance;
        su = SilkUtil.hookIntoSilkSpanwers();
    }

    /**
     * Called when a spawner is in state of changing.
     *
     * @param event custom SilkSpawnersSpawnerChangeEvent
     */
    @EventHandler
    public void onSpawnerChange(SilkSpawnersSpawnerChangeEvent event) {
        // Get information
        Player player = event.getPlayer();
        short entityID = event.getEntityID();
        // Don't charge the same mob more than 1 time
        short spawnerID = event.getOldEntityID();
        if (!plugin.getConfig().getBoolean("chargeSameMob") && entityID == spawnerID) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("sameMob")));
            return;
        }

        // Get name and replace occurring spaces
        String name = su.getCreatureName(entityID).toLowerCase().replace(" ", "");
        double price = plugin.getDefaultPrice();
        // Is a specific price listed, yes get it!
        if (plugin.getConfig().contains(name)) {
            price = plugin.getConfig().getDouble(name);
        } else if (plugin.getConfig().contains(Short.toString(entityID))) {
            // Maybe only the ID is delivered?
            price = plugin.getConfig().getDouble(Short.toString(entityID));
        }

        // If price is 0 or player has free perm, stop here!
        if (price <= 0 || player.hasPermission("silkspawners.free")) {
            return;
        }
        // Charge for the amount (multiply)
        if (plugin.getConfig().getBoolean("chargeMultipleAmounts", false)) {
            price *= event.getAmount();
        }

        // Hook into the pending confirmation list
        if (plugin.isConfirmation()) {
            UUID playerName = player.getUniqueId();
            // Notify the player and cancel event
            if (!plugin.getPendingConfirmationList().contains(playerName)) {
                plugin.getPendingConfirmationList().add(playerName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("confirmationPending")).replace("%money%", Double.toString(price)));
                event.setCancelled(true);
                return;
            }
            // Now remove the player and continue normal procedure
            plugin.getPendingConfirmationList().remove(playerName);
        }

        // If he has the money, charge it
        if (plugin.isChargeXP()) {
            int totalXP = player.getTotalExperience();
            if (totalXP >= price) {
                totalXP -= price;
                player.setTotalExperience(0);
                player.setLevel(0);
                player.giveExp(totalXP);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afford")).replace("%money%", Double.toString(price)));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cantAfford")));
                event.setCancelled(true);
            }
        } else {
            if (plugin.getEcon().has(player, price)) {
                plugin.getEcon().withdrawPlayer(player, price);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("afford")).replace("%money%", Double.toString(price)));
            } else {
                // Else notify and cancel
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cantAfford")));
                event.setCancelled(true);
            }
        }
    }
}