package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.Optional;

import io.astralforge.astralitems.block.tile.AstralTileEntity;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.TechnicalPiston;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import io.astralforge.astralitems.AstralItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class BasicBlockEventListener implements Listener {

    private final AstralItems plugin;
    private final BasicBlockStateManager basicBlockStateManager;

    public BasicBlockEventListener(AstralItems plugin, BasicBlockStateManager basicBlockStateManager) {
        this.plugin = plugin;
        this.basicBlockStateManager = basicBlockStateManager;
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockBreak(BlockBreakEvent event) {
        //plugin.getLogger().info("Block break event for " + event.getBlock().getLocation().toString());
        BlockState state = event.getBlock().getState();
        Optional<AstralTileEntity> tileEntity = basicBlockStateManager.getTileEntityFromBlock(event.getBlock());
        Optional<AstralBlock> astralBlock = basicBlockStateManager.processBlockRemoval(state);
        if (astralBlock.isPresent() && astralBlock.get().blockSpec instanceof AstralPlaceholderBlockSpec) {
            event.setCancelled(true);
            BaseComponent[] message = new ComponentBuilder(astralBlock.get().blockSpec.itemSpec.id.toString())
                .color(ChatColor.GRAY)
                .append(" at ").color(ChatColor.RED)
                .append(event.getBlock().getX() + " " + event.getBlock().getY() + " " + event.getBlock().getZ()).color(ChatColor.GRAY)
                .append(" is not registered, please contact your server owner and/or plugin developer.")
                .color(ChatColor.RED)
                .create();
            event.getPlayer().spigot().sendMessage(message);
        } else if (astralBlock.isPresent() && event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack item = astralBlock.get().blockSpec.itemSpec.createItemStack();
            tileEntity.ifPresent(AstralTileEntity::onDestroy);
            event.setDropItems(false); // To prevent sync issues with ChunkStorage
            state.getWorld().dropItemNaturally(state.getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        AbstractAstralBlockSpec spec = plugin.getAstralBlock(item);
        if (spec instanceof AstralPlaceholderBlockSpec) {
            event.setCancelled(true);
            return;
        }
        if (spec != null) {
            //plugin.getLogger().info("Block place event for " + event.getBlock().getLocation().toString() + " with " + spec.getClass().getName());
            basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) spec, event.getBlock());
            basicBlockStateManager.getTileEntityFromBlock(event.getBlock()).ifPresent(tileEntity -> {
                tileEntity.onPlace(event.getPlayer());
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Optional<AstralTileEntity> spec = plugin.getTileEntity(block);
        spec.ifPresent(astralTileEntity -> astralTileEntity.onInteract(event));
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkLoad(ChunkLoadEvent event) {
        basicBlockStateManager.loadChunkTileEntities(event.getChunk());
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkUnload(ChunkUnloadEvent event) {
        basicBlockStateManager.unloadChunkTileEntities(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Listening for the event.
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Piston piston = (Piston) event.getBlock().getBlockData();
        BlockFace face = piston.getFacing();
        ArrayList<AstralBlock> astralBlocks = new ArrayList<>();
        for (Block block : event.getBlocks()) {
            Optional<AstralBlock> optAstralBlock = basicBlockStateManager.processBlockRemoval(block.getState());
            optAstralBlock.ifPresent(astralBlocks::add);
        }
        for (AstralBlock astralBlock : astralBlocks) {
            Block targetBlock = astralBlock.blockLocation.add(face.getDirection()).getBlock();
            if (astralBlock.blockSpec instanceof AstralBasicBlockSpec) {
                basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) astralBlock.blockSpec, targetBlock, astralBlock.data);
            } else if (astralBlock.blockSpec instanceof AstralPlaceholderBlockSpec) {
                basicBlockStateManager.processBlockPlacement((AstralPlaceholderBlockSpec) astralBlock.blockSpec, targetBlock, astralBlock.data);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Listening for the event.
    public void onPistonRetract(BlockPistonRetractEvent event) {
        TechnicalPiston piston = (TechnicalPiston) event.getBlock().getBlockData();
        BlockFace face = piston.getFacing().getOppositeFace();
        ArrayList<AstralBlock> astralBlocks = new ArrayList<>();
        for (Block block : event.getBlocks()) {
            Optional<AstralBlock> optAstralBlock = basicBlockStateManager.processBlockRemoval(block.getState());
            optAstralBlock.ifPresent(astralBlocks::add);
        }
        for (AstralBlock astralBlock : astralBlocks) {
            Block targetBlock = astralBlock.blockLocation.add(face.getDirection()).getBlock();
            if (astralBlock.blockSpec instanceof AstralBasicBlockSpec) {
                basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) astralBlock.blockSpec, targetBlock, astralBlock.data);
            } else if (astralBlock.blockSpec instanceof AstralPlaceholderBlockSpec) {
                basicBlockStateManager.processBlockPlacement((AstralPlaceholderBlockSpec) astralBlock.blockSpec, targetBlock, astralBlock.data);
            }
        }
    }
    
}
