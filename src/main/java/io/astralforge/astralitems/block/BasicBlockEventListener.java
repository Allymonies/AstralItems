package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.astralforge.astralitems.AstralItems;

public class BasicBlockEventListener implements Listener {

    private final AstralItems plugin;
    private final BasicBlockStateManager basicBlockStateManager;

    private Map<Location, AstralBasicBlockSpec> blockCache = new HashMap<>();

    public BasicBlockEventListener(AstralItems plugin, BasicBlockStateManager basicBlockStateManager) {
        this.plugin = plugin;
        this.basicBlockStateManager = basicBlockStateManager;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                //plugin.getLogger().info("Feeding cache with " + chunk.getX() + "," + chunk.getZ());
                basicBlockStateManager.getChunkAstralBlockSpecLocations(chunk).forEach(blockSpecLocation -> {
                    blockCache.put(blockSpecLocation.blockLocation, blockSpecLocation.blockSpec);
                });
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entry<Location, AstralBasicBlockSpec> entry : blockCache.entrySet()) {
                    entry.getKey().getWorld().spawnParticle(Particle.HEART, entry.getKey().clone().add(0.5, 0.5, 0.5), 2, 0.5, 0.5, 0.5);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
        new BukkitRunnable() {
            boolean place = false;
            @Override
            public void run() {
                place = !place;
                World world = Bukkit.getWorlds().get(0);
                for (int y = 112; y <= (112 + 8); y++) {
                    for (int x = -16; x <= -16; x++) {
                        for (int z = -64; z <= -49; z++) {
                            if (place) {
                                AbstractAstralBlockSpec spec = plugin.getAstralBlock(new NamespacedKey(plugin, "gay_block"));
                                Block a = world.getBlockAt(new Location(world, x, y, z));
                                basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) spec, a);
                            } else {
                                Block a = world.getBlockAt(new Location(world, x, y, z));
                                Optional<AstralBasicBlockSpec> spec = basicBlockStateManager.processBlockRemoval(a.getState());
                            }
                            
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockBreak(BlockBreakEvent event) {
        //plugin.getLogger().info("Block break event for " + event.getBlock().getLocation().toString());
        BlockState state = event.getBlock().getState();
        Optional<AstralBasicBlockSpec> spec = basicBlockStateManager.processBlockRemoval(state);
        if (spec.isPresent() && event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack item = spec.get().itemSpec.createItemStack();
            event.setDropItems(false); // To prevent sync issues with ChunkStorage
            state.getWorld().dropItemNaturally(state.getLocation(), item);
            blockCache.remove(state.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        AbstractAstralBlockSpec spec = plugin.getAstralBlock(item);
        if (spec != null) {
            //plugin.getLogger().info("Block place event for " + event.getBlock().getLocation().toString() + " with " + spec.getClass().getName());
            basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) spec, event.getBlock());
            blockCache.put(event.getBlock().getLocation(), (AstralBasicBlockSpec) spec);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkLoad(ChunkLoadEvent event) {
        List<BasicBlockStateManager.AstralBlockSpecLocation> astralBlockSpecLocations;
        astralBlockSpecLocations = basicBlockStateManager.getChunkAstralBlockSpecLocations(event.getChunk());
        for (BasicBlockStateManager.AstralBlockSpecLocation astralBlockSpecLocation : astralBlockSpecLocations) {
            blockCache.put(astralBlockSpecLocation.blockLocation, astralBlockSpecLocation.blockSpec);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<BasicBlockStateManager.AstralBlockSpecLocation> astralBlockSpecLocations;
        astralBlockSpecLocations = basicBlockStateManager.getChunkAstralBlockSpecLocations(event.getChunk());
        for (BasicBlockStateManager.AstralBlockSpecLocation astralBlockSpecLocation : astralBlockSpecLocations) {
            blockCache.remove(astralBlockSpecLocation.blockLocation);
        }
    }
    
}
