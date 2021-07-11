package portalStones.utilities;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.meta.ItemMeta;

import portalStones.Main;
import portalStones.data.Portalstone;

public class RecipeCreator implements Listener {
    private Player player;
    private ItemStack[] _playerInventory;
    private boolean closed = false, playerInventory = false, validRecipe = false;    
    private int page = 0;

    public InventoryView inventoryView;
    
    private static ItemStack itemStack;
    private static HashMap<Character, ItemStack> recipeIngredients = new HashMap<Character, ItemStack>();
    private static ItemMeta itemMeta;
    private static Material[] materials = Arrays.asList(Material.values()).stream().filter(m -> {
        return (m.isItem() && m != null && m != Material.AIR);
    }).toArray(Material[]::new);
    private static ItemStack[][] itemStacks = new ItemStack[(materials.length % 27) == 0 ? materials.length / 27
            : (materials.length / 27) + 1][27];
    static {
        for (int i = 0, j = 0; i < itemStacks.length; i++) {
            for (j = 0; j < 27; j++) {
                if (i * 27 + j < materials.length)
                    itemStacks[i][j] = new ItemStack(materials[(i * 27) + j]);
                else
                    break;
            }
        }
    }

    public RecipeCreator(Player player) {
        super();
        this.player = player;
        _playerInventory = this.player.getInventory().getContents();
        Bukkit.getServer().getPluginManager().registerEvents(this, Main.plugin);        
        inventoryView = this.player.openWorkbench(null, true);
        goToPage(0);
    }

    @Override
    protected void finalize() throws Throwable {
        if (!closed) {     
            player.getInventory().setContents(_playerInventory);
            player.setItemOnCursor(null);
            HandlerList.unregisterAll(this);
            inventoryView.close();
            player.closeInventory();
            player.getInventory().setContents(_playerInventory);
            player.setItemOnCursor(null);
            inventoryView.getInventory(0).clear();
            closed = true;
        }        
        super.finalize();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView() == inventoryView) {
            if (e.getAction() == InventoryAction.CLONE_STACK || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.NOTHING || e.getRawSlot() == 0 || e.getClick() == ClickType.DOUBLE_CLICK) {
                e.setResult(Result.DENY);
                return;
            }
            switch (e.getSlotType()) {
                case ARMOR:
                    break;
                case CONTAINER:
                    e.setResult(Result.DENY);
                    inventoryView.setCursor(e.getCurrentItem());
                    return;
                case CRAFTING:
                    if (e.getCurrentItem() != null && e.getCurrentItem().equals(e.getCursor())) {
                        e.setResult(Result.DENY);
                    }
                    return;
                case FUEL:
                    break;
                case OUTSIDE:
                    inventoryView.setCursor(null);
                    e.setResult(Result.DENY);
                    return;
                case QUICKBAR:
                    switch (e.getRawSlot()) {
                        case 37:
                            if (page > 0) {
                                goToPage(page - 1);
                            } else {
                                goToPage(itemStacks.length - 1);
                            }
                            break;
                        case 40:
                            if (playerInventory) {
                                playerInventory = false;                                
                                goToPage(page);                              
                            }
                            break;
                        case 41:
                            if (validRecipe) {
                                saveRecipe();
                                inventoryView.close();
                                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2, 2);
                            }
                            break;
                        case 42:
                            if (!playerInventory) {
                                playerInventory = true;
                                goToPlayerInventory();
                            }
                            break;
                        case 45:
                            if (page < itemStacks.length - 1) {
                                goToPage(page + 1);
                            } else {
                                goToPage(0);
                            }
                            break;
                        default:
                            break;
                    }
                    e.setResult(Result.DENY);
                    return;
                case RESULT:
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView() == inventoryView) {
            inventoryView.getTopInventory().clear();
            inventoryView.getBottomInventory().clear();
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable(){
                @Override
                public void run() {
                    player.getInventory().setContents(_playerInventory);
                }
            });             
            inventoryView.setCursor(null);
            HandlerList.unregisterAll(this);
            closed = true;
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        if (e.getView() == inventoryView) {
            if (!e.getInventory().isEmpty()) {
                if (e.getRecipe() != null) {
                    validRecipe = e.getRecipe().getResult() == null;        
                } else {
                    validRecipe = true;
                }            
            }            
            else {
                validRecipe = false;
            }      
            if (validRecipe) {
                e.getInventory().setItem(0, Main.plugin.recipe.getResult());
            }
            Bukkit.getServer().getScheduler().runTask(Main.plugin, new Runnable(){
                @Override
                public void run() {
                    setQuickBar();
                }
            }); 
        }
    }
    private void goToPage(int i) {
        this.page = i;
        // 37 41 45
        itemStack = new ItemStack(Material.ARROW);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("← " + (i == 0 ? itemStacks.length : i) + "\\" + itemStacks.length);
        itemStack.setItemMeta(itemMeta);
        inventoryView.setItem(37, itemStack);
        itemMeta.setDisplayName((i == itemStacks.length - 1 ? 1 : i + 2) + "\\" + itemStacks.length + " →");
        itemStack.setItemMeta(itemMeta);
        inventoryView.setItem(45, itemStack);        
        for (int j = 0; j < 27; j++) {
            inventoryView.setItem(j + 10, itemStacks[i][j]);
        }
        setQuickBar();
    }
    private void goToPlayerInventory() {        
        for (int j = 0; j < 27; j++) {
            inventoryView.setItem(j + 10, _playerInventory[j + 9]);
        }
        inventoryView.setItem(37, null);
        inventoryView.setItem(45, null);
        setQuickBar();
    }
    private void setQuickBar() {
        //✔ ❌ ← → ↓ ↑
        if (validRecipe) {
            itemStack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            itemMeta = itemStack.getItemMeta();       
            itemMeta.setDisplayName(ChatColor.GREEN + "✔");                        
        } else {
            itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            itemMeta = itemStack.getItemMeta();       
            itemMeta.setDisplayName(ChatColor.RED + "❌");   
        }
        itemStack.setItemMeta(itemMeta);
        inventoryView.setItem(41, itemStack);
        if(playerInventory) {
            itemStack = new ItemStack(Material.HOPPER);
            itemMeta = itemStack.getItemMeta();       
            itemMeta.setDisplayName("↑");
            itemStack.setItemMeta(itemMeta);
            inventoryView.setItem(40, itemStack);
            inventoryView.setItem(42, null);
        } else {
            itemStack = new ItemStack(Material.HOPPER);
            itemMeta = itemStack.getItemMeta();       
            itemMeta.setDisplayName("↓");
            itemStack.setItemMeta(itemMeta);
            inventoryView.setItem(40, null);
            inventoryView.setItem(42, itemStack);
        }
        inventoryView.setItem(38, null);
        inventoryView.setItem(39, null);
        inventoryView.setItem(43, null);
        inventoryView.setItem(44, null);
    }
    private void saveRecipe() {
        recipeIngredients.clear();
        char c = 'A', cc = 0;
        char shape[][] = new char[3][3];        
        boolean similar = false;
        for(int i = 1; i < 10; i++) {
            similar = false;
            cc = 0;
            for(char is : recipeIngredients.keySet()) {
                if(recipeIngredients.get(is).isSimilar(inventoryView.getItem(i))) {
                    similar = true;
                    cc = is;
                }
            }
            if(inventoryView.getItem(i) != null && inventoryView.getItem(i).getType() != Material.AIR) {
                if(!similar) {
                    recipeIngredients.put(c, inventoryView.getItem(i));
                    shape[i < 4 ? 0 : i < 7 ? 1 : 2][(i - 1) % 3] = c;
                    c++;
                }
                else {
                    shape[i < 4 ? 0 : i < 7 ? 1 : 2][(i - 1) % 3] = cc;
                }              
            } else  {
                shape[i < 4 ? 0 : i < 7 ? 1 : 2][(i - 1) % 3] = ' ';
            }
        }
        int minX = 2, maxX = 0, minY = 2, maxY = 0;
        for (int i = 0, j; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                if (shape[i][j] != ' ') {
                    if (maxX < j) {
                        maxX = j;
                    }
                    if (maxY < i) {
                        maxY = i;
                    }
                    if (minX > j) {
                        minX = j;
                    }
                    if (minY > i) {
                        minY = i;
                    }
                }
            }
        }
        char[][] finalShape = new char[maxY - minY + 1][maxX - minX + 1];
        for (int y = minY, x; y < maxY + 1; y++) {
            for (x = minX; x < maxX + 1; x++) {
                finalShape[y - minY][x - minX] = shape[y][x];
            }
        }            
        String shapeString[] = new String[finalShape.length];
        for (int i = 0; i < finalShape.length; i++) {
            shapeString[i] = new String(finalShape[i]);
        }
        Main.plugin.recipe = new ShapedRecipe(Main.plugin.recipeKey, new Portalstone(1));
        Main.plugin.recipe.shape(shapeString);
        for (Character ch : recipeIngredients.keySet()) {
            Main.plugin.recipe.setIngredient(ch, new ExactChoice(recipeIngredients.get(ch)));
        }
        Bukkit.getServer().removeRecipe(Main.plugin.recipeKey);
        if(Main.plugin.configuration.craftable) {
            Bukkit.getServer().addRecipe(Main.plugin.recipe);
        }
    }
}
