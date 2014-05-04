package de.dustplanet.silkspawnersecoaddon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

// Metrics
import org.mcstats.Metrics;

//Vault
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;

/**
 * General stuff (config)
 * 
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersEcoAddon extends JavaPlugin {
    public FileConfiguration config;
    private File configFile;
    public Economy economy;
    public double defaultPrice = 10.5;
    public boolean chargeXP, confirmation;
    public ArrayList<UUID> pendingConfirmationList = new ArrayList<UUID>();


    public void onDisable() {
	getServer().getScheduler().cancelTasks(this);
	pendingConfirmationList.clear();
    }

    public void onEnable() {
	// Check for Vault
	Plugin vault = getServer().getPluginManager().getPlugin("Vault");
	if (vault != null && vault instanceof Vault) {
	    // If Vault is enabled, load the economy
	    getLogger().info("Loaded Vault successfully");
	    setupEconomy();
	} else {
	    // Else tell the admin about the missing of Vault
	    getLogger().severe("Vault was not found! XP charging is now ON...");
	}

	// Config
	configFile = new File(getDataFolder(), "config.yml");
	if (!configFile.exists()) {
	    if (configFile.getParentFile().mkdirs()) {
		copy(getResource("config.yml"), configFile);
	    } else {
		getLogger().severe("The config folder could NOT be created, make sure it's writable!");
		getLogger().severe("Disabling now!");
		setEnabled(false);
		return;
	    }
	}

	config = getConfig();
	loadConfig();

	// Now we need to check regardless of the setting in the conifg if we need to charge XP
	if (vault == null || !(vault instanceof Vault)) {
	    chargeXP = true;
	}

	// Listeners
	getServer().getPluginManager().registerEvents(new SilkSpawnersEcoAddonListener(this), this);

	// Metrics
	try {
	    Metrics metrics = new Metrics(this);
	    metrics.start();
	} catch (IOException e) {
	    getLogger().info("Couldn't start Metrics, please report this!");
	    e.printStackTrace();
	}

	// Task if needed
	if (confirmation) {
	    getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
		public void run() {
		    // Clear pending list
		    pendingConfirmationList.clear();
		}
	    }, getConfig().getInt("confirmation.delay") * 20L, getConfig().getInt("confirmation.delay") * 20L);
	}
    }

    private void loadConfig() {
	config.options().header("You can configure every entityID/name (without spaces) or a default!");
	config.addDefault("cantAfford", "&e[SilkSpawnersEco] &4Sorry, but you can't change the mob of this spawner, because you have not enough money!");
	config.addDefault("afford", "&e[SilkSpawnersEco] &2This action costs &e%money%");
	config.addDefault("sameMob", "&e[SilkSpawnersEco] &2This action was free, because it's the same mob!");
	config.addDefault("confirmationPending", "&e[SilkSpawnersEco] Remember that changing the spawner costs money, if you want to continue, do the action again!");
	config.addDefault("chargeSameMob", false);
	config.addDefault("chargeXP", false);
	config.addDefault("confirmation.enabled", false);
	config.addDefault("confirmation.delay", 30);
	config.addDefault("default", 10.5);
	config.addDefault("pig", 7.25);
	config.addDefault("cow", 0.00);
	config.options().copyDefaults(true);
	saveConfig();
	defaultPrice = config.getDouble("default");
	chargeXP = config.getBoolean("chargeXP");
	confirmation = config.getBoolean("confirmation.enabled");
    }

    // Initialized to work with Vault
    private boolean setupEconomy() {
	RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
	if (economyProvider != null) {
	    economy = economyProvider.getProvider();
	}
	return (economy != null);
    }

    // If no config is found, copy the default one(s)!
    private void copy(InputStream in, File file) {
	OutputStream out = null;
	try {
	    out = new FileOutputStream(file);
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
		out.write(buf, 0, len);
	    }
	} catch (IOException e) {
	    getLogger().warning("Failed to copy the default config! (I/O)");
	    e.printStackTrace();
	} finally {
	    try {
		if (out != null) {
		    out.close();
		}
	    } catch (IOException e) {
		getLogger().warning("Failed to close the streams! (I/O -> Output)");
		e.printStackTrace();
	    }
	    try {
		if (in != null) {
		    in.close();
		}
	    } catch (IOException e) {
		getLogger().warning("Failed to close the streams! (I/O -> Input)");
		e.printStackTrace();
	    }
	}
    }
}