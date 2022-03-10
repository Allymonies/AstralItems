package io.astralforge.astralitems.block;

import java.util.Optional;

import org.bukkit.GameMode;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        Optional<AbstractAstralBlockSpec> spec = basicBlockStateManager.processBlockRemoval(state);
        if (spec.isPresent() && spec.get() instanceof AstralPlaceholderBlockSpec) {
            event.setCancelled(true);
            BaseComponent[] message = new ComponentBuilder(spec.get().itemSpec.id.toString())
                .color(ChatColor.GRAY)
                .append(" at ").color(ChatColor.RED)
                .append(event.getBlock().getX() + " " + event.getBlock().getY() + " " + event.getBlock().getZ()).color(ChatColor.GRAY)
                .append(" is not registered, please contact your server owner and/or plugin developer.")
                .color(ChatColor.RED)
                .create();
            event.getPlayer().spigot().sendMessage(message);
        } else if (spec.isPresent() && event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack item = spec.get().itemSpec.createItemStack();
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
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkLoad(ChunkLoadEvent event) {
        basicBlockStateManager.loadChunkTileEntities(event.getChunk());
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onChunkUnload(ChunkUnloadEvent event) {
        basicBlockStateManager.unloadChunkTileEntities(event.getChunk());
    }
    
}
