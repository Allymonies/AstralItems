package io.astralforge.astralitems.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import io.astralforge.astralitems.AstralItemSpec;
import io.astralforge.astralitems.AstralItems;

public interface AstralRecipeChoice extends RecipeChoice {

    public RecipeChoice translateToVanilla();

    public static RecipeChoice translateToVanilla(RecipeChoice choice) {
        if (choice instanceof AstralRecipeChoice) {
            return ((AstralRecipeChoice) choice).translateToVanilla();
        } else {
            return choice;
        }
    }

    // Used for translation purposes only, the implementation can be the same
    public class ExactChoice extends RecipeChoice.ExactChoice implements AstralRecipeChoice {
        public RecipeChoice translateToVanilla() {
            return (RecipeChoice.ExactChoice) this;
        }

    }

    public class MaterialChoice extends RecipeChoice.MaterialChoice implements AstralRecipeChoice {
        Set<AstralItemSpec> astralMaterials = new HashSet<>();

        public RecipeChoice translateToVanilla() {
            Bukkit.getLogger().info(Arrays.toString(this.getChoices().toArray()));
            return new RecipeChoice.MaterialChoice(this.getChoices());
        }

        public MaterialChoice(Material... materials) {
            super(materials);
        }

        public MaterialChoice(AstralItemSpec... materials) {
            super(Lists.newArrayList(Material.AIR));

            this.astralMaterials = new HashSet<>(Arrays.asList(materials));
        }

        public MaterialChoice(Tag<AstralItemSpec> materials) {
            super(Lists.newArrayList(Material.AIR));

            this.astralMaterials = materials.getValues();
        }

        @Override
        public boolean test(ItemStack stack) {
            if (stack == null || stack.getType() == Material.AIR) return false;

            AstralItems plugin = AstralItems.getInstance();
            if (plugin.isAstralItem(stack)) {
                AstralItemSpec item = plugin.getAstralItem(stack);
                return astralMaterials.contains(item);
            }
    
            return super.test(stack);
        }

        @Override
        public List<Material> getChoices() {
            Set<Material> materials = new HashSet<>();
            for (AstralItemSpec item : astralMaterials) {
                materials.add(item.getMaterial());
            }
            materials.addAll(super.getChoices().stream().filter( (material) -> !material.equals(Material.AIR)).collect(java.util.stream.Collectors.toList()));
            return new ArrayList<>(materials);
        }

        public List<Object> getAstralChoices() {
            List<Object> choices = new ArrayList<>();
            choices.addAll(astralMaterials);
            choices.addAll(super.getChoices());
            return choices;
        }
    }

}
