package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DyedHolders;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

public final class DDPolymerColors {
    private static final Map<DyeColor, DyeColor> VANILLA_COLORS = new HashMap<>();

    private DDPolymerColors() {
    }

    public static DyeColor vanillaColor(DyeColor color) {
        if (color.getId() < 16) {
            return color;
        }
        return VANILLA_COLORS.computeIfAbsent(color, DDPolymerColors::findNearestVanillaColor);
    }

    private static DyeColor findNearestVanillaColor(DyeColor source) {
        int rgb = source.getTextureDiffuseColor();
        return DyedHolders.vanillaColors()
                .min((left, right) -> Integer.compare(distance(rgb, left.getTextureDiffuseColor()), distance(rgb, right.getTextureDiffuseColor())))
                .orElse(DyeColor.WHITE);
    }

    private static int distance(int left, int right) {
        int red = ((left >> 16) & 0xff) - ((right >> 16) & 0xff);
        int green = ((left >> 8) & 0xff) - ((right >> 8) & 0xff);
        int blue = (left & 0xff) - (right & 0xff);
        return red * red + green * green + blue * blue;
    }
}
