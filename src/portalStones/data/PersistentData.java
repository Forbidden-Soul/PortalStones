package portalStones.data;

import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import portalStones.Main;

public class PersistentData {
  public static final NamespacedKey portals = new NamespacedKey((Plugin)Main.plugin, "persistentData.portals");  
	public static final PersistentDataType<byte[], ArrayList<UUID>> arrayListUUID = new ArrayListUUID();
}
