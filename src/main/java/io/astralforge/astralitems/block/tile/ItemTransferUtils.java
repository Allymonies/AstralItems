package io.astralforge.astralitems.block.tile;

import io.astralforge.astralitems.AstralItems;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ItemTransferUtils {
    public static int getSize(Block block, BlockFace side) {
        return 0;
    }

    public static ItemStack getItem(Block block, BlockFace side, int slot) {
        return null;
    }

    private static ItemHandler getItemHandler(AstralTileEntity tileEntity, BlockFace side) {
        if (tileEntity instanceof SidedInventory) {
            return ((SidedInventory) tileEntity).getItemHandler(side);
        } else if (tileEntity instanceof InventoryHolder) {
            return ((InventoryHolder) tileEntity).getItemHandler();
        }
        return null;
    }

    public static ItemStack extractItem(Block block, BlockFace side, int slot, int amount) {
        Optional<AstralTileEntity> blockTileEntityOpt = AstralItems.getInstance().getTileEntity(block);
        if (blockTileEntityOpt.isPresent()) {
            AstralTileEntity blockTileEntity = blockTileEntityOpt.get();
            ItemHandler itemHandler = getItemHandler(blockTileEntity, side);
            if (itemHandler == null) {
                return null;
            }
            return itemHandler.extractItem(slot, amount);
        } else {
            // Vanilla fallback

        }
        return null;
    }

    public static ItemStack insertItem(Block block, BlockFace side, int slot, ItemStack item) {
        return null;
    }
}
