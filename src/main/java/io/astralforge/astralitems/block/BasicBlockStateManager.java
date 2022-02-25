package io.astralforge.astralitems.block;

import java.util.Optional;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import io.astralforge.astralitems.AstralItems;

public class BasicBlockStateManager {

    private AstralItems plugin;
    private ChunkStorage chunkStorage;

    public BasicBlockStateManager(AstralItems plugin) {
        this.plugin = plugin;
        this.chunkStorage = new ChunkStorage(plugin);
    }

    public void processBlockPlacement(AstralBasicBlockSpec spec, Block block) {
        ChunkAstralBlock meta = new ChunkAstralBlock(spec.itemSpec.id, new byte[0]);
        chunkStorage.setMeta(block.getChunk(), block.getX(), block.getY(), block.getZ(), meta);
    }
    
    public Optional<AstralBasicBlockSpec> processBlockRemoval(BlockState blockState) {
        Optional<ChunkAstralBlock> chunkBlock = chunkStorage.getMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());
        if (chunkBlock.isPresent()) {
            chunkStorage.removeMeta(blockState.getChunk(), blockState.getX(), blockState.getY(), blockState.getZ());

            AbstractAstralBlockSpec spec = plugin.getAstralBlock(chunkBlock.get().key);
            if (spec instanceof AstralBasicBlockSpec) {
                return Optional.of((AstralBasicBlockSpec) spec);
            }
        }

        return Optional.empty();
    }

}
