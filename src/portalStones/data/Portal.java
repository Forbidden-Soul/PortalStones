package portalStones.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class Portal implements Serializable {
	private static final transient long serialVersionUID = -2277699096575125629L;

	private final UUID id;

	private final Location location;

	private UUID toPortal;

	public final HashSet<UUID> fromPortals;

	private final double minXZ;

	private final double maxXZ;

	private final double minY;

	private final double maxY;

	private final boolean eastWest;

	public static transient HashMap<UUID, Portal> portalMap = new HashMap<>();

	public static transient HashMap<Location, UUID> portalBlockMap = new HashMap<>();

	public static transient HashMap<UUID, HashSet<Location>> blockMap = new HashMap<>();

	public Portal(Location location) throws Exception {
		this(location.getBlock(), location.getDirection());
	}

	public Portal(Block block, Vector direction) throws Exception {
		boolean eastWest;
		toPortal = null;
		if (block.getType() != Material.NETHER_PORTAL)
			throw new Exception("Portals cannt be created from " + block.getType() + ". Use " + Material.NETHER_PORTAL
					+ " instead.");
		if (fromBlock(block) != null)
			throw new Exception("This block already belongs to a portal.\nPortal was not created.");
		id = UUID.randomUUID();
		portalMap.put(id, this);
		fromPortals = new HashSet<>();
		if (block.getBlockData().getAsString().equals("minecraft:nether_portal[axis=x]")) {
			eastWest = true;
		} else if (block.getBlockData().getAsString().equals("minecraft:nether_portal[axis=z]")) {
			eastWest = false;
		} else {
			throw new Exception("Nether Portal Data Incompatable (We are still on 1.16.3)");
		}
		this.eastWest = eastWest;
		blockMap.put(id, GetConnectedBlocks(block, new HashSet<>(), eastWest));
		double x = 0.0D, z = 0.0D;
		int minXZ = eastWest ? block.getX() : block.getZ(), maxXZ = eastWest ? block.getX() : block.getZ(),
				minY = block.getY(), maxY = block.getY();
		for (Location location : blockMap.get(id)) {
			portalBlockMap.put(location, id);
			if (location.getBlockY() < minY)
				minY = location.getBlockY();
			if (location.getBlockY() > maxY)
				maxY = location.getBlockY();
			if (eastWest) {
				if (location.getBlockX() < minXZ)
					minXZ = location.getBlockX();
				if (location.getBlockX() > maxXZ)
					maxXZ = location.getBlockX();
			} else {
				if (location.getBlockZ() < minXZ)
					minXZ = location.getBlockZ();
				if (location.getBlockZ() > maxXZ)
					maxXZ = location.getBlockZ();
			}
			x += location.getX();
			z += location.getZ();
		}
		this.minXZ = minXZ;
		this.maxXZ = maxXZ;
		this.minY = minY;
		this.maxY = maxY;
		direction = direction.setY(0);
		// When we lost source and had to decompile this 6.28... % 6.28 appeared, I
		// forget what the original calculation involved, I hope this is still correct.
		float yaw = (float) Math
				.toDegrees((Math.atan2(-direction.getX(), direction.getZ()) + 6.283185307179586D) % 6.283185307179586D);
		if (yaw >= 0.0F && yaw <= 45.0F) {
			yaw = 0.0F;
		} else if (yaw > 45.0F && yaw <= 135.0F) {
			yaw = 90.0F;
		} else if (yaw > 135.0F && yaw <= 225.0F) {
			yaw = 180.0F;
		} else if (yaw > 225.0F && yaw <= 315.0F) {
			yaw = 270.0F;
		} else if (yaw > 315.0F && yaw <= 360.0F) {
			yaw = 360.0F;
		}
		yaw -= 180.0F;
		location = new Location(block.getWorld(), x / (blockMap.get(id)).size() + 0.5D, minY,
				z / (blockMap.get(id)).size() + 0.5D, yaw, 0.0F);
	}

	public static HashSet<Location> GetConnectedBlocks(Block block, HashSet<Location> blockLocations,
			boolean eastWest) {
		if (!blockLocations.contains(block.getLocation()) && block.getType() == Material.NETHER_PORTAL) {
			blockLocations.add(block.getLocation());
			blockLocations = GetConnectedBlocks(block.getRelative(BlockFace.DOWN), blockLocations, eastWest);
			blockLocations = GetConnectedBlocks(block.getRelative(BlockFace.UP), blockLocations, eastWest);
			blockLocations = GetConnectedBlocks(block.getRelative(eastWest ? BlockFace.EAST : BlockFace.SOUTH),
					blockLocations, eastWest);
			blockLocations = GetConnectedBlocks(block.getRelative(eastWest ? BlockFace.WEST : BlockFace.NORTH),
					blockLocations, eastWest);
		}
		return blockLocations;
	}

	public static Location getPortalLocation(Block block) throws Exception {
		boolean eastWest;
		double x = 0.0D, y = Double.MAX_VALUE, z = 0.0D;
		if (block.getBlockData().getAsString().equals("minecraft:nether_portal[axis=x]")) {
			eastWest = true;
		} else if (block.getBlockData().getAsString().equals("minecraft:nether_portal[axis=z]")) {
			eastWest = false;
		} else {
			throw new Exception("Nether Portal Data Incompatable (We are still on 1.15.2)");
		}
		int size = 0;
		for (Location location : GetConnectedBlocks(block, new HashSet<>(), eastWest)) {
			if (location.getY() < y)
				y = (int) location.getY();
			x += location.getX();
			z += location.getZ();
			size++;
		}
		return new Location(block.getWorld(), x / size + 0.5D, y, z / size + 0.5D, block.getLocation().getYaw(),
				block.getLocation().getPitch());
	}

	public Portal getLinkedPortal() {
		return portalMap.get(toPortal);
	}

	public void setLinkedPortal(Portal portal) {
		toPortal = portal.getID();
	}

	public void setLinkedPortal(UUID id) {
		toPortal = id;
	}

	public Location getLocation() {
		return location;
	}

	public UUID getID() {
		return id;
	}

	public boolean isPortalBlock(Block block) {
		if (portalBlockMap.containsKey(block.getLocation()) && portalBlockMap.get(block.getLocation()).equals(id))
			return true;
		return false;
	}

	public static UUID getId(Block block) {
		return getId(block.getLocation());
	}

	public static UUID getId(Location location) {
		if (portalBlockMap.containsKey(location))
			return portalBlockMap.get(location);
		return null;
	}

	public static Portal fromBlock(Block block) {
		if (portalBlockMap.containsKey(block.getLocation()))
			return portalMap.get(portalBlockMap.get(block.getLocation()));
		return null;
	}

	public static Portal fromLocation(Location location) {
		if (portalBlockMap.containsKey(location.getBlock().getLocation()))
			return portalMap.get(portalBlockMap.get(location.getBlock().getLocation()));
		return null;
	}

	public static Portal fromUUID(UUID id) {
		if (!portalMap.containsKey(id))
			return null;
		return portalMap.get(id);
	}

	public void remove() {
		portalMap.remove(id);
		for (Location location : blockMap.get(id))
			portalBlockMap.remove(location);
		blockMap.remove(id);
		if (toPortal != null && portalMap.keySet().contains(toPortal)
				&& portalMap.get(toPortal).fromPortals.contains(id))
			portalMap.get(toPortal).fromPortals.remove(id);
		for (UUID fromId : fromPortals) {
			if (fromId != null && toPortal != null && !fromId.equals(toPortal) && portalMap.containsKey(toPortal)
					&& portalMap.containsKey(fromId)) {
				portalMap.get(fromId).toPortal = toPortal;
				portalMap.get(toPortal).fromPortals.add(fromId);
				continue;
			}
			if (fromId != null && toPortal != null && portalMap.containsKey(toPortal) && portalMap.containsKey(fromId)
					&& portalMap.get(fromId).fromPortals.size() > 0) {
				boolean substituted = false;
				for (UUID substituteId : portalMap.get(fromId).fromPortals) {
					if (fromId != null && substituteId != null && !substituteId.equals(fromId)
							&& !substituteId.equals(id)) {
						portalMap.get(fromId).toPortal = substituteId;
						portalMap.get(substituteId).fromPortals.add(fromId);
						if (portalMap.get(substituteId).toPortal.equals(id))
							portalMap.get(substituteId).toPortal = fromId;
						substituted = true;
						break;
					}
				}
				if (!substituted)
					for (UUID substituteId : fromPortals) {
						if (fromId != null && substituteId != null && !substituteId.equals(fromId)
								&& !substituteId.equals(id)) {
							portalMap.get(fromId).toPortal = substituteId;
							portalMap.get(substituteId).fromPortals.add(fromId);
							if (portalMap.get(substituteId).toPortal.equals(id))
								portalMap.get(substituteId).toPortal = fromId;
							substituted = true;
							break;
						}
					}
				if (!substituted) {
					blockMap.get(fromId).iterator().next().getBlock().breakNaturally();
					portalMap.get(fromId).remove();
				}
				continue;
			}
			if (fromId != null && blockMap.containsKey(fromId)) {
				blockMap.get(fromId).iterator().next().getBlock().breakNaturally();
				portalMap.get(fromId).remove();
			}
		}
	}

	public Location getScaledLinkedPortalLocation(Location location) {
		double yPercent = (location.getY() - minY) / (maxY - minY);
		double xzPercent = eastWest ? ((location.getX() - minXZ) / (maxXZ - minXZ))
				: ((location.getZ() - minXZ) / (maxXZ - minXZ));
		Portal to = getLinkedPortal();
		return to.eastWest ? new Location(getLinkedPortal().getLocation().getWorld(),
				(to.maxXZ - to.minXZ) * xzPercent + to.minXZ + 0.5D, (to.maxY - to.minY) * yPercent + to.minY + 0.5D,
				to.location.getZ(), to.location.getYaw(), to.location.getPitch())
				: new Location(location.getWorld(), to.location.getX(), (to.maxY - to.minY) * yPercent + to.minY + 0.5D,
						(to.maxXZ - to.minXZ) * xzPercent + to.minXZ + 0.5D, to.location.getYaw(),
						to.location.getPitch());
	}
}
