package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

public class ChunkStorage {
    private Plugin plugin;
    private final String storageKey;

    public ChunkStorage(Plugin plugin) {
        this.plugin = plugin;
        storageKey = "basic-blocks-";
    }

    public Optional<ChunkAstralBlock> getMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();

        try {
            ChunkAstralBlock block = container.get(
                new NamespacedKey(plugin, storageKey + chunkLocation.toString()), 
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

        container.set(
            new NamespacedKey(plugin, storageKey + chunkLocation.toString()), 
            ChunkAstralBlock.Serial.get(), 
            blockMeta
        );
    }

    public void removeMeta(Chunk chunk, int worldX, int worldY, int worldZ) {
        ChunkLocation chunkLocation = worldToChunk(worldX, worldY, worldZ);

        PersistentDataContainer container = chunk.getPersistentDataContainer();

        container.remove(new NamespacedKey(plugin, storageKey + chunkLocation.toString()));
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

    public List<ChunkAstralBlockLocation> getChunkAstralBlocks(Chunk chunk) {
        int chunkCornerX = chunk.getX() << 4;
        int chunkCornerZ = chunk.getZ() << 4;
        List<ChunkAstralBlockLocation> blocks = new ArrayList<>();

        //Bukkit.getLogger().info("Getting all chunk astral blocks for chunk " + chunk.getX() + "," + chunk.getZ());

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        
        if (container.isEmpty()) {
            //Bukkit.getLogger().info("Container for this chunk is empty!");
            return blocks;
        }

        for (NamespacedKey key : container.getKeys()) {

            if (key.getNamespace().equalsIgnoreCase(plugin.getName()) && key.getKey().startsWith(storageKey)) {
                ChunkAstralBlock block = container.get(
                    key, 
                    ChunkAstralBlock.Serial.get()
                );

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
                    blocks.add(new ChunkAstralBlockLocation(block, blockLocation));
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Invalid key " + key.getKey() + " in chunk " + chunk.getX() + "," + chunk.getZ());
                    continue;
                }
            }
        }

        return blocks;
    }
}
