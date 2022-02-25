package io.astralforge.astralitems.block;

import java.util.Arrays;
import java.util.Optional;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ChunkStorage {
    private Plugin plugin;
    private NamespacedKey blocksKey;

    public ChunkStorage(Plugin plugin) {
        this.plugin = plugin;
        this.blocksKey = new NamespacedKey(plugin, "basic-blocks");
    }

    public Optional<ChunkAstralBlock> getMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        plugin.getLogger().info("container: " + container.getKeys());
        PersistentDataContainer blocks = container.get(blocksKey, PersistentDataType.TAG_CONTAINER);
        if (blocks == null) {
            return Optional.empty();
        }

        plugin.getLogger().info("blocks: " + blocks.getKeys());

        try {
            ChunkAstralBlock block = blocks.get(
                new NamespacedKey(plugin, chunkLocation.toString()), 
                ChunkAstralBlock.Serial.get()
            );

            return Optional.ofNullable(block);
        } catch (IllegalArgumentException e) {
            // Corrupt?
            plugin.getLogger().warning(
                "Corrupt chunk data for " + chunk.getWorld().getName() + ":" + 
                chunk.getX() + "," + chunk.getZ() + " at " + chunkLocation.toString()
            );
        }

        return Optional.empty();
    }

    public void setMeta(Chunk chunk, int worldX, int worldY, int worldZ, ChunkAstralBlock blockMeta) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        PersistentDataContainer blocks = container.get(blocksKey, PersistentDataType.TAG_CONTAINER);
        if (blocks == null) {
            blocks = container.getAdapterContext().newPersistentDataContainer();
        }

        blocks.set(
            new NamespacedKey(plugin, chunkLocation.toString()), 
            ChunkAstralBlock.Serial.get(), 
            blockMeta
        );

        // Flush
        container.set(blocksKey, PersistentDataType.TAG_CONTAINER, blocks);
    }

    public void removeMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        PersistentDataContainer blocks = container.get(blocksKey, PersistentDataType.TAG_CONTAINER);
        if (blocks == null) {
            return;
        }

        blocks.remove(new NamespacedKey(plugin, chunkLocation.toString()));
        container.set(blocksKey, PersistentDataType.TAG_CONTAINER, blocks);
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
}
