package com.ninni.dye_depot.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Wolf.class)
public interface WolfDataAccessor {
    @Accessor("DATA_COLLAR_COLOR")
    static EntityDataAccessor<Integer> dyeDepot$getCollarData() {
        throw new AssertionError();
    }

    @Accessor("DATA_VARIANT_ID")
    static EntityDataAccessor<Holder<WolfVariant>> dyeDepot$getVariantData() {
        throw new AssertionError();
    }
}
