package com.ninni.dye_depot.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ninni.dye_depot.polymer.DDPolymerBlockEntityNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BannerBlockEntity.class)
public class BannerBlockEntityMixin {
    @ModifyReturnValue(method = "getUpdateTag", at = @At("RETURN"))
    private CompoundTag dyeDepot$addPolymerBannerBase(CompoundTag original) {
        var blockEntity = (BannerBlockEntity) (Object) this;
        if (blockEntity.getBlockState().getBlock() instanceof AbstractBannerBlock banner) {
            return DDPolymerBlockEntityNbt.withBannerBase(original, banner.getColor());
        }
        return original;
    }
}
