package io.astralforge.astralitems.block;

import org.bukkit.block.Block;

public interface TickHandlerInterface {
    public void tick(Block block, AbstractAstralBlockSpec blockSpec);
}
