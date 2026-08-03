package com.ninni.dye_depot.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.feline.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Cat.class)
public interface CatDataAccessor {
    @Accessor("DATA_COLLAR_COLOR")
    static EntityDataAccessor<Integer> dyeDepot$getCollarData() {
        throw new AssertionError();
    }
}
