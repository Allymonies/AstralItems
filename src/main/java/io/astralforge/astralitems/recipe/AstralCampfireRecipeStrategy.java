package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.*;

public class AstralCampfireRecipeStrategy implements AstralRecipeEvaluator.Strategy<CampfireRecipe> {

    @Override
    public boolean test(ItemStack[] craftingMatrix, CampfireRecipe recipe) {
        RecipeChoice inputChoice = recipe.getInputChoice();

        if (craftingMatrix[0] == null) {
            craftingMatrix[0] = new ItemStack(Material.AIR);
        }

        return craftingMatrix.length == 1 && inputChoice.test(craftingMatrix[0]);
    }

    @Override
    public Class<CampfireRecipe> getRecipeType() {
        return CampfireRecipe.class;
    }

}
