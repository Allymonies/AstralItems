package io.astralforge.astralitems.block;

import io.astralforge.astralitems.AstralItemSpec;

abstract public class AbstractAstralBlockSpec {
    public final AstralItemSpec itemSpec;

    public AbstractAstralBlockSpec(AstralItemSpec itemSpec) {
        this.itemSpec = itemSpec;
    }
}
