package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DDDyes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class DDPolymerBlockEntityNbt {
    private DDPolymerBlockEntityNbt() {
    }

    /** Rewrites only fields defined by the supplied vanilla block-entity type. */
    public static CompoundTag sanitize(BlockEntityType<?> type, CompoundTag original) {
        if (type == BlockEntityTypes.BANNER) {
            return sanitizePatterns(original);
        }
        if (type == BlockEntityTypes.SIGN || type == BlockEntityTypes.HANGING_SIGN) {
            return sanitizeSignText(original);
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

    private static CompoundTag sanitizeSignText(CompoundTag original) {
        CompoundTag changed = original;

        for (String textSection : new String[]{"front_text", "back_text"}) {
            Tag section = original.get(textSection);
            if (section instanceof CompoundTag text) {
                CompoundTag replacement = sanitizeColorField(text);
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

    private static CompoundTag sanitizePatterns(CompoundTag original) {
        Tag patternTag = original.get("patterns");
        if (patternTag instanceof ListTag patterns) {
            ListTag replacement = sanitizePatternList(patterns);
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

    private static ListTag sanitizePatternList(ListTag original) {
        ListTag changed = null;
        for (int index = 0; index < original.size(); index++) {
            Tag entry = original.get(index);
            if (entry instanceof CompoundTag pattern) {
                CompoundTag replacement = sanitizeColorField(pattern);
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
