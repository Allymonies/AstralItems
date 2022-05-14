package io.astralforge.astralitems.block.tile;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public interface ItemHandler {
    // Inspired by Forge IItemHandler
    int getSize();
    @Nullable
    ItemStack getItem(int slot);
    void setItem(int slot, ItemStack stack);
    @Nullable
    ItemStack extractItem(int slot, int amount, boolean simulate);
    @Nullable
    ItemStack extractItem(int slot, int amount);
    @Nullable
    ItemStack extractItem(int amount);
    @Nullable
    ItemStack insertItem(int slot, ItemStack item);
    @Nullable
    ItemStack insertItem(int slot, ItemStack item, boolean simulate);
    @Nullable
    ItemStack insertItem(ItemStack item);
}
