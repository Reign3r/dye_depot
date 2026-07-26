package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bridges Minecraft's fixed 16-slot color collections to Dye Depot's extended
 * colors. These collections replaced the individual dye/banner/shulker lookup
 * methods used by the 1.21 implementation.
 */
@Mixin(ColorCollection.class)
public class ColorCollectionMixin {

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private void DD$pickCustomColor(DyeColor color, CallbackInfoReturnable<Object> cir) {
        if (!DDDyes.isModDye(color)) return;

        Object collection = this;
        Object value = null;

        if (collection == Blocks.BED) value = DDBlocks.BEDS.getOrThrow(color);
        else if (collection == Blocks.WOOL) value = DDBlocks.WOOL.getOrThrow(color);
        else if (collection == Blocks.STAINED_GLASS) value = DDBlocks.STAINED_GLASS.getOrThrow(color);
        else if (collection == Blocks.DYED_TERRACOTTA) value = DDBlocks.TERRACOTTA.getOrThrow(color);
        else if (collection == Blocks.STAINED_GLASS_PANE) value = DDBlocks.STAINED_GLASS_PANES.getOrThrow(color);
        else if (collection == Blocks.CARPET) value = DDBlocks.CARPETS.getOrThrow(color);
        else if (collection == Blocks.BANNER) value = DDBlocks.BANNERS.getOrThrow(color);
        else if (collection == Blocks.WALL_BANNER) value = DDBlocks.WALL_BANNERS.getOrThrow(color);
        else if (collection == Blocks.DYED_SHULKER_BOX) value = DDBlocks.SHULKER_BOXES.getOrThrow(color);
        else if (collection == Blocks.GLAZED_TERRACOTTA) value = DDBlocks.GLAZED_TERRACOTTA.getOrThrow(color);
        else if (collection == Blocks.CONCRETE) value = DDBlocks.CONCRETE.getOrThrow(color);
        else if (collection == Blocks.CONCRETE_POWDER) value = DDBlocks.CONCRETE_POWDER.getOrThrow(color);
        else if (collection == Blocks.DYED_CANDLE) value = DDBlocks.CANDLES.getOrThrow(color);
        else if (collection == Blocks.DYED_CANDLE_CAKE) value = DDBlocks.CANDLE_CAKES.getOrThrow(color);
        else if (collection == Items.WOOL) value = DDBlocks.WOOL.getOrThrow(color).asItem();
        else if (collection == Items.DYED_TERRACOTTA) value = DDBlocks.TERRACOTTA.getOrThrow(color).asItem();
        else if (collection == Items.CARPET) value = DDBlocks.CARPETS.getOrThrow(color).asItem();
        else if (collection == Items.STAINED_GLASS) value = DDBlocks.STAINED_GLASS.getOrThrow(color).asItem();
        else if (collection == Items.STAINED_GLASS_PANE) value = DDBlocks.STAINED_GLASS_PANES.getOrThrow(color).asItem();
        else if (collection == Items.DYED_SHULKER_BOX) value = DDItems.SHULKER_BOXES.getOrThrow(color);
        else if (collection == Items.GLAZED_TERRACOTTA) value = DDBlocks.GLAZED_TERRACOTTA.getOrThrow(color).asItem();
        else if (collection == Items.CONCRETE) value = DDBlocks.CONCRETE.getOrThrow(color).asItem();
        else if (collection == Items.CONCRETE_POWDER) value = DDBlocks.CONCRETE_POWDER.getOrThrow(color).asItem();
        else if (collection == Items.DYE) value = DDItems.DYES.getOrThrow(color);
        else if (collection == Items.BED) value = DDItems.BEDS.getOrThrow(color);
        else if (collection == Items.BANNER) value = DDItems.BANNERS.getOrThrow(color);
        else if (collection == Items.DYED_CANDLE) value = DDBlocks.CANDLES.getOrThrow(color).asItem();

        if (value != null) cir.setReturnValue(value);
    }
}
