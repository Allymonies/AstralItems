package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.*;

public class AstralSmokingRecipeStrategy implements AstralRecipeEvaluator.Strategy<SmokingRecipe> {

    @Override
    public boolean test(ItemStack[] craftingMatrix, SmokingRecipe recipe) {
        RecipeChoice inputChoice = recipe.getInputChoice();

        if (craftingMatrix[0] == null) {
            craftingMatrix[0] = new ItemStack(Material.AIR);
        }

        return craftingMatrix.length == 1 && inputChoice.test(craftingMatrix[0]);
    }

    @Override
    public Class<SmokingRecipe> getRecipeType() {
        return SmokingRecipe.class;
    }

}
