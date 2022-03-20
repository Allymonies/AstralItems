package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.astralforge.astralitems.AstralItems;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ChunkStorage {
    private final AstralItems plugin;
    private final String storageKey;
    private final NamespacedKey idKey;
    private final NamespacedKey dataKey;
    private final NamespacedKey versionKey;

    public ChunkStorage(AstralItems plugin) {
        this.plugin = plugin;
        idKey = new NamespacedKey(plugin, "id");
        dataKey = new NamespacedKey(plugin, "data");
        versionKey = new NamespacedKey(plugin, "version");
        storageKey = "basic-blocks-";
    }

    public Optional<AstralBlock> getMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();

        try {
            NamespacedKey blockKey = new NamespacedKey(plugin, storageKey + chunkLocation.toString());
            PersistentDataContainer blockContainer = container.get(blockKey, PersistentDataType.TAG_CONTAINER);
            if (blockContainer == null) return Optional.empty();

            Location blockLocation = new Location(chunk.getWorld(), worldX, worldY, worldZ);
            NamespacedKey blockId = NamespacedKey.fromString(Objects.requireNonNull(blockContainer.get(idKey, PersistentDataType.STRING)));
            if (blockId == null) return Optional.empty();
            PersistentDataContainer blockData = blockContainer.get(dataKey, PersistentDataType.TAG_CONTAINER);
            AbstractAstralBlockSpec blockSpec = plugin.getAstralBlock(blockId);

            return Optional.of(new AstralBlock(blockSpec, blockLocation, blockData));
        } catch (IllegalArgumentException e) {
            // Corrupt?
            plugin.getLogger().warning(
                "Corrupt chunk data for " + chunk.getWorld().getName() + ":" + 
                chunk.getX() + "," + chunk.getZ() + " at " + chunkLocation.toString()
            );
        }

        return Optional.empty();
    }

    public void setMeta(Chunk chunk, AstralBlock block) {
        int worldX = block.blockLocation.getBlockX();
        int worldY = block.blockLocation.getBlockY();
        int worldZ = block.blockLocation.getBlockZ();
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);
        NamespacedKey blockKey = new NamespacedKey(plugin, storageKey + chunkLocation.toString());

        PersistentDataContainer container = chunk.getPersistentDataContainer();

        PersistentDataContainer blockContainer;
        if (container.has(blockKey, PersistentDataType.TAG_CONTAINER)) {
            blockContainer = container.get(blockKey, PersistentDataType.TAG_CONTAINER);
        } else {
            blockContainer = container.getAdapterContext().newPersistentDataContainer();
        }
        if (blockContainer == null) return;

        blockContainer.set(idKey, PersistentDataType.STRING, block.blockSpec.itemSpec.id.toString());
        blockContainer.set(versionKey, PersistentDataType.INTEGER, 1);
        if (block.data != null) {
            blockContainer.set(dataKey, PersistentDataType.TAG_CONTAINER, block.data);
        }

        container.set(
                blockKey,
                PersistentDataType.TAG_CONTAINER,
                blockContainer
        );
    }

    public void removeMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();

        container.remove(new NamespacedKey(plugin, storageKey + chunkLocation.toString()));
    }

    public PersistentDataAdapterContext getAdapterContext(Chunk chunk) {
        return chunk.getPersistentDataContainer().getAdapterContext();
    }


    private static final class ChunkLocation {
        protected final int x;
        protected final int y;
        protected final int z;

        public ChunkLocation(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String toString() {
            return String.format("%d.%d.%d", x, y, z);
        }
    } 

    private static ChunkLocation worldToChunk(int x, int y, int z) {
        return new ChunkLocation(
            x & 0xF,
            y,
            z & 0xF
        );
    }

    public static final class ChunkAstralBlockLocation {
        public final ChunkAstralBlock chunkAstralBlock;
        public final Location blockLocation;

        public ChunkAstralBlockLocation(ChunkAstralBlock chunkAstralBlock, Location blockLocation) {
            this.chunkAstralBlock = chunkAstralBlock;
            this.blockLocation = blockLocation;
        }
    }

    public void saveChunkAstralBlocks(Chunk chunk, List<AstralBlock> blocks) {
        for (AstralBlock block : blocks) {
            setMeta(
                chunk,
                block
            );
        }
    }

    public List<AstralBlock> getChunkAstralBlocks(Chunk chunk) {
        int chunkCornerX = chunk.getX() << 4;
        int chunkCornerZ = chunk.getZ() << 4;
        List<AstralBlock> blocks = new ArrayList<>();

        //Bukkit.getLogger().info("Getting all chunk astral blocks for chunk " + chunk.getX() + "," + chunk.getZ());

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        
        if (container.isEmpty()) {
            //Bukkit.getLogger().info("Container for this chunk is empty!");
            return blocks;
        }

        for (NamespacedKey key : container.getKeys()) {

            if (key.getNamespace().equalsIgnoreCase(plugin.getName()) && key.getKey().startsWith(storageKey)) {
                if (!container.has(key, PersistentDataType.TAG_CONTAINER)) container.remove(key);
                PersistentDataContainer blockContainer = container.get(key, PersistentDataType.TAG_CONTAINER);
                if (blockContainer == null || blockContainer.isEmpty()) continue;
                NamespacedKey blockId = NamespacedKey.fromString(Objects.requireNonNull(blockContainer.get(
                        idKey,
                        PersistentDataType.STRING)));
                AbstractAstralBlockSpec spec = plugin.getAstralBlock(blockId);

                String[] keyParts = key.getKey().substring(storageKey.length()).split("\\.");

                if (keyParts.length != 3) {
                    Bukkit.getLogger().warning("Invalid key " + key.getKey() + " in chunk " + chunk.getX() + "," + chunk.getZ());
                    Bukkit.getLogger().warning( key.getKey().substring(storageKey.length()) + " " + keyParts.length);
                    continue;
                }

                try {
                    Location blockLocation = new Location(
                        chunk.getWorld(),
                        Integer.parseInt(keyParts[0]) + chunkCornerX,
                        Integer.parseInt(keyParts[1]),
                        Integer.parseInt(keyParts[2]) + chunkCornerZ
                        
                    );
                    if (blockContainer.has(dataKey, PersistentDataType.TAG_CONTAINER)) {
                        PersistentDataContainer blockData = blockContainer.get(dataKey, PersistentDataType.TAG_CONTAINER);
                        blocks.add(new AstralBlock(spec, blockLocation, blockData));
                    } else {
                        if (spec.tileEntityBuilder != null) {
                            plugin.getLogger().info("Tile entity @ " + blockLocation.toString() + " has no data!");
                            continue;
                        }
                        blocks.add(new AstralBlock(spec, blockLocation, null));
                    }

                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Invalid key " + key.getKey() + " in chunk " + chunk.getX() + "," + chunk.getZ());
                    continue;
                }
            }
        }

        return blocks;
    }
}
