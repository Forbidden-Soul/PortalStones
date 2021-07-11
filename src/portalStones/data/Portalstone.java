package portalStones.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import portalStones.Main;

public class Portalstone extends ItemStack implements Serializable {
	private static final long serialVersionUID = 6311434699891048867L;

	private Item item = null;

	public Portalstone(int amount) {
		super(Material.REDSTONE);
		ItemMeta itemMeta = getItemMeta();
		itemMeta.getPersistentDataContainer().set(PersistentData.portals, PersistentData.arrayListUUID,
				new ArrayList<UUID>());
		setItemMeta(itemMeta);
		setAmount(amount);
		updateLore(true);
	}

	public Portalstone() {
		this(1);
	}

	public Portalstone(Item item) throws Exception {
		super(Material.REDSTONE, item.getItemStack().getAmount());
		if (item.getItemStack().getItemMeta() == null || !item.getItemStack().getItemMeta().getPersistentDataContainer()
				.has(PersistentData.portals, PersistentData.arrayListUUID))
			throw new Exception("You can only use the ItemStack constructor for converting pre-existing Portalstones.");
		setItemMeta(item.getItemStack().getItemMeta());
		this.item = item;
	}

	public Portalstone(ItemStack itemStack) throws Exception {
		super(Material.REDSTONE, itemStack.getAmount());
		if (itemStack.getItemMeta() == null || !itemStack.getItemMeta().getPersistentDataContainer()
				.has(PersistentData.portals, PersistentData.arrayListUUID))
			throw new Exception("You can only use the ItemStack constructor for converting pre-existing Portalstones.");
		setItemMeta(itemStack.getItemMeta());
	}

	public void bindPortal(Portal portal) {
		ItemMeta itemMeta = getItemMeta();
		ArrayList<UUID> ids = itemMeta.getPersistentDataContainer().get(PersistentData.portals,
				PersistentData.arrayListUUID);
		ids.add(portal.getID());
		itemMeta.getPersistentDataContainer().set(PersistentData.portals, PersistentData.arrayListUUID, ids);
		setItemMeta(itemMeta);
		updateLore(true);
		item.getItemStack().setItemMeta(getItemMeta());
		item.setVelocity(item.getVelocity().multiply(-1));
		item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 11, 0.25D, 0.25D, 0.25D,
				new Particle.DustOptions(Color.RED, 0.5F));
		item.getWorld().playSound(item.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1.0F);
	}

	public void fusePortals(Portal finalPortal) {
		fusePortals(finalPortal.getID());
	}

	public void fusePortals(UUID finalPortal) {
		UUID[] ids = getItemMeta().getPersistentDataContainer()
				.get(PersistentData.portals, PersistentData.arrayListUUID).stream()
				.filter(x -> (Portal.fromUUID(x) != null)).toArray(length -> new UUID[length]);
		for (int i = 0; i < ids.length; i++) {
			if (i < ids.length - 1)
				Portal.fromUUID(ids[i]).setLinkedPortal(ids[i + 1]);
			if (i > 0)
				Portal.fromUUID(ids[i]).fromPortals.add(ids[i - 1]);
		}
		if (finalPortal.equals(ids[ids.length - 1])) {
			Portal.fromUUID(ids[ids.length - 1]).setLinkedPortal(ids[0]);
			Portal.fromUUID(ids[0]).fromPortals.add(ids[ids.length - 1]);
		} else {
			Portal.fromUUID(ids[ids.length - 1]).setLinkedPortal(finalPortal);
			Portal.fromUUID(finalPortal).fromPortals.add(ids[ids.length - 1]);
		}
		item.getWorld().spawnParticle(Particle.REDSTONE, item.getLocation(), 44, 1.0D, 1.0D, 1.0D,
				new Particle.DustOptions(Color.RED, 1.0F));
		item.getWorld().playSound(item.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 1.0F);
		item.remove();
	}

	public void updateLore() {
		updateLore(false);
	}

	public void updateLore(boolean forceUpdate) {
		ItemMeta itemMeta = getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		ArrayList<UUID> ids = itemMeta.getPersistentDataContainer().get(PersistentData.portals,
				PersistentData.arrayListUUID);
		boolean changeLore = forceUpdate;
		UUID[] invalidIds = ids.stream().filter(x -> Portal.fromUUID(x) == null).toArray(x -> new UUID[x]);
		for (UUID id : invalidIds) {
			ids.remove(id);
			changeLore = true;
		}
		switch (Main.plugin.configuration.locale) {
		case English:
			if (itemMeta.getLocalizedName() != "Portalstone")
				changeLore = true;
			break;
		case French:
			if (itemMeta.getLocalizedName() != "Pierreportail")
				changeLore = true;
			break;
		case German:
			if (itemMeta.getLocalizedName() != "Portalstein")
				changeLore = true;
			break;
		}
		if (!changeLore)
			return;
		if (ids.size() > 0) {
			switch (Main.plugin.configuration.locale) {
			case English:
				itemMeta.setLocalizedName("Portalstone");
				itemMeta.setDisplayName(ChatColor.RESET + "Portalstone");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "A bound Portalstone.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Bound Portals: " + ids.size());
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Throw this into an unbound portal");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "to continue binding portals to");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "this Portalstone.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Throwing this into any previously");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "bound portal will fuse the portals");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "together in a portal path way.");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Any entity may traverse the portal ways.");
				break;
			case French:
				itemMeta.setLocalizedName("Pierreportail");
				itemMeta.setDisplayName(ChatColor.RESET + "Pierreportail");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Portails lié" + ids.size());
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Lance ceci dans un portail non lié");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "pour continuer a lier autre portails");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "à cette Pierreportail.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Quand ceci est lancer dans n'importe");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "quel portail précédemment lié, les");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "portails seront fusionnés ensemble");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "formant un chemin portail.");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Toute entités peut traverser les");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "chemins portail.");
				break;
			case German:
				itemMeta.setLocalizedName("Portalstein");
				itemMeta.setDisplayName(ChatColor.RESET + "Portalstein");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Gebundende Portale: " + ids.size());
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Werfe ihn in ein ungebundenes Portal, um");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "weitere Portale an diesen Portalstein zu");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "binden.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Ein Werfen in ein vorher gebundenes Portal,");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "wird alle bisher gebundenen Portale in");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "einer Portalverknvereinen.");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Alle Wesen ldiese Portalwege");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "durchqueren.");
				break;
			}
		} else {
			switch (Main.plugin.configuration.locale) {
			case English:
				itemMeta.setLocalizedName("Portalstone");
				itemMeta.setDisplayName(ChatColor.RESET + "Portalstone");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "An unbound Portalstone.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Throw this into a nether portal to");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "bind that portal to the Portalstone.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "The Portalstone can then be used to");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "create portal path ways between portals.");
				break;
			case French:
				itemMeta.setLocalizedName("Pierreportail");
				itemMeta.setDisplayName(ChatColor.RESET + "Pierreportail");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Une Pierreportail non lié");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Lance ceci dans un portail non lié");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "pour lier le portail avec la Pierreportail.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "La Pierreportail peut ensuite être  utilisée pour");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "créer des chemins de portail entre les portails.");
				break;
			case German:
				itemMeta.setLocalizedName("Portalstein");
				itemMeta.setDisplayName(ChatColor.RESET + "Portalstein");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Ein ungebundener Portalstein.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Werfe ihn in ein Netherportal, um das");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Portal an den Portalstein zu binden.");
				lore.add(" ");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Der Portalstein kann benutzt werden, um");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "eine Verknzwischen verschiedenen");
				lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "Portalen zu erstellen.");
				break;
			}
		}
		itemMeta.getPersistentDataContainer().set(PersistentData.portals, PersistentData.arrayListUUID, ids);
		itemMeta.setLore(lore);
		setItemMeta(itemMeta);
	}

	public ArrayList<UUID> getBoundPortals() {
		return (ArrayList<UUID>) getItemMeta().getPersistentDataContainer().get(PersistentData.portals,
				PersistentData.arrayListUUID);
	}
}
