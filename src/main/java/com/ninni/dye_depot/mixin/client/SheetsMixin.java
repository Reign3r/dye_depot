package com.ninni.dye_depot.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sheets.class)
public class SheetsMixin {
    @ModifyReturnValue(method = "colorToShulkerSprite", at = @At("RETURN"))
    private static Identifier DD$useCustomShulkerNamespace(Identifier original, DyeColor color) {
        return DDDyes.isModDye(color) ? DyeDepot.modLoc(original.getPath()) : original;
    }
}
