package io.astralforge.astralitems;

import de.tr7zw.nbtapi.NBTTileEntity;
import io.astralforge.astralitems.block.tile.AstralTileEntity;
import io.astralforge.astralitems.block.tile.InventoryHolder;
import io.astralforge.astralitems.block.tile.ItemHandler;
import io.astralforge.astralitems.block.tile.SidedInventory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class HopperSimulationTask implements Runnable {

    private final BlockData enabledHopper = Bukkit.getServer().createBlockData("minecraft:hopper[enabled=true]");
    private final BlockData downHopper = Bukkit.getServer().createBlockData("minecraft:hopper[facing=down]");
    private final BlockData eastHopper = Bukkit.getServer().createBlockData("minecraft:hopper[facing=east]");
    private final BlockData westHopper = Bukkit.getServer().createBlockData("minecraft:hopper[facing=west]");
    private final BlockData northHopper = Bukkit.getServer().createBlockData("minecraft:hopper[facing=north]");
    private final BlockData southHopper = Bukkit.getServer().createBlockData("minecraft:hopper[facing=south]");
    private final int hopperCacheDuration = 20 * 5; // 5 Seconds
    private int cacheCounter = hopperCacheDuration;
    private AstralItems plugin;
    private HashSet<Hopper> dedupeHoppers = new HashSet<>();
    private ArrayList<Hopper> hopperCache = new ArrayList<>();
    private HashMap<World, Integer> worldTicksPerTransfer = new HashMap<>();
    private HashMap<World, Integer> worldHopperAmount = new HashMap<>();

    HopperSimulationTask(AstralItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        cacheCounter++;
        if (cacheCounter >= hopperCacheDuration) {
            cacheCounter = 0;
            hopperCache.clear();
            Bukkit.getServer().getWorlds().forEach(world -> {
                int ticksPerHopperTransfer = Bukkit.spigot().getConfig().getInt("world-settings.default.ticks-per.hopper-transfer", 8);
                int hopperAmount = Bukkit.spigot().getConfig().getInt("world-settings.default.hopper-amount", 1);
                if (Bukkit.spigot().getConfig().contains("world-settings." + world.getName() + ".ticks-per.hopper-transfer")) {
                    ticksPerHopperTransfer = Bukkit.spigot().getConfig().getInt("world-settings." + world.getName() + ".ticks-per.hopper-transfer", 8);
                }
                if (Bukkit.spigot().getConfig().contains("world-settings." + world.getName() + ".hopper-amount")) {
                    hopperAmount = Bukkit.spigot().getConfig().getInt("world-settings." + world.getName() + ".hopper-amount", 1);
                }
                worldTicksPerTransfer.put(world, ticksPerHopperTransfer);
                worldHopperAmount.put(world, hopperAmount);

                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState tileEntity : chunk.getTileEntities()) {
                        if (tileEntity instanceof Hopper) {
                            hopperCache.add((Hopper) tileEntity);
                        }
                    }
                }
            });
        }

        for (Hopper hopper : hopperCache) {
            if (dedupeHoppers.contains(hopper)) continue;
            if (hopper.getBlock().getType() != Material.HOPPER) continue;
            BlockData blockData = hopper.getBlockData();
            if (!blockData.matches(enabledHopper)) {
                // Hopper is disabled, don't need to simulate it
                continue;
            }
            NBTTileEntity hopperNBT = new NBTTileEntity(hopper);

            Integer transferCooldown = hopperNBT.getInteger("TransferCooldown");

            if (transferCooldown > 1) {
                // Hopper isn't ready to transfer yet
                continue;
            }

            Block destinationBlock = null;
            BlockFace sourceBlockFace = null;
            if (blockData.matches(downHopper)) {
                destinationBlock = hopper.getLocation().add(0, -1, 0).getBlock();
                sourceBlockFace = BlockFace.UP;
            } else if (blockData.matches(eastHopper)) {
                destinationBlock = hopper.getLocation().add(1, 0, 0).getBlock();
                sourceBlockFace = BlockFace.WEST;
            } else if (blockData.matches(westHopper)) {
                destinationBlock = hopper.getLocation().add(-1, 0, 0).getBlock();
                sourceBlockFace = BlockFace.EAST;
            } else if (blockData.matches(northHopper)) {
                destinationBlock = hopper.getLocation().add(0, 0, -1).getBlock();
                sourceBlockFace = BlockFace.SOUTH;
            } else if (blockData.matches(southHopper)) {
                destinationBlock = hopper.getLocation().add(0, 0, 1).getBlock();
                sourceBlockFace = BlockFace.NORTH;
            }

            boolean didOperation = false;

            if (destinationBlock != null) {
                Optional<AstralTileEntity> optTileEntity = plugin.getTileEntity(destinationBlock);
                if (optTileEntity.isPresent()) {
                    didOperation = processInsertion(hopper, worldHopperAmount.get(hopper.getWorld()), optTileEntity.get(), sourceBlockFace);
                }
            }

            Block extractionBlock = hopper.getLocation().add(0, 1, 0).getBlock();
            if (extractionBlock.getType() != Material.AIR) {
                Optional<AstralTileEntity> optTileEntity = plugin.getTileEntity(extractionBlock);
                if (optTileEntity.isPresent()) {
                    didOperation = didOperation || processExtraction(hopper, worldHopperAmount.get(hopper.getWorld()), optTileEntity.get(), BlockFace.DOWN);
                }
            }

            if (didOperation) {
                int finalTicksPerHopperTransfer = worldTicksPerTransfer.get(hopper.getWorld());
                dedupeHoppers.add(hopper);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    dedupeHoppers.remove(hopper);
                    hopperNBT.setInteger("TransferCooldown", finalTicksPerHopperTransfer);
                }, 1);
            }


            //Bukkit.getLogger().info("Hopper reader for transfer at " + hopper.getLocation().toString());
            //hopperNBT.setInteger("TransferCooldown", ticksPerHopperTransfer + 1);
        }
    }

    public boolean processInsertion(Hopper hopper, int hopperAmount, AstralTileEntity astralTileEntity, BlockFace face) {
        ItemHandler handler = null;
        boolean transferredItems = false;
        if (astralTileEntity instanceof SidedInventory) {
            handler = ((SidedInventory) astralTileEntity).getItemHandler(face);
        } else if (astralTileEntity instanceof InventoryHolder) {
            handler = ((InventoryHolder) astralTileEntity).getItemHandler();
        }
        if (handler == null) return false;
        for (int i = 0; i < hopper.getInventory().getSize(); i++) {
            ItemStack itemStack = hopper.getInventory().getItem(i);
            if (itemStack == null || itemStack.getAmount() == 0) continue;
            itemStack = itemStack.clone();
            int remainderAmount = 0;
            if (itemStack.getAmount() > hopperAmount) {
                remainderAmount = itemStack.getAmount() - hopperAmount;
                itemStack.setAmount(hopperAmount);
            }
            ItemStack remainder = handler.insertItem(itemStack);
            if (remainder == null) {
                transferredItems = true;
                itemStack.setAmount(remainderAmount);
                if (itemStack.getAmount() > 0) {
                    hopper.getInventory().setItem(i, itemStack);
                } else {
                    hopper.getInventory().setItem(i, null);
                }
                break;
            } else if (!remainder.equals(itemStack)) {
                transferredItems = true;
                itemStack.setAmount(remainderAmount + remainder.getAmount());
                hopper.getInventory().setItem(i, itemStack);
                break;
            }
        }
        return transferredItems;
    }

    private boolean processExtraction(Hopper hopper, int hopperAmount, AstralTileEntity astralTileEntity, BlockFace sourceBlockFace) {
        ItemHandler handler = null;
        boolean transferredItems = false;
        if (astralTileEntity instanceof SidedInventory) {
            handler = ((SidedInventory) astralTileEntity).getItemHandler(sourceBlockFace);
        } else if (astralTileEntity instanceof InventoryHolder) {
            handler = ((InventoryHolder) astralTileEntity).getItemHandler();
        }
        if (handler == null) return false;
        for (int i = 0; i < handler.getSize(); i++) {
            ItemStack itemStack = handler.getItem(i);
            if (itemStack == null || itemStack.getAmount() == 0) continue;
            int remainderAmount = 0;
            if (itemStack.getAmount() > hopperAmount) {
                remainderAmount = itemStack.getAmount() - hopperAmount;
                itemStack.setAmount(hopperAmount);
            }
            HashMap<Integer, ItemStack> leftover = hopper.getInventory().addItem(itemStack);
            if (!leftover.isEmpty()) {
                if (leftover.get(0).getAmount() < itemStack.getAmount()) {
                    transferredItems = true;
                }
                remainderAmount += leftover.get(0).getAmount();
            } else {
                transferredItems = true;
            }
            if (remainderAmount > 0) {
                itemStack.setAmount(remainderAmount);
                handler.setItem(i, itemStack);
            } else if (remainderAmount == 0) {
                handler.setItem(i, null);
            }

        }
        return transferredItems;
    }
}
