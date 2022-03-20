package io.astralforge.astralitems.block;

import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataContainer;

public class AstralBlock {
    public final AbstractAstralBlockSpec blockSpec;
    public final PersistentDataContainer data;
    public final Location blockLocation;

    public AstralBlock(AbstractAstralBlockSpec abstractAstralBlockSpec, Location blockLocation, PersistentDataContainer data) {
        this.blockSpec = abstractAstralBlockSpec;
        this.blockLocation = blockLocation;
        this.data = data;
    }
}