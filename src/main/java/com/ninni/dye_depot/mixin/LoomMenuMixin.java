package com.ninni.dye_depot.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin {
    @Unique
    private static final int MAX_BANNER_PATTERN_LAYERS = 6;

    @Shadow
    public abstract Slot getBannerSlot();

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void dyeDepot$rejectPatternsPastVanillaLimit(
            Player player,
            int patternIndex,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BannerPatternLayers patterns = this.getBannerSlot().getItem()
                .getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        if (patterns.layers().size() >= MAX_BANNER_PATTERN_LAYERS) {
            cir.setReturnValue(false);
        }
    }
}
