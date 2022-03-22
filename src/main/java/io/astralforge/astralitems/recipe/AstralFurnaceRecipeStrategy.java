package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class AstralFurnaceRecipeStrategy implements AstralRecipeEvaluator.Strategy<FurnaceRecipe> {

    @Override
    public boolean test(ItemStack[] craftingMatrix, FurnaceRecipe recipe) {
        RecipeChoice inputChoice = recipe.getInputChoice();

        if (craftingMatrix[0] == null) {
            craftingMatrix[0] = new ItemStack(Material.AIR);
        }

        return craftingMatrix.length == 1 && inputChoice.test(craftingMatrix[0]);
    }

    @Override
    public Class<FurnaceRecipe> getRecipeType() {
        return FurnaceRecipe.class;
    }

}
