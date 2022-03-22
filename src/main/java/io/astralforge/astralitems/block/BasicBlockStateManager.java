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
import org.bukkit.persistence.PersistentDataContainer;
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

    public AstralTileEntity loadTileEntity(Block block, AstralBlock astralBlock ) {
        if (astralBlock.blockSpec.tileEntityBuilder == null) return null;

        AstralTileEntity tileEntity = astralBlock.blockSpec.tileEntityBuilder.build();
        tileEntity.setLocation(block.getLocation());

        ChunkCoord coord = ChunkCoord.from(block.getLocation());
        Map<Block, AstralTileEntity> chunkTileEntities = tileEntities.computeIfAbsent(coord, k -> new HashMap<>());
        chunkTileEntities.put(block, tileEntity);

        tileEntity.onLoad(astralBlock.data);

        return tileEntity;
    }

    public AstralTileEntity unloadTileEntity(AstralBlock astralBlock) {
        Block block = astralBlock.blockLocation.getBlock();
        ChunkCoord coord = ChunkCoord.from(block.getChunk());
        Map<Block, AstralTileEntity> chunkTileEntities = tileEntities.get(coord);
        if (chunkTileEntities != null) {
            AstralTileEntity tile = chunkTileEntities.remove(block);
            if (tile != null) tile.onUnload(astralBlock.data);
            return tile;
        }
        return null;
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
                AstralTileEntity tileEntity = loadTileEntity(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation), blockSpecLocation);
            }
        });
    }

    public void unloadChunkTileEntities(Chunk chunk) {
        List<AstralBlock> blocks = getAstralBlockSpecLocationsFromChunk(chunk);
        blocks.forEach(blockSpecLocation -> {
//            getAstralBlockSpecLocationsFromChunk(chunk).forEach(blockSpecLocation -> {
//            tickCache.remove(chunk.getWorld().getBlockAt(blockSpecLocation.blockLocation));
            if (blockSpecLocation.blockSpec.tileEntityBuilder != null) {
                AstralTileEntity tileEntity = unloadTileEntity(blockSpecLocation);
            }
        });
        chunkStorage.saveChunkAstralBlocks(chunk, blocks);
    }

    public void unloadAllTileEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                unloadChunkTileEntities(chunk);
            }
        }
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
        PersistentDataContainer data = null;
        if (spec.tileEntityBuilder != null) {
            data = chunkStorage.getAdapterContext(block.getChunk()).newPersistentDataContainer();
        }
        AstralBlock astralBlock = new AstralBlock(spec, block.getLocation(), data);
        chunkStorage.setMeta(block.getChunk(), astralBlock);

        if (spec.tileEntityBuilder != null) {
            loadTileEntity(block, astralBlock);
        }
    }

    public Optional<AbstractAstralBlockSpec> getSpecFromBlock(Block block) {
        Optional<AstralBlock> astralBlock = chunkStorage.getMeta(block.getChunk(), block.getX(), block.getY(), block.getZ());
        return astralBlock.map(value -> value.blockSpec);
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
        Optional<AstralBlock> astralBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (astralBlock.isPresent()) {
            AbstractAstralBlockSpec spec = astralBlock.get().blockSpec;
            return spec instanceof AstralPlaceholderBlockSpec;
        }
        return false;
    }

    public Optional<AbstractAstralBlockSpec> forceProcessBlockRemoval(BlockState blockState) {
        Optional<AstralBlock> astralBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (astralBlock.isPresent()) {

            AbstractAstralBlockSpec spec = astralBlock.get().blockSpec;
            chunkStorage.removeMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
//            tickCache.remove(blockState.getBlock());

            return Optional.of(spec);
        }

        return Optional.empty();
    }
    
    public Optional<AbstractAstralBlockSpec> processBlockRemoval(BlockState blockState) {
        Optional<AstralBlock> astralBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (astralBlock.isPresent()) {

            AbstractAstralBlockSpec spec = astralBlock.get().blockSpec;
            if (spec instanceof AstralBasicBlockSpec) {
                chunkStorage.removeMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
//                tickCache.remove(blockState.getBlock());
                unloadTileEntity(astralBlock.get());
                return Optional.of(spec);
            } else if (spec instanceof AstralPlaceholderBlockSpec) {
                return Optional.of(spec);
            }
        }

        return Optional.empty();
    }

    public List<AstralBlock> getAstralBlockSpecLocationsFromChunk(Chunk chunk) {

        List<AstralBlock> chunkAstralBlocks = chunkStorage.getChunkAstralBlocks(chunk);

        return new ArrayList<>(chunkAstralBlocks);
    }

    /*public static final class AstralBasicBlock extends AstralBlock {
        public final AstralBasicBlockSpec blockSpec;
        public final PersistentDataContainer data;
        public final Location blockLocation;

        public AstralBasicBlock(AstralBasicBlockSpec astralBasicBlockSpec, Location blockLocation, PersistentDataContainer data) {
            super(astralBasicBlockSpec, blockLocation, data);
            this.blockSpec = astralBasicBlockSpec;
            this.blockLocation = blockLocation;
            this.data = data;
        }
    }*/

}
