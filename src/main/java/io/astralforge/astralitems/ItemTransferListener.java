package io.astralforge.astralitems;

import io.astralforge.astralitems.block.AbstractAstralBlockSpec;
import io.astralforge.astralitems.block.tile.AstralTileEntity;
import io.astralforge.astralitems.block.tile.InventoryHolder;
import io.astralforge.astralitems.block.tile.ItemHandler;
import io.astralforge.astralitems.block.tile.SidedInventory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ItemTransferListener implements Listener {
    private final BlockData westDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=west]");
    private final BlockData eastDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=east]");
    private final BlockData downDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=down]");
    private final BlockData upDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=up]");
    private final BlockData northDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=north]");
    private final BlockData southDropper = Bukkit.getServer().createBlockData("minecraft:dropper[facing=south]");

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        // TODO: Handle droppers dropping into a valid inventory
        Location destinationLocation = event.getDestination().getLocation();
        if (destinationLocation == null) return;
        Optional<AbstractAstralBlockSpec> blockSpec = AstralItems.getInstance().getAstralBlock(destinationLocation.getBlock());
        if (!blockSpec.isPresent()) return;
        // If the destination block is an astral block, cancel and let other listeners handle the event.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onDispenseEvent(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (event.getBlock().getType() == Material.DROPPER) {
            Block facingBlock = null;
            BlockFace fromFace = null;
            if (event.getBlock().getBlockData().matches(westDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.WEST);
                fromFace = BlockFace.EAST;
            } else if (event.getBlock().getBlockData().matches(eastDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.EAST);
                fromFace = BlockFace.WEST;
            } else if (event.getBlock().getBlockData().matches(downDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.DOWN);
                fromFace = BlockFace.UP;
            } else if (event.getBlock().getBlockData().matches(upDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.UP);
                fromFace = BlockFace.DOWN;
            } else if (event.getBlock().getBlockData().matches(northDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.NORTH);
                fromFace = BlockFace.SOUTH;
            } else if (event.getBlock().getBlockData().matches(southDropper)) {
                facingBlock = event.getBlock().getRelative(BlockFace.SOUTH);
                fromFace = BlockFace.NORTH;
            }
            if (facingBlock == null) return;
            Optional<AstralTileEntity> optAstralTileEntity = AstralItems.getInstance().getTileEntity(facingBlock);
            if (!optAstralTileEntity.isPresent()) return;
            AstralTileEntity tileEntity = optAstralTileEntity.get();
            ItemHandler itemHandler = null;
            if (tileEntity instanceof SidedInventory) {
                itemHandler = ((SidedInventory) tileEntity).getItemHandler(fromFace);
            } else if (tileEntity instanceof InventoryHolder) {
                itemHandler = ((InventoryHolder) tileEntity).getItemHandler();
            }
            if (itemHandler == null) return;
            ItemStack remainder = itemHandler.insertItem(item);
            if (remainder == null || remainder.getType() == Material.AIR) {
                // TODO: Match vanilla mechanics (drop random slot)
                Dropper dropper = (Dropper) event.getBlock().getState();
                Inventory dropperInventory = dropper.getInventory();
                for (int i = 0; i < dropperInventory.getSize(); i++) {
                    ItemStack itemStack = dropperInventory.getItem(i);
                    if (itemStack != null && itemStack.isSimilar(item) && itemStack.getAmount() >= item.getAmount()) {
                        // Remove item that has been dispensed
                        itemStack.setAmount(itemStack.getAmount() - item.getAmount());
                        break;
                    }
                }
                event.setCancelled(true);
            } else {
                event.setItem(remainder);
            }
        }
    }
}
