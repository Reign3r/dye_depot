package com.ninni.dye_depot.polymer;

import com.mojang.serialization.DynamicOps;
import com.ninni.dye_depot.registry.DDDyes;
import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class DDPolymerBlockEntityNbt {
    private DDPolymerBlockEntityNbt() {
    }

    /** Rewrites only fields defined by the supplied vanilla block-entity type. */
    public static CompoundTag sanitize(BlockEntityType<?> type, CompoundTag original) {
        return sanitize(type, original, null);
    }

    /** Rewrites registry-backed text while keeping the server-owned tag untouched. */
    public static CompoundTag sanitize(
            BlockEntityType<?> type,
            CompoundTag original,
        HolderLookup.Provider registries
    ) {
        if (type == BlockEntityTypes.BANNER) {
            return sanitizePatterns(original, registries);
        }
        if (type == BlockEntityTypes.SIGN || type == BlockEntityTypes.HANGING_SIGN) {
            return sanitizeSignText(original, registries);
        }
        return original;
    }

    /** Adds a client-only full-cloth layer without changing the server block entity. */
    public static CompoundTag withBannerBase(CompoundTag original, DyeColor baseColor) {
        if (!DDDyes.isModDye(baseColor)) {
            return original;
        }

        ListTag patterns = new ListTag();
        Tag originalPatterns = original.get("patterns");
        if (originalPatterns instanceof ListTag list) {
            for (Tag entry : list) {
                if (!isPolymerBase(entry)) {
                    patterns.add(entry.copy());
                }
            }
        }

        CompoundTag base = new CompoundTag();
        base.putString("pattern", DDPolymerBannerBases.patternId(baseColor).toString());
        base.putString("color", DyeColor.WHITE.getName());
        patterns.addFirst(base);

        CompoundTag changed = original.copy();
        changed.put("patterns", patterns);
        return changed;
    }

    private static boolean isPolymerBase(Tag entry) {
        if (entry instanceof CompoundTag pattern && pattern.get("pattern") instanceof StringTag id) {
            return id.value().startsWith("dye_depot:polymer_base_");
        }
        return false;
    }

    private static CompoundTag sanitizeSignText(CompoundTag original, HolderLookup.Provider registries) {
        CompoundTag changed = original;
        DynamicOps<Tag> ops = registries == null
                ? NbtOps.INSTANCE
                : RegistryOps.create(NbtOps.INSTANCE, registries);

        for (String textSection : new String[]{"front_text", "back_text"}) {
            Tag section = original.get(textSection);
            if (section instanceof CompoundTag text) {
                CompoundTag replacement = sanitizeSignTextSection(text, ops);
                if (replacement != text) {
                    if (changed == original) {
                        changed = original.copy();
                    }
                    changed.put(textSection, replacement);
                }
            }
        }

        return changed;
    }

    private static CompoundTag sanitizeSignTextSection(CompoundTag original, DynamicOps<Tag> ops) {
        DyeColor customColor = customColor(original.get("color"));
        if (customColor == null) {
            return original;
        }

        int textColor = original.getBooleanOr("has_glowing_text", false)
                ? customColor.getTextColor()
                : ARGB.scaleRGB(customColor.getTextColor(), 0.4F);
        CompoundTag changed = original.copy();
        for (String messageField : new String[]{"messages", "filtered_messages"}) {
            Tag messages = original.get(messageField);
            if (messages != null) {
                Tag replacement = colorUnstyledMessages(messages, ops, textColor);
                if (replacement != messages) {
                    changed.put(messageField, replacement);
                }
            }
        }
        changed.putString("color", vanillaSignColor(customColor).getName());
        return changed;
    }

    private static Tag colorUnstyledMessages(Tag original, DynamicOps<Tag> ops, int textColor) {
        var decoded = ComponentSerialization.CODEC.listOf().parse(ops, original).result();
        if (decoded.isEmpty()) {
            return original;
        }

        boolean changed = false;
        var replacement = new ArrayList<Component>(decoded.get().size());
        for (Component message : decoded.get()) {
            if (message.getStyle().getColor() == null) {
                replacement.add(message.copy().withStyle(style -> style.withColor(textColor)));
                changed = true;
            } else {
                replacement.add(message);
            }
        }
        if (!changed) {
            return original;
        }

        return ComponentSerialization.CODEC.listOf()
                .encodeStart(ops, replacement)
                .result()
                .orElse(original);
    }

    private static DyeColor customColor(Tag tag) {
        if (tag instanceof StringTag string) {
            for (DDDyes dye : DDDyes.values()) {
                if (dye.getName().equals(string.value())) {
                    return dye.get();
                }
            }
        }
        return null;
    }

    private static DyeColor vanillaSignColor(DyeColor source) {
        DyeColor nearest = DyeColor.WHITE;
        int nearestDistance = Integer.MAX_VALUE;
        for (DyeColor candidate : DyeColor.values()) {
            // BLACK has special always-visible pale glow outlining. It is not a
            // faithful carrier for any of Dye Depot's non-black custom colors.
            if (candidate.getId() >= 16 || candidate == DyeColor.BLACK) {
                continue;
            }
            int distance = colorDistance(source.getTextColor(), candidate.getTextColor());
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int colorDistance(int left, int right) {
        int red = ((left >> 16) & 0xff) - ((right >> 16) & 0xff);
        int green = ((left >> 8) & 0xff) - ((right >> 8) & 0xff);
        int blue = (left & 0xff) - (right & 0xff);
        return red * red + green * green + blue * blue;
    }

    private static CompoundTag sanitizePatterns(
            CompoundTag original,
            HolderLookup.Provider registries
    ) {
        Tag patternTag = original.get("patterns");
        if (patternTag instanceof ListTag patterns) {
            ListTag replacement = sanitizePatternList(patterns, registries);
            if (replacement != patterns) {
                CompoundTag changed = original.copy();
                changed.put("patterns", replacement);
                return changed;
            }
        }
        return original;
    }

    private static CompoundTag sanitizeColorField(CompoundTag original) {
        Tag color = original.get("color");
        Tag replacement = sanitizeColor(color);
        if (replacement == color) {
            return original;
        }

        CompoundTag changed = original.copy();
        changed.put("color", replacement);
        return changed;
    }

    private static ListTag sanitizePatternList(
            ListTag original,
            HolderLookup.Provider registries
    ) {
        DynamicOps<Tag> ops = registries == null
                ? null
                : RegistryOps.create(NbtOps.INSTANCE, registries);
        ListTag changed = null;
        for (int index = 0; index < original.size(); index++) {
            Tag entry = original.get(index);
            if (entry instanceof CompoundTag pattern) {
                CompoundTag replacement = sanitizePattern(pattern, ops);
                if (replacement != pattern) {
                    if (changed == null) {
                        changed = original.copy();
                    }
                    changed.set(index, replacement);
                }
            }
        }
        return changed == null ? original : changed;
    }

    private static CompoundTag sanitizePattern(
            CompoundTag original,
            DynamicOps<Tag> ops
    ) {
        DyeColor customColor = customColor(original.get("color"));
        Tag patternTag = original.get("pattern");
        if (customColor != null && patternTag != null && ops != null) {
            var decoded = BannerPattern.CODEC.parse(ops, patternTag).result();
            if (decoded.isPresent()) {
                var exactLayer = DDPolymerBannerPatterns.visualize(
                        new BannerPatternLayers.Layer(decoded.get(), customColor)
                );
                var encoded = BannerPattern.CODEC.encodeStart(ops, exactLayer.pattern()).result();
                if (encoded.isPresent()) {
                    CompoundTag changed = original.copy();
                    changed.put("pattern", encoded.get());
                    changed.putString("color", exactLayer.color().getName());
                    return changed;
                }
            }
        }
        return sanitizeColorField(original);
    }

    private static Tag sanitizeColor(Tag tag) {
        if (tag instanceof StringTag string) {
            for (DDDyes dye : DDDyes.values()) {
                if (dye.getName().equals(string.value())) {
                    return StringTag.valueOf(DDPolymerColors.vanillaColor(dye.get()).getName());
                }
            }
        }
        return tag;
    }
}
