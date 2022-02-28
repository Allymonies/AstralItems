package io.astralforge.astralitems.recipe;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public class AstralRecipeTranslations {
    private Map<Class<? extends Recipe>, RecipeTranslator<? extends Recipe>> recipeTranslators = new HashMap<>();
    
    // Default translators
    public AstralRecipeTranslations() {
        recipeTranslators.put(ShapedRecipe.class, new ShapedRecipeTranslator());
        recipeTranslators.put(ShapelessRecipe.class, new ShapelessRecipeTranslator());
    }

    public <RecipeType extends Recipe> void registerTranslator(Class<RecipeType> recipeClass, RecipeTranslator<RecipeType> recipeTranslator) {
        recipeTranslators.put(recipeClass, recipeTranslator);
    }

    @SuppressWarnings("unchecked")
    public <RecipeType extends Recipe> RecipeType translateRecipe(RecipeType recipe) {
        RecipeTranslator<? extends Recipe> translator = recipeTranslators.get(recipe.getClass());
        if (translator == null) {
            throw new UnsupportedOperationException("No translator found for recipe class " + recipe.getClass());
        }

        if (translator.getRecipeType() != recipe.getClass()) {
            throw new IllegalArgumentException("Recipe class " + recipe.getClass() + " does not match translator class " + translator.getRecipeType());
        }

        return ((RecipeTranslator<RecipeType>) translator).translateRecipe(recipe);
    }

    public interface RecipeTranslator<RecipeType extends Recipe> {
        RecipeType translateRecipe(RecipeType recipe);

        Class<RecipeType> getRecipeType();
    }

    public static final class ShapedRecipeTranslator implements RecipeTranslator<ShapedRecipe> {
        @Override
        public ShapedRecipe translateRecipe(ShapedRecipe recipe) {
            ShapedRecipe vanillaRecipe = new ShapedRecipe(
                    recipe.getKey(), 
                    recipe.getResult()
                )
                .shape(recipe.getShape());

            vanillaRecipe.setGroup(recipe.getGroup());

            for (Map.Entry<Character, RecipeChoice> entry : recipe.getChoiceMap().entrySet()) {
                vanillaRecipe.setIngredient(entry.getKey(), 
                    AstralRecipeChoice.translateToVanilla(entry.getValue())
                );
            }

            return vanillaRecipe;
        }

        @Override
        public Class<ShapedRecipe> getRecipeType() {
            return ShapedRecipe.class;
        }
    }

    public static final class ShapelessRecipeTranslator implements RecipeTranslator<ShapelessRecipe> {
        @Override
        public ShapelessRecipe translateRecipe(ShapelessRecipe recipe) {
            ShapelessRecipe vanillaRecipe = new ShapelessRecipe(
                    recipe.getKey(), 
                    recipe.getResult()
                );

            vanillaRecipe.setGroup(recipe.getGroup());

            for (RecipeChoice choice : recipe.getChoiceList()) {
                vanillaRecipe.addIngredient(
                    AstralRecipeChoice.translateToVanilla(choice)
                );
            }

            return vanillaRecipe;
        }

        @Override
        public Class<ShapelessRecipe> getRecipeType() {
            return ShapelessRecipe.class;
        }
    }
}
