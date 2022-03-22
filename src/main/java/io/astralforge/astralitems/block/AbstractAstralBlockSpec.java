package io.astralforge.astralitems.block;

import io.astralforge.astralitems.AstralItems;
import io.astralforge.astralitems.block.tile.AstralTileEntity;
import org.bukkit.block.Block;

import io.astralforge.astralitems.AstralItemSpec;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@SuperBuilder
abstract public class AbstractAstralBlockSpec {
    @NonNull
    public final AstralItemSpec itemSpec;
//    public final TickHandlerInterface tickHandler;

    public final AstralTileEntity.Builder tileEntityBuilder;

//    public interface TickHandlerInterface {
//        void tick(Block block, AbstractAstralBlockSpec blockSpec);
//    }

    public void register() {
        AstralItems.getInstance().addBlock(this);
    }

}
