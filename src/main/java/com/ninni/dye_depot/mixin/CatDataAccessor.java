package com.ninni.dye_depot.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Cat.class)
public interface CatDataAccessor {
    @Accessor("DATA_COLLAR_COLOR")
    static EntityDataAccessor<Integer> dyeDepot$getCollarData() {
        throw new AssertionError();
    }

    @Accessor("DATA_VARIANT_ID")
    static EntityDataAccessor<Holder<CatVariant>> dyeDepot$getVariantData() {
        throw new AssertionError();
    }
}
