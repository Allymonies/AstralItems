package io.astralforge.astralitems.block.tile;

import org.bukkit.inventory.ItemStack;

public interface ItemHandler {
    // Inspired by Forge IItemHandler
    int getSize();
    ItemStack getItem(int slot);
    void setItem(int slot, ItemStack stack);
    ItemStack extractItem(int slot, int amount, boolean simulate);
    ItemStack extractItem(int slot, int amount);
    ItemStack extractItem(int amount);
    ItemStack insertItem(int slot, ItemStack item);
    ItemStack insertItem(int slot, ItemStack item, boolean simulate);
    ItemStack insertItem(ItemStack item);
}
