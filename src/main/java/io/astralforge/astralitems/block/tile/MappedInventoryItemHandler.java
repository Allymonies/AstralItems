package io.astralforge.astralitems.block.tile;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MappedInventoryItemHandler implements ItemHandler {

    Inventory inventory;
    List<Integer> slots;
    ItemTransferHandler itemTransferHandler;

    public MappedInventoryItemHandler(Inventory inventory, List<Integer> slots, ItemTransferHandler itemTransferHandler) {
        this.inventory = inventory;
        this.slots = slots;
        this.itemTransferHandler = itemTransferHandler;
    }

    @Override
    public int getSize() {
        return slots.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slots.get(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slots.get(slot), stack);
        if (itemTransferHandler != null) itemTransferHandler.onItemsTransferred();
    }

    @Override
    public ItemStack extractItem(int slot, int amount) {
        return extractItem(slot, amount, false);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack item = inventory.getItem(slots.get(slot));
        if (item == null) return null;
        if (item.getAmount() >= amount) {
            ItemStack leftover = item.clone();
            leftover.setAmount(item.getAmount() - amount);
            item.setAmount(amount);
            if (!simulate) {
                inventory.setItem(slots.get(slot), item);
                if (itemTransferHandler != null) itemTransferHandler.onItemsTransferred();
            }
        }
        return item;
    }

    @Override
    public ItemStack extractItem(int amount) {
        ItemStack item = null;
        for (int i = 0; i < slots.size(); i++) {
            if (item == null) {
                item = extractItem(i, amount);
                if (item.getAmount() == 0) item = null;
            } else {
                int amountLeft = amount - item.getAmount();
                ItemStack extracted = extractItem(i, amountLeft, true);
                if (extracted != null && extracted.getAmount() > 0 && extracted.isSimilar(item)) {
                    extractItem(i, amountLeft, false);
                    item.setAmount(item.getAmount() + extracted.getAmount());
                }
            }
        }
        return item;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack item) {
        return insertItem(slot, item, false);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack item, boolean simulate) {
        ItemStack existing = inventory.getItem(slots.get(slot));
        if (existing == null) {
            if (!simulate) {
                inventory.setItem(slots.get(slot), item);
                if (itemTransferHandler != null) itemTransferHandler.onItemsTransferred();
            }
            return null;
        }
        if (existing.isSimilar(item)) {
            int spaceAvailable = existing.getMaxStackSize() - existing.getAmount();
            if (spaceAvailable >= item.getAmount()) {
                if (!simulate) {
                    existing.setAmount(existing.getAmount() + item.getAmount());
                    inventory.setItem(slots.get(slot), existing);
                    if (itemTransferHandler != null) itemTransferHandler.onItemsTransferred();
                }
                return null;
            } else {
                if (!simulate) {
                    existing.setAmount(existing.getAmount() + spaceAvailable);
                    inventory.setItem(slots.get(slot), existing);
                    if (itemTransferHandler != null) itemTransferHandler.onItemsTransferred();
                }
                item.setAmount(item.getAmount() - spaceAvailable);
                return item;
            }
        }
        return item;
    }

    @Override
    public ItemStack insertItem(ItemStack item) {
        ItemStack leftover = item.clone();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack insertLeftover = insertItem(i, leftover);
            if (insertLeftover != null) {
                leftover = insertLeftover;
            }
            if (insertLeftover == null || leftover.getAmount() == 0) return null;
        }
        return leftover;
    }
}
