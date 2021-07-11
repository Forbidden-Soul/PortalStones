package portalStones.utilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import com.google.gson.Gson;

import portalStones.Main;
import portalStones.data.Portal;
import portalStones.data.previous.Data;

public class PreviousDataConverter {
	public static void convert() throws Exception {
		Data previousData = new Data();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(Main.plugin.getDataFile()));
			previousData = new Gson().fromJson(bufferedReader, Data.class);
			int i = 0;
			for (Integer portalWay : previousData.serializedPortalWays.keySet()) {
				for (i = 0; i < previousData.serializedPortalWays.get(portalWay).size(); i++) {
					Portal portal1;
					Location location = new Location(
							Main.plugin.getServer()
									.getWorld(previousData.serializedPortalWays.get(portalWay).get(i).worldName),
							previousData.serializedPortalWays.get(portalWay).get(i).x,
							previousData.serializedPortalWays.get(portalWay).get(i).y,
							previousData.serializedPortalWays.get(portalWay).get(i).z,
							previousData.serializedPortalWays.get(portalWay).get(i).yaw, 0.0F);
					location = findAndLightPortal(location);
					if (Portal.fromLocation(location) == null) {
						portal1 = new Portal(location);
					} else {
						portal1 = Portal.fromLocation(location);
					}
					if (i < previousData.serializedPortalWays.get(portalWay).size() - 2) {
						Portal portal2;
						location = new Location(
								Main.plugin.getServer().getWorld(
										previousData.serializedPortalWays.get(portalWay).get(i + 1).worldName),
								previousData.serializedPortalWays.get(portalWay).get(i + 1).x,
								previousData.serializedPortalWays.get(portalWay).get(i + 1).y,
								previousData.serializedPortalWays.get(portalWay).get(i + 1).z,
								previousData.serializedPortalWays.get(portalWay).get(i + 1).yaw, 0.0F);
						location = findAndLightPortal(location);
						if (Portal.fromLocation(location) == null) {
							portal2 = new Portal(location);
						} else {
							portal2 = Portal.fromLocation(location);
						}
						portal1.setLinkedPortal(portal2);
					} else {
						Portal portal2;
						location = new Location(
								Main.plugin.getServer()
										.getWorld(previousData.serializedPortalWays.get(portalWay).get(0).worldName),
								previousData.serializedPortalWays.get(portalWay).get(0).x,
								previousData.serializedPortalWays.get(portalWay).get(0).y,
								previousData.serializedPortalWays.get(portalWay).get(0).z,
								previousData.serializedPortalWays.get(portalWay).get(0).yaw, 0.0F);
						location = findAndLightPortal(location);
						if (Portal.fromLocation(location) == null) {
							portal2 = new Portal(location);
						} else {
							portal2 = Portal.fromLocation(location);
						}
						portal1.setLinkedPortal(portal2);
					}
					if (i > 0) {
						location = new Location(
								Main.plugin.getServer().getWorld(
										previousData.serializedPortalWays.get(portalWay).get(i - 1).worldName),
								previousData.serializedPortalWays.get(portalWay).get(i - 1).x,
								previousData.serializedPortalWays.get(portalWay).get(i - 1).y,
								previousData.serializedPortalWays.get(portalWay).get(i - 1).z,
								previousData.serializedPortalWays.get(portalWay).get(i - 1).yaw, 0.0F);
						location = findAndLightPortal(location);
						portal1.fromPortals.add(Portal.fromLocation(location).getID());
					} else {
						Portal portal;
						location = new Location(
								Main.plugin.getServer()
										.getWorld(previousData.serializedPortalWays.get(portalWay).get(
												previousData.serializedPortalWays.get(portalWay).size() - 1).worldName),
								previousData.serializedPortalWays.get(portalWay)
										.get(previousData.serializedPortalWays.get(portalWay).size() - 1).x,
								previousData.serializedPortalWays.get(portalWay)
										.get(previousData.serializedPortalWays.get(portalWay).size() - 1).y,
								previousData.serializedPortalWays.get(portalWay)
										.get(previousData.serializedPortalWays.get(portalWay).size() - 1).z,
								previousData.serializedPortalWays.get(portalWay)
										.get(previousData.serializedPortalWays.get(portalWay).size() - 1).yaw,
								0.0F);
						location = findAndLightPortal(location);
						if (Portal.fromLocation(location) == null) {
							portal = new Portal(location);
						} else {
							portal = Portal.fromLocation(location);
						}
						portal1.fromPortals.add(portal.getID());
					}
				}
			}
		} catch (FileNotFoundException fileNotFoundException) {
		}
	}

	private static Location findAndLightPortal(Location location) {
		if (location.getBlock().getType() == Material.NETHER_PORTAL)
			return location;
		Location newLocation = location;
		int count = 0;
		while (newLocation.getBlock().getType() != Material.NETHER_PORTAL && count <= 100) {
			if (newLocation.getBlock().getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) {
				newLocation = newLocation.getBlock().getRelative(BlockFace.DOWN).getLocation();
			} else {
				newLocation.getBlock().setType(Material.FIRE);
				return newLocation;
			}
			count++;
		}
		return location;
	}
}
