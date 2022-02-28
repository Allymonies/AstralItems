package io.astralforge.astralitems.recipe;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import io.astralforge.astralitems.AstralItemSpec;
import io.astralforge.astralitems.AstralItems;
import io.astralforge.astralitems.recipe.AstralRecipeEvaluator.Strategy;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CraftingListener implements Listener {

    private AstralItems plugin;

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
}
