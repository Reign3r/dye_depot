package com.ninni.dye_depot.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Sheep.class)
public interface SheepDataAccessor {
    @Accessor("DATA_WOOL_ID")
    static EntityDataAccessor<Byte> dyeDepot$getWoolData() {
        throw new AssertionError();
    }
}
