package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DDDyes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    static boolean requiresPolymerConversion(ItemInstance stack) {
        return requiresPolymerConversion(stack, 0);
    }

    private static boolean requiresPolymerConversion(ItemInstance stack, int depth) {
        if (stack.count() <= 0) {
            return false;
        }
        for (DataComponentType<DyeColor> component : COLOR_COMPONENTS) {
            DyeColor color = stack.get(component);
            if (color != null && DDDyes.isModDye(color)) {
                return true;
            }
        }
        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
        if (patterns != null && patterns.layers().stream().anyMatch(layer -> DDDyes.isModDye(layer.color()))) {
            return true;
        }
        var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            var tag = blockEntityData.copyTagWithoutId();
            if (DDPolymerBlockEntityNbt.sanitize(blockEntityData.type(), tag) != tag) {
                return true;
            }
        }
        if (depth >= MAX_CONTAINER_DEPTH) {
            return false;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.allItemsCopyStream()
                .anyMatch(item -> requiresPolymerConversion(item, depth + 1));
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

        DyeColor exactShieldBase = original.getItem() == Items.SHIELD
                ? original.get(DataComponents.BASE_COLOR)
                : null;

        for (DataComponentType<DyeColor> component : COLOR_COMPONENTS) {
            DyeColor color = stack.get(component);
            if (color != null) {
                stack.set(component, DDPolymerColors.vanillaColor(color));
            }
        }

        BannerPatternLayers originalPatterns = original.get(DataComponents.BANNER_PATTERNS);
        BannerPatternLayers.Layer existingVisualBase = originalPatterns == null
                ? null
                : DDPolymerBannerBases.find(originalPatterns).orElse(null);
        BannerPatternLayers authoredPatterns = originalPatterns == null
                ? BannerPatternLayers.EMPTY
                : DDPolymerBannerBases.strip(originalPatterns);
        boolean hasCustomAuthoredPattern = authoredPatterns.layers().stream()
                .anyMatch(layer -> DDDyes.isModDye(layer.color()));
        boolean addedSyntheticBase = false;
        boolean alreadyVisualized = existingVisualBase != null
                && existingVisualBase.color() == DyeColor.WHITE
                && exactShieldBase == null
                && (!(original.getItem() instanceof BannerItem banner) || !DDDyes.isModDye(banner.getColor()));
        BannerPatternLayers patterns = originalPatterns == null
                ? null
                : DDPolymerBannerPatterns.visualize(authoredPatterns);
        if (alreadyVisualized) {
            patterns = DDPolymerBannerBases.prepend(
                    existingVisualBase,
                    DDPolymerBannerPatterns.visualize(authoredPatterns)
            );
        } else if (registries != null && original.getItem() instanceof BannerItem banner) {
            // The vanilla Loom counts every client-visible layer and locks at
            // six. Omit the synthetic base only from its actual five-pattern
            // input slot so the sixth authored choice remains available.
            // Inventory icons, the result slot, and placed banners stay exact.
            if (DDDyes.isModDye(banner.getColor())
                    && !isLoomInputAwaitingSixthPattern(original, authoredPatterns, context)) {
                patterns = DDPolymerBannerBases.prepend(
                        banner.getColor(),
                        DDPolymerBannerPatterns.visualize(authoredPatterns),
                        registries
                );
                addedSyntheticBase = true;
            } else if (originalPatterns != null) {
                patterns = DDPolymerBannerPatterns.visualize(authoredPatterns);
            }
        }
        if (registries != null && exactShieldBase != null && DDDyes.isModDye(exactShieldBase)) {
            patterns = DDPolymerBannerBases.prepend(
                    exactShieldBase,
                    DDPolymerBannerPatterns.visualize(authoredPatterns),
                    registries
            );
            addedSyntheticBase = true;
            // The synthetic first pattern is an opaque exact-color shield face,
            // so the unsafe custom BASE_COLOR is neither needed nor sent.
            stack.remove(DataComponents.BASE_COLOR);
            if (Objects.equals(
                    original.get(DataComponents.ITEM_NAME),
                    Items.SHIELD.components().get(DataComponents.ITEM_NAME)
            )) {
                stack.set(
                        DataComponents.ITEM_NAME,
                        Component.translatable("item.minecraft.shield." + exactShieldBase.getName())
                );
            }
        }
        if (!alreadyVisualized && (hasCustomAuthoredPattern || addedSyntheticBase)) {
            preserveAuthoredPatternTooltip(original, stack, authoredPatterns);
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
            var safeTag = DDPolymerBlockEntityNbt.sanitize(blockEntityData.type(), originalTag, registries);
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
        // Only AbstractContainerMenu's actual Loom banner-slot packet copy can
        // carry this transient marker. Do not require player.containerMenu to
        // be assigned yet: ServerPlayer sends a newly opened menu's initial
        // contents before replacing the previously active menu.
        return DDPolymerLoomSlotMarker.isMarked(original);
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
