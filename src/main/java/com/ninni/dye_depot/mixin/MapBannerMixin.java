package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.registry.DDMapDecorationType;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapBanner.class)
public class MapBannerMixin {
    @Inject(method = "getDecoration", at = @At(value = "HEAD"), cancellable = true)
    private void DD$addCustomDecoration(CallbackInfoReturnable<Holder<MapDecorationType>> cir) {
        MapBanner self = (MapBanner) (Object) this;
        DDMapDecorationType.BANNERS.holder(self.color()).ifPresent(cir::setReturnValue);
    }
}
