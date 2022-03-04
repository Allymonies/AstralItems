package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import io.astralforge.astralitems.AstralItems;

public class BasicBlockStateManager {

    private AstralItems plugin;
    private ChunkStorage chunkStorage;
    private Map<Block, AstralBasicBlockSpec> tickCache = new HashMap<>();

    public BasicBlockStateManager(AstralItems plugin) {
        this.plugin = plugin;
        this.chunkStorage = new ChunkStorage(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entry<Block, AstralBasicBlockSpec> entry : tickCache.entrySet()) {
                    if (entry.getValue().tickHandler != null) {
                        entry.getValue().tickHandler.tick(entry.getKey(), entry.getValue());
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void addToTickCache(Block block, AstralBasicBlockSpec spec) {
        tickCache.put(block, spec);
    }

    public void removeFromTickCache(Block block) {
        tickCache.remove(block);
    }

    public void addChunkToTickCache(Chunk chunk) {
        getAstralBlockSpecLocationsFromChunk(chunk).forEach(blockSpecLocation -> {
            if (blockSpecLocation.blockSpec.tickHandler != null) {
                tickCache.put(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation), blockSpecLocation.blockSpec);
            }
        });
    }

    public void removeChunkFromTickCache(Chunk chunk) {
        getAstralBlockSpecLocationsFromChunk(chunk).forEach(blockSpecLocation -> {
            tickCache.remove(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation));
        });
    }

    public void refreshTickCache() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                addChunkToTickCache(chunk);
            }
        }
    }

    public void processBlockPlacement(AstralBasicBlockSpec spec, Block block) {
        ChunkAstralBlock meta = new ChunkAstralBlock(spec.itemSpec.id, new byte[0]);
        chunkStorage.setMeta(block.getChunk(), block.getX(), block.getY(), block.getZ(), meta);
        if (spec.tickHandler != null) {
            tickCache.put(block, spec);
        }
    }

    public Optional<AstralBasicBlockSpec> getSpecFromBlock(Block block) {
        Optional<ChunkAstralBlock> meta = chunkStorage.getMeta(block.getChunk(), block.getX(), block.getY(), block.getZ());
        return meta.map(chunkAstralBlock ->
            (AstralBasicBlockSpec) plugin.getAstralBlock(chunkAstralBlock.key)
        );
    }

    public boolean isPlaceholder(BlockState blockState) {
        Optional<ChunkAstralBlock> chunkBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (chunkBlock.isPresent()) {
            AbstractAstralBlockSpec spec = plugin.getAstralBlock(chunkBlock.get().key);
            return spec instanceof AstralPlaceholderBlockSpec;
        }
        return false;
    }

    public Optional<AbstractAstralBlockSpec> forceProcessBlockRemoval(BlockState blockState) {
        Optional<ChunkAstralBlock> chunkBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (chunkBlock.isPresent()) {

            AbstractAstralBlockSpec spec = plugin.getAstralBlock(chunkBlock.get().key);
            chunkStorage.removeMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
            tickCache.remove(blockState.getBlock());
            return Optional.of(spec);
        }

        return Optional.empty();
    }
    
    public Optional<AbstractAstralBlockSpec> processBlockRemoval(BlockState blockState) {
        Optional<ChunkAstralBlock> chunkBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (chunkBlock.isPresent()) {

            AbstractAstralBlockSpec spec = plugin.getAstralBlock(chunkBlock.get().key);
            if (spec instanceof AstralBasicBlockSpec) {
                chunkStorage.removeMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
                tickCache.remove(blockState.getBlock());
                return Optional.of(spec);
            } else if (spec instanceof AstralPlaceholderBlockSpec) {
                return Optional.of(spec);
            }
        }

        return Optional.empty();
    }

    public List<AstralBasicBlock> getAstralBlockSpecLocationsFromChunk(Chunk chunk) {
        List<AstralBasicBlock> blockSpecLocations = new ArrayList<AstralBasicBlock>();

        List<ChunkStorage.ChunkAstralBlockLocation> chunkAstralBlocks = chunkStorage.getChunkAstralBlocks(chunk);
        for (ChunkStorage.ChunkAstralBlockLocation chunkAstralBlock : chunkAstralBlocks) {
            AbstractAstralBlockSpec spec = plugin.getAstralBlock(chunkAstralBlock.chunkAstralBlock.key);
            if (spec instanceof AstralBasicBlockSpec) {
                blockSpecLocations.add(new AstralBasicBlock((AstralBasicBlockSpec)spec, chunkAstralBlock.blockLocation));
            }
        }

        return blockSpecLocations;
    }

    public static final class AstralBasicBlock {
        public final AstralBasicBlockSpec blockSpec;
        public final Location blockLocation;

        public AstralBasicBlock(AstralBasicBlockSpec astralBasicBlockSpec, Location blockLocation) {
            this.blockSpec = astralBasicBlockSpec;
            this.blockLocation = blockLocation;
        }
    }

}
