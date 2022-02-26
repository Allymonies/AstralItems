package io.astralforge.astralitems.block;

import org.bukkit.block.Block;

import io.astralforge.astralitems.AstralItemSpec;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
abstract public class AbstractAstralBlockSpec {
    @NonNull
    public final AstralItemSpec itemSpec;
    public final TickHandlerInterface tickHandler;

    public static interface TickHandlerInterface {
        public void tick(Block block, AbstractAstralBlockSpec blockSpec);
    }    
}
