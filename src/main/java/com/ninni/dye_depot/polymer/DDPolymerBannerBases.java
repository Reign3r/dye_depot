package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

final class DDPolymerBannerBases {
    private static final String PATTERN_PREFIX = "polymer_base_";

    private DDPolymerBannerBases() {
    }

    static Identifier patternId(DyeColor color) {
        return DyeDepot.modLoc(PATTERN_PREFIX + color.getName());
    }

    static BannerPatternLayers prepend(
            DyeColor baseColor,
            BannerPatternLayers patterns,
            HolderLookup.Provider registries
    ) {
        Holder<BannerPattern> base = registries.lookupOrThrow(Registries.BANNER_PATTERN)
                .getOrThrow(ResourceKey.create(Registries.BANNER_PATTERN, patternId(baseColor)));
        return prepend(new BannerPatternLayers.Layer(base, DyeColor.WHITE), patterns);
    }

    static BannerPatternLayers prepend(
            BannerPatternLayers.Layer base,
            BannerPatternLayers patterns
    ) {
        var layers = new ArrayList<BannerPatternLayers.Layer>(patterns.layers().size() + 1);
        layers.add(base);
        patterns.layers().stream()
                .filter(layer -> !isPolymerBase(layer))
                .forEach(layers::add);
        return new BannerPatternLayers(layers);
    }

    static Optional<BannerPatternLayers.Layer> find(BannerPatternLayers patterns) {
        return patterns.layers().stream()
                .filter(DDPolymerBannerBases::isPolymerBase)
                .findFirst();
    }

    static BannerPatternLayers strip(BannerPatternLayers patterns) {
        return new BannerPatternLayers(patterns.layers().stream()
                .filter(layer -> !isPolymerBase(layer))
                .toList());
    }

    static boolean isPolymerBase(BannerPatternLayers.Layer layer) {
        return layer.pattern().unwrapKey()
                .map(ResourceKey::identifier)
                .filter(id -> DyeDepot.MOD_ID.equals(id.getNamespace()))
                .map(Identifier::getPath)
                .filter(path -> path.startsWith(PATTERN_PREFIX))
                .isPresent();
    }
}
