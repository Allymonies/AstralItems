package io.astralforge.astralitems;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.StonecutterInventory;

import io.astralforge.astralitems.block.AbstractAstralBlockSpec;

public class AstralItemListener implements Listener {

    private final AstralItems plugin;

    static final List<Material> PROHIBITED_INTERACTIONS = Arrays.asList(Material.COMPOSTER, Material.FARMLAND, Material.JUKEBOX, Material.LECTERN, Material.LODESTONE, Material.CAULDRON, Material.TNT, Material.GRINDSTONE, Material.FLOWER_POT);
    static final List<InventoryType> PROHIBITED_INSERTIONS = Arrays.asList(InventoryType.STONECUTTER, InventoryType.BREWING, InventoryType.SMOKER, InventoryType.GRINDSTONE, InventoryType.LECTERN, InventoryType.BEACON, InventoryType.BLAST_FURNACE, InventoryType.CARTOGRAPHY, InventoryType.LOOM, InventoryType.FURNACE);

    boolean isVanillaCraftable(ItemStack item) {
        AstralItemSpec astralItem = plugin.getAstralItem(item);
        if (astralItem == null) return true;
        return astralItem.isVanillaCraftable();
    }

    boolean isPlacable(ItemStack item) {
        AstralItemSpec astralItem = plugin.getAstralItem(item);
        if (astralItem == null) return true;

        AbstractAstralBlockSpec astralBlock = plugin.getAstralBlock(item);
        return astralBlock != null;
    }

    public AstralItemListener(AstralItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onCraft(CraftItemEvent event) {
        CraftingInventory craftingTable = event.getInventory();
        for (ItemStack item : craftingTable.getMatrix()) {
            if (!isVanillaCraftable(item)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory craftingTable = event.getInventory();
        for (ItemStack item : craftingTable.getMatrix()) {
            if (!isVanillaCraftable(item)) {
                craftingTable.setResult(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onPlace(BlockPlaceEvent event) {
        if (!isPlacable(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isVanillaCraftable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        /*
        Bukkit.getLogger().info("Information for " + event.getItem().toString());
        Bukkit.getLogger().info(event.getAction().toString());
        Bukkit.getLogger().info(event.isBlockInHand() ? "Block in hand" : "Block not in hand");
        Bukkit.getLogger().info(event.hasBlock() ? "Has Block" : "Does not have Block");
        Bukkit.getLogger().info(event.hasItem() ? "Has Item" : "Does not have Item");
        Bukkit.getLogger().info(event.useItemInHand().toString());
        Bukkit.getLogger().info(event.useInteractedBlock().toString());
        */
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !isVanillaCraftable(event.getItem())) {
            if (block != null && PROHIBITED_INTERACTIONS.contains(block.getType())) {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
            }

            if (event.isBlockInHand() && !isPlacable(event.getItem())) {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
            }

        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onDispense(BlockDispenseEvent event) {
        if (event.getVelocity().length()==0 && !isVanillaCraftable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!isVanillaCraftable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onEnchant(EnchantItemEvent event) {
        if (!isVanillaCraftable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        if (inventory.getItem(1) == null) {
            AstralItemSpec astralItem = plugin.getAstralItem(inventory.getItem(0));
            ItemStack result = event.getResult();
            if (astralItem != null && !astralItem.isRenamable()) {
                event.setResult(null);
            } else if (astralItem != null && result != null) {
                ItemStack newResult = astralItem.setLore(result, astralItem.getLore(result));
                plugin.getLogger().info("Setting resolt with lore");
                event.setResult(newResult);
            }
        } else if (!isVanillaCraftable(inventory.getItem(0)) || !isVanillaCraftable(inventory.getItem(1))) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClick() == ClickType.NUMBER_KEY && PROHIBITED_INSERTIONS.contains(event.getInventory().getType())) {
            if (!isVanillaCraftable(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
            }
        }
        if (event.getClick().isShiftClick()) {
            if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
                if (!isVanillaCraftable(event.getCurrentItem()) && PROHIBITED_INSERTIONS.contains(event.getWhoClicked().getOpenInventory().getType())) {
                    event.setCancelled(true);
                }
            }
        }
        if (event.getClickedInventory() != null && PROHIBITED_INSERTIONS.contains(event.getClickedInventory().getType())) {
            if (!isVanillaCraftable(event.getCursor())) {
                event.setCancelled(true);
            }
        }
        if (event.getClickedInventory() instanceof AnvilInventory) {
            AnvilInventory anvilInventory = (AnvilInventory) event.getClickedInventory();
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                if (anvilInventory.getItem(1) == null) {
                    AstralItemSpec astralItem = plugin.getAstralItem(anvilInventory.getItem(0));

                    ItemStack result = event.getCurrentItem();
                    if (astralItem != null && !astralItem.isRenamable()) {
                        event.setCancelled(true);
                    } else if (astralItem != null && result != null) {
                        ItemStack newResult = astralItem.setLore(result, astralItem.getLore(result));
                        event.setCurrentItem(newResult);
                    }
                } else if (!isVanillaCraftable(anvilInventory.getItem(0)) || !isVanillaCraftable(anvilInventory.getItem(1))) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getClickedInventory() instanceof SmithingInventory) {
            SmithingInventory smithingInventory = (SmithingInventory) event.getClickedInventory();
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                if (!isVanillaCraftable(smithingInventory.getItem(0)) || !isVanillaCraftable(smithingInventory.getItem(1))) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getClickedInventory() instanceof StonecutterInventory) {
            StonecutterInventory stonecutterInventory = (StonecutterInventory) event.getClickedInventory();
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                if (!isVanillaCraftable(stonecutterInventory.getItem(0))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isVanillaCraftable(event.getOldCursor())) {
            if (PROHIBITED_INSERTIONS.contains(event.getInventory().getType())) {
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!isVanillaCraftable(event.getItem()) && PROHIBITED_INSERTIONS.contains(event.getDestination().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onPrepareSmith(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        if (!isVanillaCraftable(inventory.getItem(0)) || !isVanillaCraftable(inventory.getItem(1))) {
            event.setResult(null);
            //inventory.setItem(3, null);
        }
    }

}
