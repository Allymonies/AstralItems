package io.astralforge.astralitems.block;

import io.astralforge.astralitems.AstralItems;
import org.bukkit.block.Block;

import io.astralforge.astralitems.AstralItemSpec;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@SuperBuilder
abstract public class AbstractAstralBlockSpec {
    @NonNull
    public final AstralItemSpec itemSpec;
    public final TickHandlerInterface tickHandler;

    public interface TickHandlerInterface {
        void tick(Block block, AbstractAstralBlockSpec blockSpec);
    }

    public void register() {
        Objects.requireNonNull(AstralItems.getInstance()).addBlock(this);
    }

}
