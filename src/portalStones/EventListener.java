package portalStones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import portalStones.data.PersistentData;
import portalStones.data.Portal;
import portalStones.data.Portalstone;
import portalStones.utilities.RNG;

public class EventListener implements Listener {
	private long lastSave = System.currentTimeMillis();

	private HashMap<UUID, Long> portalCooldowns = new HashMap<>();

	private HashMap<UUID, Long> activationCooldowns = new HashMap<>();

	private HashMap<Chunk, UUID> reservedChunks = new HashMap<>();

	public EventListener() {
		Main.plugin.getServer().getScheduler().scheduleSyncRepeatingTask((Plugin) Main.plugin, new Runnable() {
			public void run() {
				HashSet<UUID> coolDownRemovals = new HashSet<>(portalCooldowns.keySet());
				coolDownRemovals.forEach(x -> {
					if (!isOnPortalCooldown(x))
						portalCooldowns.remove(x);
				});
				coolDownRemovals = new HashSet<>(activationCooldowns.keySet());
				coolDownRemovals.forEach(x -> {
					if (!isOnActivationCooldown(x))
						activationCooldowns.remove(x);
				});
				Main.plugin.getLogger().log(Level.FINE,
						"Freed entities that are no longer on cooldown from their respective maps");
			}
		}, 6000L, 6000L);
	}

	@EventHandler
	public void onWorldSave(WorldSaveEvent event) {
		if (System.currentTimeMillis() - lastSave >= 30000L) {
			lastSave = System.currentTimeMillis();
			Main.plugin.saveData();
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getItemInHand().hasItemMeta() && event.getItemInHand().getItemMeta().getPersistentDataContainer()
				.has(PersistentData.portals, PersistentData.arrayListUUID))
			event.setCancelled(true);
	}

	@EventHandler
	public void onItemSpawn(ItemSpawnEvent event) throws Exception {
		if (event.getEntity().getItemStack().getItemMeta().getPersistentDataContainer().has(PersistentData.portals,
				PersistentData.arrayListUUID)) {
			event.getEntity().setGlowing(true);
			Portalstone portalStone = new Portalstone(event.getEntity());
			portalStone.updateLore();
			event.getEntity().getItemStack().setItemMeta(portalStone.getItemMeta());
		}
	}

	@EventHandler
	public void onEntityPickupItem(EntityPickupItemEvent event) throws Exception {
		if (event.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(PersistentData.portals,
				PersistentData.arrayListUUID)) {
			Portalstone portalStone = new Portalstone(event.getItem());
			portalStone.updateLore();
			event.getItem().getItemStack().setItemMeta(portalStone.getItemMeta());
		}
		putOnActivationCooldown(event.getItem().getUniqueId());
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) throws Exception {
		HashMap<Integer, ? extends ItemStack> items = event.getInventory().all(Material.REDSTONE);
		for (Integer i : items.keySet()) {
			if (items.get(i).getAmount() == 1 && items.get(i).getItemMeta().getPersistentDataContainer()
					.has(PersistentData.portals, PersistentData.arrayListUUID)) {
				Portalstone portalStone = new Portalstone(items.get(i));
				portalStone.updateLore();
				event.getInventory().setItem(i, portalStone);
			}
		}
		for (HumanEntity viewer : event.getViewers()) {
			items = viewer.getInventory().all(Material.REDSTONE);
			for (Integer i : items.keySet()) {
				if (items.get(i) != null && items.get(i).getAmount() == 1 && items.get(i).getItemMeta()
						.getPersistentDataContainer().has(PersistentData.portals, PersistentData.arrayListUUID)) {
					Portalstone portalStone = new Portalstone(items.get(i));
					portalStone.updateLore();
					viewer.getInventory().setItem(i, portalStone);
				}
			}
		}
	}

	@EventHandler
	public void onEntityPortalEnter(EntityPortalEnterEvent event) throws Exception {
		if (!event.getEntity().isValid() || isOnActivationCooldown(event.getEntity().getUniqueId())
				|| isOnPortalCooldown(event.getEntity().getUniqueId()) || event.getEntity().getPortalCooldown() > 240
				|| event.getLocation().getBlock().getType() != Material.NETHER_PORTAL)
			return;
		Portal portal = Portal.fromLocation(event.getLocation());
		Item item = null;
		Portalstone portalstone = null;
		Player player = null;
		if (event.getEntity() instanceof Item) {
			item = (Item) event.getEntity();
			if (item.getPickupDelay() <= 0)
				return;
			if (item.getItemStack().getType() == Material.REDSTONE) {
				if (item.getItemStack().getItemMeta().getPersistentDataContainer().has(PersistentData.portals,
						PersistentData.arrayListUUID))
					portalstone = new Portalstone(item);
			} else {
				item = null;
			}
		} else if (event.getEntity() instanceof Player) {
			player = (Player) event.getEntity();
		}
		if (portal == null) {
			if (portalstone == null && item != null && Main.plugin.configuration.fromRedstone) {
				createPortalStone(item);
				return;
			}
			if (portalstone != null) {
				putOnActivationCooldown(event.getEntity().getUniqueId());
				portalstone.bindPortal(new Portal(event.getLocation().getBlock(), item.getVelocity()));
				return;
			}
		} else {
			if (portalstone == null && item != null && Main.plugin.configuration.fromRedstone) {
				createPortalStone(item);
				return;
			}
			if (portal.getLinkedPortal() == null) {
				if (portalstone != null) {
					if (portalstone.getBoundPortals().contains(portal.getID())
							&& portalstone.getBoundPortals().size() > 1) {
						portalstone.fusePortals(portal);
						return;
					}
					bounce(event.getEntity(), event.getLocation());
					putOnActivationCooldown(event.getEntity().getUniqueId());
					return;
				}
				bounce(event.getEntity(), event.getLocation());
				putOnActivationCooldown(event.getEntity().getUniqueId());
				return;
			}
			if (player != null) {
				if (!portalCooldowns.containsKey(player.getUniqueId()) && player.getGameMode() == GameMode.CREATIVE
						&& player.getPortalCooldown() == 0) {
					portalCooldowns.put(player.getUniqueId(), Long.MIN_VALUE);
				} else if (!portalCooldowns.containsKey(player.getUniqueId()) && player.getPortalCooldown() == 0) {
					portalCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 4000L);
				} else if (portalCooldowns.containsKey(player.getUniqueId()) && player.getPortalCooldown() == 10) {
					safePortal(player, portal.getLinkedPortal().getLocation());
					if (portalCooldowns.containsKey(player.getUniqueId()))
						portalCooldowns.remove(player.getUniqueId());
				}
			} else if (portalstone != null) {
				if (portalstone.getBoundPortals().size() > 0) {
					portalstone.fusePortals(portal);
				} else {
					putOnActivationCooldown(event.getEntity().getUniqueId());
					safePortal(event.getEntity(), portal.getScaledLinkedPortalLocation(event.getLocation()));
					return;
				}
			} else {
				putOnActivationCooldown(event.getEntity().getUniqueId());
				if (event.getEntity() instanceof LivingEntity) {
					safePortal(event.getEntity(), portal.getLinkedPortal().getLocation());
				} else {
					safePortal(event.getEntity(), portal.getScaledLinkedPortalLocation(event.getLocation()));
				}
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			Block blockFrom = event.getFrom().getBlock();
			if (blockFrom.getType() != Material.NETHER_PORTAL
					&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.EAST))
							.getType() != Material.NETHER_PORTAL
					&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.WEST))
							.getType() != Material.NETHER_PORTAL
					&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.SOUTH))
							.getType() != Material.NETHER_PORTAL
					&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.NORTH))
							.getType() == Material.NETHER_PORTAL)
				;
			if (Portal.getId(blockFrom) != null)
				event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPortal(EntityPortalEvent event) {
		Block blockFrom = event.getFrom().getBlock();
		if (blockFrom.getType() != Material.NETHER_PORTAL
				&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.EAST))
						.getType() != Material.NETHER_PORTAL
				&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.WEST))
						.getType() != Material.NETHER_PORTAL
				&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.SOUTH))
						.getType() != Material.NETHER_PORTAL
				&& (blockFrom = event.getFrom().getBlock().getRelative(BlockFace.NORTH))
						.getType() == Material.NETHER_PORTAL)
			;
		if (Portal.getId(blockFrom) != null) {
			event.setCancelled(true);
		} else {
			Block blockTo = event.getFrom().getBlock();
			if (blockFrom.getType() != Material.NETHER_PORTAL
					&& (blockTo = event.getFrom().getBlock().getRelative(BlockFace.EAST))
							.getType() != Material.NETHER_PORTAL
					&& (blockTo = event.getFrom().getBlock().getRelative(BlockFace.WEST))
							.getType() != Material.NETHER_PORTAL
					&& (blockTo = event.getFrom().getBlock().getRelative(BlockFace.SOUTH))
							.getType() != Material.NETHER_PORTAL
					&& (blockTo = event.getFrom().getBlock().getRelative(BlockFace.NORTH))
							.getType() == Material.NETHER_PORTAL)
				;
			Portal portalTo = Portal.fromBlock(blockTo);
			if (portalTo != null) {
				if (blockFrom.getType() == Material.NETHER_PORTAL)
					blockFrom.breakNaturally();
				bounce(event.getEntity(), blockFrom.getLocation());
				event.setCancelled(true);
			}
			if (Main.plugin.configuration.fromRedstone && event.getEntity() instanceof Item) {
				Item item = (Item) event.getEntity();
				if (item.getItemStack().getType() == Material.REDSTONE) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Portal portal;
		Block block;
		switch (event.getBlock().getType()) {
		case NETHER_PORTAL:
			portal = Portal.fromBlock(event.getBlock());
			if (portal != null)
				portal.remove();
			return;
		case OBSIDIAN:
			block = null;
			portal = null;
			if ((block = event.getBlock().getRelative(BlockFace.EAST)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			if ((block = event.getBlock().getRelative(BlockFace.WEST)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			if ((block = event.getBlock().getRelative(BlockFace.SOUTH)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			if ((block = event.getBlock().getRelative(BlockFace.NORTH)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			if ((block = event.getBlock().getRelative(BlockFace.DOWN)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			if ((block = event.getBlock().getRelative(BlockFace.UP)).getType() == Material.NETHER_PORTAL
					&& (portal = Portal.fromBlock(block)) != null)
				portal.remove();
			return;
		default:
			break;
		}
	}

	public void onBlockExplode(BlockExplodeEvent event) {
		HashSet<UUID> portalsToRemove = new HashSet<UUID>();
		event.blockList().stream().filter(x -> (Portal.getId(x) != null)).forEach(x -> {
			portalsToRemove.add(Portal.getId(x));
		});
		portalsToRemove.forEach(x -> Portal.fromUUID(x).remove());
	}

	@EventHandler
	public void onEntityExplodeEvent(EntityExplodeEvent event) {
		HashSet<UUID> portalsToRemove = new HashSet<UUID>();
		event.blockList().stream().filter(x -> (Portal.getId(x) != null)).forEach(x -> {
			portalsToRemove.add(Portal.getId(x));
		});
		portalsToRemove.forEach(x -> Portal.fromUUID(x).remove());
	}

	@EventHandler
	public void onBlockDropItem(BlockDropItemEvent event) {
		if (Main.plugin.configuration.drops && event.getBlockState().getType() == Material.REDSTONE_ORE
				&& event.getItems().stream().anyMatch(x -> (x.getItemStack().getType() == Material.REDSTONE))) {
			int roll = RNG.Random(1, Main.plugin.configuration.dropChance);
			if (roll == Main.plugin.configuration.dropChance)
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), (ItemStack) new Portalstone());
		}
	}

	private void safePortal(final Entity entity, final Location location) {
		location.getChunk().addPluginChunkTicket((Plugin) Main.plugin);
		reservedChunks.put(location.getChunk(), entity.getUniqueId());
		Main.plugin.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) Main.plugin, new Runnable() {
			public void run() {
				putOnActivationCooldown(entity.getUniqueId());
				if (entity instanceof Player) {
					((Player) entity).playSound(location, Sound.BLOCK_PORTAL_TRAVEL, 0.2F, 1.0F);
				} else {
					entity.setPortalCooldown(300);
					entity.setVelocity(
							location.getDirection().multiply(entity.getVelocity().distance(new Vector(0, 0, 0))));
				}
				entity.getLocation().setDirection(location.getDirection());
				entity.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
				Main.plugin.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) Main.plugin, new Runnable() {
					public void run() {
						if (Main.plugin.eventListener.reservedChunks.get(location.getChunk())
								.equals(entity.getUniqueId()))
							location.getChunk().removePluginChunkTicket((Plugin) Main.plugin);
					}
				}, 40L);
			}
		}, 1L);
	}

	private void putOnActivationCooldown(UUID id) {
		activationCooldowns.put(id, System.currentTimeMillis() + 500L);
	}

	private boolean isOnActivationCooldown(UUID id) {
		if (!activationCooldowns.containsKey(id))
			return false;
		if (activationCooldowns.get(id) >= System.currentTimeMillis())
			return true;
		activationCooldowns.remove(id);
		return false;
	}

	private boolean isOnPortalCooldown(UUID id) {
		return portalCooldowns.containsKey(id) && portalCooldowns.get(id) >= System.currentTimeMillis();
	}

	private void bounce(Entity entity, Location fromLocation) {
		if (entity instanceof LivingEntity) {
			Vector v1 = fromLocation.getBlock().getLocation().toVector(),
					v2 = entity.getLocation().getBlock().getLocation().toVector();
			v1.setY(0);
			v2.setY(0);
			v1 = v2.subtract(v1);
			v2 = v1.normalize();
			try {
				entity.setVelocity(v2);
			} catch (Exception exception) {
			}
			entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation(), 11, 0.25D, 0.25D, 0.25D,
					new Particle.DustOptions(Color.FUCHSIA, 0.5F));
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5F, 1.0F);
		} else {
			putOnActivationCooldown(entity.getUniqueId());
			entity.setVelocity(entity.getVelocity().multiply(-1));
			entity.getWorld().spawnParticle(Particle.REDSTONE, entity.getLocation(), 11, 0.25D, 0.25D, 0.25D,
					new Particle.DustOptions(Color.FUCHSIA, 0.5F));
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5F, 1.0F);
		}
	}

	private void createPortalStone(Item item) {
		if (item.getItemStack().getType() == Material.REDSTONE && !item.getItemStack().getItemMeta()
				.getPersistentDataContainer().has(PersistentData.portals, PersistentData.arrayListUUID)) {
			item.setItemStack(new Portalstone(item.getItemStack().getAmount()));
			putOnActivationCooldown(item.getUniqueId());
			item.setVelocity(item.getVelocity().multiply(-1));
			item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 11, 0.25D, 0.25D, 0.25D,
					new Particle.DustOptions(Color.RED, 0.5F));
			item.getWorld().playSound(item.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1.0F);
			item.setGlowing(true);
		} else {
			throw new IllegalArgumentException(item + " is not pure " + Material.REDSTONE);
		}
	}
}
