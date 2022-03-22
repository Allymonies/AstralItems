package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import lombok.Getter;

public class AstralRecipeEvaluator {
    @Getter
    private AstralRecipeTranslations translator = new AstralRecipeTranslations();

    private Map<Class<? extends Recipe>, Set<Recipe>> recipeList = new HashMap<>();
    private Set<NamespacedKey> knownKeys = new HashSet<>();

    public void registerNonVanillaRecipe(Recipe recipe) {
        if (!(recipe instanceof Keyed)) {
            throw new UnsupportedOperationException("Only Keyed recipes are supported at the moment");
        }

        Keyed keyedRecipe = (Keyed) recipe;
        knownKeys.add(keyedRecipe.getKey());

        recipeList.computeIfAbsent(recipe.getClass(), k -> new HashSet<Recipe>()).add(recipe);
    }

    public void registerRecipe(Recipe recipe) {
        if (!(recipe instanceof Keyed)) {
            throw new UnsupportedOperationException("Only Keyed recipes are supported at the moment");
        }

        Keyed keyedRecipe = (Keyed) recipe;
        knownKeys.add(keyedRecipe.getKey());

        // Make sure that the vanilla translation is registered to ensure that the recipe is known by the client
        Recipe vanillaRecipe = translator.translateRecipe(recipe);
        Bukkit.addRecipe(vanillaRecipe);

        recipeList.computeIfAbsent(recipe.getClass(), k -> new HashSet<Recipe>()).add(recipe);
    }

    public void unregisterRecipe(Recipe recipe) {
        if (!isRegistered(recipe)) {
            return;
        }

        if (recipe instanceof Keyed) {
            Keyed keyedRecipe = (Keyed) recipe;
            knownKeys.remove(keyedRecipe.getKey());
            Bukkit.removeRecipe(keyedRecipe.getKey());
        }

        Set<Recipe> recipeSet = recipeList.get(recipe.getClass());
        if (recipeSet != null) {
            recipeSet.remove(recipe);
        }
    }

    public void unregisterAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        for (Map.Entry<Class<? extends Recipe>, Set<Recipe>> entry : recipeList.entrySet()) {
            for (Recipe recipe : entry.getValue()) {
                recipes.add(recipe);
            }
        }

        for (Recipe recipe : recipes) {
            unregisterRecipe(recipe);
        }
    }

    public Optional<Recipe> matchRecipe(ItemStack[] craftingMatrix, List<Strategy<? extends Recipe>> strategyList) {
        for (Strategy<? extends Recipe> strategy : strategyList) {
            Optional<? extends Recipe> recipe = matchRecipe(craftingMatrix, strategy);
            if (recipe.isPresent()) {
                Recipe result = recipe.get();
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }

    public <RecipeType extends Recipe> Optional<RecipeType> matchRecipe(ItemStack[] craftingMatrix, Strategy<RecipeType> strategy) {
        Class<RecipeType> recipeClass = strategy.getRecipeType();
        Set<Recipe> recipes = recipeList.get(recipeClass);
        if (recipes == null) {
            return Optional.empty();
        }

        for (Recipe recipe : recipes) {
            Optional<RecipeType> result = applyStrategy(craftingMatrix, strategy, recipe);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    public  Optional<Recipe> fallbackToVanilla(ItemStack[] craftingMatrix, List<Strategy<? extends Recipe>> strategies) {
        for (Recipe vRecipe : (Iterable<Recipe>) () -> Bukkit.recipeIterator()) {
            if (isRegistered(vRecipe)) {
                continue; // Only evaluating vanilla recipes...
            }

            for (Strategy<? extends Recipe> strategy : strategies) {
                Optional<? extends Recipe> match = applyStrategy(craftingMatrix, strategy, vRecipe);
                if (match.isPresent()) {
                    Recipe result = match.get();
                    return Optional.of(result);
                }
            }
        }

        return Optional.empty();
    }

    private <RecipeType extends Recipe> Optional<RecipeType> applyStrategy(ItemStack[] craftingMatrix, Strategy<RecipeType> strategy, Recipe recipe) {
        Class<RecipeType> recipeType = strategy.getRecipeType();
        if (recipeType.isAssignableFrom(recipe.getClass())) {
            RecipeType typedRecipe = recipeType.cast(recipe);
            
            if (strategy.test(craftingMatrix, typedRecipe)) {
                return Optional.of(typedRecipe); // Finished
            }
        }

        return Optional.empty();
    }

    public boolean isRegistered(Recipe recipe) {
        if (recipe == null) return false;

        if (recipe instanceof Keyed) {
            if (knownKeys.contains(((Keyed) recipe).getKey())) {
                return true;
            }
        }

        Set<Recipe> astralSet = recipeList.get(recipe.getClass());
        if (astralSet != null) {
            if (astralSet.contains(recipe)) {
                return true;
            }
        }

        return false;
    }

    public interface Strategy<RecipeType extends Recipe> {
        public boolean test(ItemStack[] craftingMatrix, RecipeType recipe);

        Class<RecipeType> getRecipeType();
    }
}
