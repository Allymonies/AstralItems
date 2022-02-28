package io.astralforge.astralitems.recipe;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AstralShapedRecipeStrategy implements AstralRecipeEvaluator.Strategy<ShapedRecipe> {

    private int matrixWidth;
    private int matrixHeight;

    private Optional<ItemStack> getBounded(ItemStack[] craftingMatrix, int x, int y) {
        if (x < 0 || x >= matrixWidth || y < 0 || y >= matrixHeight) {
            return Optional.empty();
        }

        ItemStack item = craftingMatrix[x + y * matrixWidth];
        if (item == null) {
            item = new ItemStack(Material.AIR);
            item.setAmount(0); // Air moment
        }

        return Optional.of(item);
    }

    private Optional<Boolean> matchesRecipe(ItemStack[] craftingMatrix, ShapedRecipe recipe, int xOffset, int yOffset) {
        String[] shape = recipe.getShape();
        for (int shapeRow = 0; shapeRow < shape.length; shapeRow++) {
            String shapeRowString = shape[shapeRow];
            for (int shapeCol = 0; shapeCol < shapeRowString.length(); shapeCol++) {
                char shapeChar = shapeRowString.charAt(shapeCol);

                Optional<ItemStack> item = getBounded(craftingMatrix, xOffset + shapeCol, yOffset + shapeRow);
                
                RecipeChoice recipeItem = recipe.getChoiceMap().get(shapeChar);
                if (item.isPresent()) {
                    // In bounds

                    if (recipeItem != null) {

                        if (recipeItem.test(item.get())) {
                            // Matches
                        } else {
                            return Optional.of(false);
                        }

                    } else {

                        // Air
                        if (item.get().getType() == Material.AIR) {
                            // Matches
                        } else {
                            return Optional.of(false);
                        }

                    }
                } else {
                    // Out of bounds

                    // If the item is not AIR, then it's not a match
                    if (recipeItem != null) return Optional.empty();
                }
            }
        }

        // We never failed anywhere, so it must be a match!
        return Optional.of(true);
    }

    @Override
    public boolean test(ItemStack[] craftingMatrix, ShapedRecipe recipe) {
        for (int xOffset = 0; xOffset < matrixWidth; xOffset++) {
            for (int yOffset = 0; yOffset < matrixHeight; yOffset++) {

                // Try to match the recipe at this offset
                Optional<Boolean> result = matchesRecipe(craftingMatrix, recipe, xOffset, yOffset);
                if (result.isPresent()) {
                    if (result.get()) {
                        return true;
                    }
                } else {
                    if (yOffset == 0) {
                        // If we're at the top row, we can't match the recipe
                        return false;
                    }

                    break;
                }
            }
        }

        return false;
    }

    @Override
    public Class<ShapedRecipe> getRecipeType() {
        return ShapedRecipe.class;
    }
    
}
