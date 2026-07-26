package com.ninni.dye_depot.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepMixin {
    @Unique
    private static final Function<DyeColor, ResourceKey<LootTable>> DD$SHEARING_LOOT_TABLES = Util.memoize(color ->
            ResourceKey.create(Registries.LOOT_TABLE, DyeDepot.modLoc("shearing/sheep/" + color.getName()))
    );

    @Inject(method = "getRandomSheepColor", at = @At("RETURN"), cancellable = true)
    private static void DD$getRandomSheepColor(
            ServerLevelAccessor level,
            BlockPos pos,
            CallbackInfoReturnable<DyeColor> cir
    ) {
        int roll = level.getRandom().nextInt(100);
        if (roll < 23) {
            cir.setReturnValue(DDDyes.BEIGE.get());
        }
        if (level.getRandom().nextInt(500) == 0) {
            cir.setReturnValue(DDDyes.AQUA.get());
        }
    }

    @ModifyExpressionValue(
            method = "shear",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/storage/loot/BuiltInLootTables;SHEAR_SHEEP:Lnet/minecraft/resources/ResourceKey;"
            )
    )
    private ResourceKey<LootTable> DD$useCustomShearingLootTable(ResourceKey<LootTable> original) {
        DyeColor color = ((Sheep) (Object) this).getColor();
        return DDDyes.isModDye(color) ? DD$SHEARING_LOOT_TABLES.apply(color) : original;
    }

    @ModifyConstant(
            method = {
                    "getColor()Lnet/minecraft/world/item/DyeColor;",
                    "setColor",
                    "isSheared",
            },
            constant = @Constant(intValue = 15)
    )
    private int DD$useFiveColorBits(int constant) {
        return 31;
    }

    @ModifyConstant(method = "setColor", constant = @Constant(intValue = 240))
    private int DD$preserveUpperDataBits(int constant) {
        return 224;
    }

    @ModifyConstant(
            method = {
                    "isSheared",
                    "setSheared"
            },
            constant = @Constant(intValue = 16)
    )
    private int DD$moveShearedBit(int constant) {
        return 32;
    }

    @ModifyConstant(method = "setSheared", constant = @Constant(intValue = -17))
    private int DD$clearMovedShearedBit(int constant) {
        return -33;
    }
}
