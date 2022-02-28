package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class AstralShapelessRecipeStrategy implements AstralRecipeEvaluator.Strategy<ShapelessRecipe> {

    @Override
    public boolean test(ItemStack[] craftingMatrix, ShapelessRecipe recipe) {
        List<ItemStack> ingredients = new ArrayList<ItemStack>();

        for (int i = 0; i < craftingMatrix.length; i++) {
            ItemStack item = craftingMatrix[i];
            if (item != null && item.getType() != Material.AIR) {
                ingredients.add(item);
            }
        }

        // Make sure we have the same amount of ingredients as the recipe
        if (ingredients.size() != recipe.getIngredientList().size()) {
            return false;
        }

        // Make sure all recipe ingredients are in the crafting matrix
        for (RecipeChoice requiredIngredient : recipe.getChoiceList()) {
            boolean found = false;
            for (int i = 0; i < ingredients.size(); i++) {
                ItemStack item = ingredients.get(i);
                if (requiredIngredient.test(item)) {
                    ingredients.remove(i);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Class<ShapelessRecipe> getRecipeType() {
        return ShapelessRecipe.class;
    }
    
}
