package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DDDyes;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
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
        sanitize(clientStack, clientStack, 0, null, null);
        return clientStack;
    }

    /** Adds the exact visual banner base when registry-backed patterns are available. */
    public static ItemStack sanitize(
            ItemStack originalStack,
            ItemStack clientStack,
            HolderLookup.Provider registries,
            PacketContext context
    ) {
        sanitize(originalStack, clientStack, 0, registries, context);
        return clientStack;
    }

    private static void sanitize(
            ItemStack original,
            ItemStack stack,
            int depth,
            HolderLookup.Provider registries,
            PacketContext context
    ) {
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
            patterns = new BannerPatternLayers(patterns.layers().stream()
                    .map(layer -> new BannerPatternLayers.Layer(layer.pattern(), DDPolymerColors.vanillaColor(layer.color())))
                    .toList());
            patterns = DDPolymerBannerBases.strip(patterns);
        }
        if (registries != null && original.getItem() instanceof BannerItem banner) {
            BannerPatternLayers authoredPatterns = patterns == null ? BannerPatternLayers.EMPTY : patterns;
            // The vanilla Loom counts every client-visible layer and locks at
            // six. Omit the synthetic base only from its actual five-pattern
            // input slot so the sixth authored choice remains available.
            // Inventory icons, the result slot, and placed banners stay exact.
            if (DDDyes.isModDye(banner.getColor())
                    && !isLoomInputAwaitingSixthPattern(original, authoredPatterns, context)) {
                patterns = DDPolymerBannerBases.prepend(
                        banner.getColor(),
                        authoredPatterns,
                        registries
                );
                preserveAuthoredPatternTooltip(original, stack, authoredPatterns);
            }
        }
        if (patterns != null) {
            stack.set(DataComponents.BANNER_PATTERNS, patterns);
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
            List<ItemStack> safeItems = contents.allItemsCopyStream().toList();
            ItemContainerContents originalContents = original.get(DataComponents.CONTAINER);
            List<ItemStack> originalItems = originalContents == null
                    ? safeItems
                    : originalContents.allItemsCopyStream().toList();
            for (int index = 0; index < safeItems.size(); index++) {
                ItemStack originalItem = index < originalItems.size() ? originalItems.get(index) : safeItems.get(index);
                sanitize(originalItem, safeItems.get(index), depth + 1, registries, context);
            }
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(safeItems));
        }
    }

    private static boolean isLoomInputAwaitingSixthPattern(
            ItemStack original,
            BannerPatternLayers authoredPatterns,
            PacketContext context
    ) {
        if (context == null || authoredPatterns.layers().size() != 5) {
            return false;
        }
        var player = PolymerCommonUtils.getPlayer(context);
        return player != null
                && player.containerMenu instanceof LoomMenu loom
                && loom.getBannerSlot().getItem() == original;
    }

    private static void preserveAuthoredPatternTooltip(
            ItemStack original,
            ItemStack client,
            BannerPatternLayers authoredPatterns
    ) {
        TooltipDisplay originalDisplay = original.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        client.set(
                DataComponents.TOOLTIP_DISPLAY,
                originalDisplay.withHidden(DataComponents.BANNER_PATTERNS, true)
        );
        if (!originalDisplay.shows(DataComponents.BANNER_PATTERNS)) {
            return;
        }

        ItemLore originalLore = original.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<Component> lines = new ArrayList<>(originalLore.lines().size() + 6);
        int availableLines = Math.min(6, ItemLore.MAX_LINES - originalLore.lines().size());
        authoredPatterns.layers().stream().limit(availableLines).forEach(layer -> lines.add(
                layer.description()
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(style -> style.withItalic(false))
        ));
        lines.addAll(originalLore.lines());
        client.set(DataComponents.LORE, new ItemLore(lines));
    }

}
