package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sheep no longer override their loot-table key in 26.2. Route only
 * unsheared custom-colored sheep to Dye Depot's wrapper tables; those tables
 * add the matching wool and delegate to vanilla's sheep table for mutton.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private static final Function<DyeColor, ResourceKey<LootTable>> DD$SHEEP_LOOT_TABLES = Util.memoize(color ->
            ResourceKey.create(Registries.LOOT_TABLE, DyeDepot.modLoc("entities/sheep/" + color.getName()))
    );

    @Inject(method = "getLootTable", at = @At("HEAD"), cancellable = true)
    private void DD$useCustomSheepLootTable(CallbackInfoReturnable<Optional<ResourceKey<LootTable>>> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Sheep sheep && !sheep.isSheared() && DDDyes.isModDye(sheep.getColor())) {
            cir.setReturnValue(Optional.of(DD$SHEEP_LOOT_TABLES.apply(sheep.getColor())));
        }
    }
}
