package portalStones;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import portalStones.data.Configuration;
import portalStones.data.Data;
import portalStones.data.Portal;
import portalStones.data.Portalstone;
import portalStones.utilities.PreviousDataConverter;
import portalStones.utilities.Update;

public class Main extends JavaPlugin {
	public static Main plugin = null;	
	public Configuration configuration = null;
	public boolean on = false;
	public NamespacedKey recipeKey = null;
	public ShapedRecipe recipe = null;

	EventListener eventListener = null;
	Commands commands = null;

	private YamlConfiguration config = null;
	private File configFile = null;
	private File dataFile = null;
	private File backupFile = null;	

	public void onEnable() {
		plugin = this;
		plugin.on = true;
		plugin.loadConfig();
		if (plugin.configuration.update) {
			plugin.getLogger().log(Level.INFO, "Auto updates enabled. Checking for updates.");
			Update.checkForUpdates();
		} else {
			plugin.getLogger().log(Level.INFO, "Auto updates disabled. Not checking for updates.");
		}
		plugin.eventListener = new EventListener();
		plugin.getServer().getPluginManager().registerEvents(plugin.eventListener, (Plugin) plugin);
		plugin.dataFile = new File(getDataFolder(), "data");
		plugin.backupFile = new File(getDataFolder(), "backup");
		plugin.recipeKey = new NamespacedKey((Plugin) plugin, "Portalstone");
		plugin.recipe = new ShapedRecipe(plugin.recipeKey, new Portalstone(1));		
		plugin.recipe.shape("A");
		plugin.recipe.setIngredient('A', new ExactChoice(new ItemStack(Material.REDSTONE)));
		if (plugin.configuration.craftable) {									
			plugin.getServer().addRecipe(plugin.recipe);
		}
		plugin.commands = new Commands();
		plugin.getCommand("portalstones").setExecutor(plugin.commands);
		plugin.loadData();
	}

	public void onDisable() {
		if (!plugin.on)
			return;
		plugin.on = false;
		plugin.saveData();
		HandlerList.unregisterAll(plugin.eventListener);
		plugin.getServer().getScheduler().cancelTasks(plugin);
		plugin.eventListener = null;
		plugin.configFile = null;
		plugin.dataFile = null;
		plugin.dataFile = null;
		plugin.config = null;
		plugin.commands = null;
		plugin.getServer().removeRecipe(plugin.recipeKey);
	}

	void loadConfig() {
		plugin.configFile = new File(getDataFolder(), "config.yml");
		plugin.config = new YamlConfiguration();
		plugin.config.set("english | deutsche | francais", "english");
		plugin.config.set("Drops from Redstone Ore", true);
		plugin.config.set("Drop chance 1 in every x blocks", 64);
		plugin.config.set("Craftable", false);
		plugin.config.set("Portals create Portalstone from Redstone", false);
		plugin.config.set("Auto updates enabled", false);
		if (!plugin.getDataFolder().exists())
			plugin.getDataFolder().mkdirs();
		if (!plugin.configFile.exists()) {
			try {
				plugin.configFile.createNewFile();
				plugin.config.save(plugin.configFile);
			} catch (IOException iOException) {
			}
		} else {
			try {
				plugin.config.load(plugin.configFile);
			} catch (FileNotFoundException fileNotFoundException) {

			} catch (IOException iOException) {

			} catch (InvalidConfigurationException invalidConfigurationException) {
			}
		}
		if (plugin.config.contains("Craftable (recipe is one redstone)")) {
			plugin.config.set("Craftable", plugin.config.getBoolean("Craftable (recipe is one redstone)"));
			plugin.config.set("Craftable (recipe is one redstone)", null);
			try {
				plugin.config.save(plugin.configFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		plugin.configuration = new Configuration(				
				plugin.config.getBoolean("Craftable", false),
				plugin.config.getBoolean("Drops from Redstone Ore", true),
				plugin.config.getBoolean("Portals create Portalstone from Redstone", false),
				plugin.config.getInt("Drop chance 1 in every x blocks", 64),
				plugin.config.getString("english | deutsche | francais"),
				plugin.config.getBoolean("Auto updates enabled")
			);
	}

	void backUpData() {
		try {
			plugin.backupFile.createNewFile();
			Files.copy(plugin.dataFile.toPath(), plugin.backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to craeate a back up file:\n", e);
		}
	}

	void saveData() {
		backUpData();
		try {
			plugin.dataFile.createNewFile();
			Data data = new Data();
			data.portalBlockMap = new HashMap<>(Portal.portalBlockMap);
			data.portalMap = new HashMap<>(Portal.portalMap);
			data.blockMap = new HashMap<>(Portal.blockMap);
			data.recipeShape = recipe.getShape();
			data.recipeIngredientMap = recipe.getIngredientMap();
			BukkitObjectOutputStream out = new BukkitObjectOutputStream(
					new GZIPOutputStream(new ObjectOutputStream(new FileOutputStream(plugin.dataFile))));
			out.writeObject(data);
			out.close();
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Save failed:\n", e);
		}
	}

	void loadData() {
		if (!plugin.dataFile.exists())
			try {
				plugin.dataFile.createNewFile();
				plugin.saveData();
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "Load failed:\n", e);
			}
		try {
			BukkitObjectInputStream in = new BukkitObjectInputStream(
					new GZIPInputStream(new ObjectInputStream(new FileInputStream(plugin.dataFile))));
			Data data = (Data) in.readObject();
			in.close();
			Portal.portalBlockMap = new HashMap<>(data.portalBlockMap);
			Portal.portalMap = new HashMap<>(data.portalMap);
			Portal.blockMap = new HashMap<>(data.blockMap);
			if(data.recipeShape != null) {
				recipe = new ShapedRecipe(recipeKey, new Portalstone(1));
				recipe.shape(data.recipeShape);
				for(char c : data.recipeIngredientMap.keySet()) {
					recipe.setIngredient(c, new ExactChoice(data.recipeIngredientMap.get(c)));
				}
				if(configuration.craftable) {
					plugin.getServer().removeRecipe(plugin.recipeKey);
					plugin.getServer().addRecipe(plugin.recipe);
				}
			}
		} catch (FileNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "Load failed:\n", e);
		} catch (IOException e) {
			plugin.getLogger().log(Level.WARNING, "Data apears to be from a previous verison.");
			plugin.getLogger().log(Level.WARNING, "Attempting to convert to the current version:");
			try {
				try {
					File file = new File(getDataFolder(), "previousVersion");
					file.createNewFile();
					Files.copy(plugin.dataFile.toPath(), file.toPath(),
							new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
				} catch (IOException e1) {
					plugin.getLogger().log(Level.SEVERE, "Failed to craeate a back up file:\n", e1);
				}
				PreviousDataConverter.convert();
				plugin.getLogger().log(Level.WARNING, "Data successfully converted");
				saveData();
			} catch (Exception e1) {
				plugin.getLogger().log(Level.SEVERE, "DATA CONVERSION FAILED!");
				plugin.getLogger().log(Level.SEVERE,
						"Blank data is being used instead. You can go back to your old data by renamaing ther \"previousVersion\" file to \"data\" while PortalStones is turned off, or disabled.");
				plugin.getLogger().log(Level.SEVERE,
						"If this problem persists you may need to restart ther server, or start over new by deleting the data file.");
			}
		} catch (ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "Load failed:\n", e);
		}
	}

	public File getDataFile() {
		return plugin.dataFile;
	}
}
