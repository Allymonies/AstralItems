package io.astralforge.astralitems.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.astralforge.astralitems.AstralItems;

public class BasicBlockEventListener implements Listener {

    private final AstralItems plugin;
    private final BasicBlockStateManager basicBlockStateManager;

    private List<BlockState> states = new ArrayList<>();

    public BasicBlockEventListener(AstralItems plugin, BasicBlockStateManager basicBlockStateManager) {
        this.plugin = plugin;
        this.basicBlockStateManager = basicBlockStateManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (BlockState state : states) {
                    state.getWorld().spawnParticle(Particle.HEART, state.getLocation().add(0.5, 0.5, 0.5), 2, 0.5, 0.5, 0.5);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getLogger().info("Block break event for " + event.getBlock().getLocation().toString());
        BlockState state = event.getBlock().getState();
        Optional<AstralBasicBlockSpec> spec = basicBlockStateManager.processBlockRemoval(state);
        if (spec.isPresent() && event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack item = spec.get().itemSpec.createItemStack();
            event.setDropItems(false); // To prevent sync issues with ChunkStorage
            state.getWorld().dropItemNaturally(state.getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Listening for the event.
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        AbstractAstralBlockSpec spec = plugin.getAstralBlock(item);
        if (spec != null) {
            plugin.getLogger().info("Block place event for " + event.getBlock().getLocation().toString() + " with " + spec.getClass().getName());
            basicBlockStateManager.processBlockPlacement((AstralBasicBlockSpec) spec, event.getBlock());
            states.add(event.getBlock().getState());
        }
    }
}
