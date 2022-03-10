package io.astralforge.astralitems.block;

import java.util.*;

import io.astralforge.astralitems.block.tile.AstralTileEntity;
import io.astralforge.astralitems.block.tile.RandomTick;
import io.astralforge.astralitems.mutil.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import io.astralforge.astralitems.AstralItems;

public class BasicBlockStateManager {

    private final AstralItems plugin;
    private final ChunkStorage chunkStorage;
//    private Map<Block, AstralBasicBlockSpec> tickCache = new HashMap<>();
    private final Map<ChunkCoord, Map<Block, AstralTileEntity>> tileEntities = new HashMap<>();

    private final Random random = new Random(System.currentTimeMillis());

    public BasicBlockStateManager(AstralItems plugin) {
        this.plugin = plugin;
        this.chunkStorage = new ChunkStorage(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
//                for (Entry<Block, AstralBasicBlockSpec> entry : tickCache.entrySet()) {
//                    if (entry.getValue().tickHandler != null) {
//                        entry.getValue().tickHandler.tick(entry.getKey(), entry.getValue());
//                    }
//                }
                for (Map<Block, AstralTileEntity> chunk : tileEntities.values()) {
                    for (AstralTileEntity tile : chunk.values()) {
                        double tickChance = 1;
                        try {
                            RandomTick ann = tile.getClass().getMethod("tick").getAnnotation(RandomTick.class);
                            if (ann != null) {
                                tickChance = ann.chance();
                            }
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }

                        if (tickChance >= 1 || random.nextDouble() < tickChance) {
                            tile.tick();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public AstralTileEntity loadTileEntity(Block block, AstralBasicBlockSpec spec) {
        if (spec.tileEntityBuilder == null) return null;

        AstralTileEntity tileEntity = spec.tileEntityBuilder.build();
        tileEntity.setLocation(block.getLocation());

        ChunkCoord coord = ChunkCoord.from(block.getLocation());
        Map<Block, AstralTileEntity> chunkTileEntities = tileEntities.computeIfAbsent(coord, k -> new HashMap<>());
        chunkTileEntities.put(block, tileEntity);

        tileEntity.onLoad();

        return tileEntity;
    }

    public void unloadTileEntity(Block block) {
        ChunkCoord coord = ChunkCoord.from(block.getChunk());
        Map<Block, AstralTileEntity> chunkTileEntities = tileEntities.get(coord);
        if (chunkTileEntities != null) {
            AstralTileEntity tile = chunkTileEntities.remove(block);
            if (tile != null) tile.onUnload();
        }
    }
//
//    public void removeFromTickCache(Block block) {
//        tickCache.remove(block);
//    }

    public void loadChunkTileEntities(Chunk chunk) {
        getAstralBlockSpecLocationsFromChunk(chunk).forEach(blockSpecLocation -> {
//            if (blockSpecLocation.blockSpec.tickHandler != null) {
//                tickCache.put(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation), blockSpecLocation.blockSpec);
//            }
            if (blockSpecLocation.blockSpec.tileEntityBuilder != null) {
                loadTileEntity(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation), blockSpecLocation.blockSpec);
            }
        });
    }

    public void unloadChunkTileEntities(Chunk chunk) {
        getAstralBlockSpecLocationsFromChunk(chunk).forEach(blockSpecLocation -> {
//            tickCache.remove(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation));
            if (blockSpecLocation.blockSpec.tileEntityBuilder != null) {
                unloadTileEntity(blockSpecLocation.blockLocation.getBlock());
            }
        });
    }

    public void refreshTickCache() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                unloadChunkTileEntities(chunk);
                loadChunkTileEntities(chunk);
            }
        }
    }

    public void processBlockPlacement(AstralBasicBlockSpec spec, Block block) {
        ChunkAstralBlock meta = new ChunkAstralBlock(spec.itemSpec.id, new byte[0]);
        chunkStorage.setMeta(block.getChunk(), block.getX(), block.getY(), block.getZ(), meta);
//        if (spec.tickHandler != null) {
//            tickCache.put(block, spec);
//        }
        if (spec.tileEntityBuilder != null) {
            loadTileEntity(block, spec);
        }
    }

    public Optional<AstralBasicBlockSpec> getSpecFromBlock(Block block) {
        Optional<ChunkAstralBlock> meta = chunkStorage.getMeta(block.getChunk(), block.getX(), block.getY(), block.getZ());
        return meta.map(chunkAstralBlock ->
            (AstralBasicBlockSpec) plugin.getAstralBlock(chunkAstralBlock.key)
        );
    }

    public Optional<AstralTileEntity> getTileEntityFromBlock(Block block) {
        ChunkCoord coord = ChunkCoord.from(block.getChunk());
        Map<Block, AstralTileEntity> chunkEntities = tileEntities.get(coord);
        if (chunkEntities != null) {
            return Optional.ofNullable(chunkEntities.get(block));
        }

        return Optional.empty();
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
//            tickCache.remove(blockState.getBlock());

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
//                tickCache.remove(blockState.getBlock());
                unloadTileEntity(blockState.getBlock());
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
