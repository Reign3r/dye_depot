package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/** Client-only exact-color variants of authored banner and shield patterns. */
final class DDPolymerBannerPatterns {
    private static final String ASSET_SUFFIX = "_" + DyeDepot.MOD_ID + "_";

    private DDPolymerBannerPatterns() {
    }

    static BannerPatternLayers visualize(BannerPatternLayers patterns) {
        return new BannerPatternLayers(patterns.layers().stream()
                .map(DDPolymerBannerPatterns::visualize)
                .toList());
    }

    static BannerPatternLayers.Layer visualize(BannerPatternLayers.Layer layer) {
        DyeColor color = layer.color();
        if (!DDDyes.isModDye(color)) {
            return layer;
        }

        BannerPattern source = layer.pattern().value();
        BannerPattern exactVisual = new BannerPattern(
                visualAssetId(source.assetId(), color),
                source.translationKey()
        );
        return new BannerPatternLayers.Layer(Holder.direct(exactVisual), DyeColor.WHITE);
    }

    static Identifier visualAssetId(Identifier source, DyeColor color) {
        return source.withSuffix(ASSET_SUFFIX + color.getName());
    }
}
