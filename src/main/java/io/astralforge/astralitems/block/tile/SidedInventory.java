package io.astralforge.astralitems.block.tile;

import org.bukkit.block.BlockFace;

public interface SidedInventory extends InventoryHolder {
    ItemHandler getItemHandler(BlockFace side);
}
