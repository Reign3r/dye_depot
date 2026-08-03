package com.ninni.dye_depot.polymer;

import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/** Makes every DyeColor-bearing item component safe for an unmodified client. */
public final class DDPolymerItemSanitizer {
    private static final int MAX_CONTAINER_DEPTH = 32;
    private static final List<DataComponentType<DyeColor>> COLOR_COMPONENTS = List.of(
            DataComponents.DYE,
            DataComponents.BASE_COLOR,
            DataComponents.WOLF_COLLAR,
            DataComponents.TROPICAL_FISH_BASE_COLOR,
            DataComponents.TROPICAL_FISH_PATTERN_COLOR,
            DataComponents.CAT_COLLAR,
            DataComponents.SHEEP_COLOR,
            DataComponents.SHULKER_COLOR
    );

    private DDPolymerItemSanitizer() {
    }

    /** Mutates only the client-side copy supplied by Polymer. */
    public static ItemStack sanitize(ItemStack clientStack) {
        sanitize(clientStack, 0);
        return clientStack;
    }

    private static void sanitize(ItemStack stack, int depth) {
        if (stack.isEmpty()) {
            return;
        }

        for (DataComponentType<DyeColor> component : COLOR_COMPONENTS) {
            DyeColor color = stack.get(component);
            if (color != null) {
                stack.set(component, DDPolymerColors.vanillaColor(color));
            }
        }

        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
        if (patterns != null) {
            stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(patterns.layers().stream()
                    .map(layer -> new BannerPatternLayers.Layer(layer.pattern(), DDPolymerColors.vanillaColor(layer.color())))
                    .toList()));
        }

        // CUSTOM_DATA, BUCKET_ENTITY_DATA, and ENTITY_DATA are opaque here.
        // Polymer stores exact recovery data in CUSTOM_DATA, and every
        // client-visible entity color is already covered by typed components.

        var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            var originalTag = blockEntityData.copyTagWithoutId();
            var safeTag = DDPolymerBlockEntityNbt.sanitize(blockEntityData.type(), originalTag);
            if (safeTag != originalTag) {
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntityData.type(), safeTag));
            }
        }

        if (depth >= MAX_CONTAINER_DEPTH) {
            return;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            List<ItemStack> safeItems = contents.allItemsCopyStream()
                    .peek(item -> sanitize(item, depth + 1))
                    .toList();
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(safeItems));
        }
    }

}
