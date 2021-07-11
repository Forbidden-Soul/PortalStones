package portalStones.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import portalStones.Main;

public class Data implements Serializable {
	private static final transient long serialVersionUID = 3546269791275403782L;
	public static final String version = Main.plugin.getDescription().getVersion();
	public String gameVersion = Main.plugin.getServer().getVersion();
	public HashMap<UUID, Portal> portalMap = new HashMap<>();
	public HashMap<Location, UUID> portalBlockMap = new HashMap<>();
	public HashMap<UUID, HashSet<Location>> blockMap = new HashMap<>();
	public String[] recipeShape;
	public Map<Character, ItemStack> recipeIngredientMap;
}
