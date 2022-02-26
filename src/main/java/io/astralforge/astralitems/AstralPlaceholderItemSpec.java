package io.astralforge.astralitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public class AstralPlaceholderItemSpec extends AstralItemSpec {

    public AstralPlaceholderItemSpec(NamespacedKey id) {
        super(id, Material.STICK, "Missing Item", false, false, null);
    }
    
}
