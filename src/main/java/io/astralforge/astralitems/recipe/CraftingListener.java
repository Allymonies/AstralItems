package io.astralforge.astralitems.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import io.astralforge.astralitems.AstralItemSpec;
import io.astralforge.astralitems.AstralItems;
import io.astralforge.astralitems.recipe.AstralRecipeEvaluator.Strategy;
import lombok.AllArgsConstructor;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class CraftingListener implements Listener {

    private AstralItems plugin;
    private final Integer cacheDuration = 60 * 20;
    private final HashMap<Location, ItemStack> unsmeltableCache = new HashMap<>();

    public CraftingListener(AstralItems plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                unsmeltableCache.clear();
            }
        }.runTaskTimer(plugin, cacheDuration, cacheDuration);
    }

    boolean isVanillaCraftable(ItemStack item) {
        return AstralItemSpec.isVanillaCraftable(plugin, item);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        boolean vanillaCraftable = true;

        // Check if we have any Astral items in crafting
        for (ItemStack itemStack : event.getInventory().getMatrix()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                if (!isVanillaCraftable(itemStack)) {
                    vanillaCraftable = false;
                    break;
                }
            }
        }

        if (plugin.getRecipeEvaluator().isRegistered(event.getRecipe())) {
            // We know this recipe!
            applyAstralRecipes(event.getInventory(), vanillaCraftable);
        } else {
            if (event.getRecipe() != null && !isVanillaCraftable(event.getRecipe().getResult())) {
                // Result is astral, so we should check it
                applyAstralRecipes(event.getInventory(), vanillaCraftable);
            } else if (vanillaCraftable == false) {
                // We have an Astral item in the crafting matrix, but minecraft was unable to determine our recipe
                // Generally this happens when there are recipe conflicts with the same base items
                applyAstralRecipes(event.getInventory(), vanillaCraftable);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStartSmelting(FurnaceStartSmeltEvent event) {
        boolean vanillaCraftable = isVanillaCraftable(event.getSource());
        Furnace furnace = (Furnace) event.getBlock().getState();
        ItemStack result = applyAstralRecipes(furnace.getInventory(), vanillaCraftable);

        if (result == null) {
            // No recipe was found, we need to try and prevent the smelting
            event.setTotalCookTime(Short.MAX_VALUE);
        }

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        Furnace furnace = (Furnace) event.getBlock().getState();
        FurnaceInventory inventory = furnace.getInventory();
        ItemStack smeltingItem = furnace.getInventory().getSmelting();
        if (smeltingItem != null) {
            ItemStack cachedUnsmeltable = unsmeltableCache.get(event.getBlock().getLocation());
            if (cachedUnsmeltable != null && cachedUnsmeltable.equals(smeltingItem)) {
                // We have already determined this item is unsmeltable
                event.setCancelled(true);
                return;
            }
        }
        boolean vanillaCraftable = isVanillaCraftable(inventory.getSmelting());
        ItemStack result = applyAstralRecipes(furnace.getInventory(), vanillaCraftable);

        if (result == null) {
            // No recipe was found, we need to try and prevent the furnace burn
            if (smeltingItem != null) {
                // Remember that this item is unsmeltable
                unsmeltableCache.put(event.getBlock().getLocation(), smeltingItem);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockCook(BlockCookEvent event) {
        if (event.getBlock().getState() instanceof Furnace) {
            Furnace furnace = (Furnace) event.getBlock().getState();
            FurnaceInventory inventory = furnace.getInventory();
            boolean vanillaCraftable = isVanillaCraftable(inventory.getSmelting());
            ItemStack result = applyAstralRecipes(furnace.getInventory(), vanillaCraftable);
            if (result != null) {
                event.setResult(result);
            } else {
                event.setCancelled(true);
            }
        } else {
            plugin.getLogger().warning("Skipping block cook event for non-furnace " + event.getBlock().getType());
        }
    }

    private void applyAstralRecipes(CraftingInventory inventory, boolean canFallback) {
        AstralRecipeEvaluator evaluator = plugin.getRecipeEvaluator();

        // First determine the strategies we can use
        List<Strategy<? extends Recipe>> strategies;
        switch (inventory.getType()) {
            case WORKBENCH:
                strategies = Lists.newArrayList(
                    new AstralShapedRecipeStrategy(3, 3),
                    new AstralShapelessRecipeStrategy()
                );
                break;

            case CRAFTING:
                strategies = Lists.newArrayList(
                    new AstralShapedRecipeStrategy(2, 2),
                    new AstralShapelessRecipeStrategy()
                );
                break;
            default:
                plugin.getLogger().warning("Skipping unsupported inventory type for crafting: " + inventory.getType());
                return;
        }

        // Now try to match against the recipes
        Optional<Recipe> recipe = evaluator.matchRecipe(inventory.getMatrix(), strategies);

        if (recipe.isPresent()) {
            inventory.setResult(recipe.get().getResult().clone());
        } else {
            if (canFallback) {
                // It isn't an astral recipe, but it might still be a vanilla recipe
                // that happens to map to the same astral recipe translation,
                // so unfortunately we have to check that here.
                Optional<Recipe> fallbackRecipe = evaluator.fallbackToVanilla(inventory.getMatrix(), strategies);
                if (fallbackRecipe.isPresent()) {
                    inventory.setResult(fallbackRecipe.get().getResult().clone());
                    return; // Don't clear result
                }
            }

            inventory.setResult(null);
        }
    }

    private ItemStack applyAstralRecipes(FurnaceInventory inventory, boolean canFallback) {
        AstralRecipeEvaluator evaluator = plugin.getRecipeEvaluator();

        // First determine the strategies we can use
        List<Strategy<? extends Recipe>> strategies;
        switch (inventory.getType()) {
            case FURNACE:
                strategies = Lists.newArrayList(
                        new AstralFurnaceRecipeStrategy()
                );
                break;
            case BLAST_FURNACE:
                strategies = Lists.newArrayList(
                        new AstralBlastingRecipeStrategy()
                );
                break;
            case SMOKER:
                strategies = Lists.newArrayList(
                        new AstralSmokingRecipeStrategy()
                );
                break;
            default:
                plugin.getLogger().warning("Skipping unsupported inventory type for smelting: " + inventory.getType());
                return null;
        }

        // Now try to match against the recipes
        Optional<Recipe> recipe = evaluator.matchRecipe(new ItemStack[]{inventory.getSmelting()}, strategies);

        if (recipe.isPresent()) {

            return recipe.get().getResult().clone();
        } else {
            if (canFallback) {
                // It isn't an astral recipe, but it might still be a vanilla recipe
                // that happens to map to the same astral recipe translation,
                // so unfortunately we have to check that here.
                Optional<Recipe> fallbackRecipe = evaluator.fallbackToVanilla(new ItemStack[]{inventory.getSmelting()}, strategies);
                if (fallbackRecipe.isPresent()) {
                    return fallbackRecipe.get().getResult().clone();
                }
            }

            return null;
        }
    }


}
