package com.ninni.dye_depot.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ninni.dye_depot.registry.DyedHolders;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;

/**
 * Identifier-only helpers for optional integrations.
 *
 * <p>The 26.2 data source set deliberately does not link against Supplementaries:
 * its 26.2 artifacts are not available yet. Keeping optional entries as resource
 * keys also means datagen remains runnable when neither compatibility mod is
 * installed.</p>
 */
public final class ModCompat {

    public static final String SUPPLEMENTARIES = "supplementaries";
    public static final String SUPPLEMENTARIES_SQUARED = "suppsquared";

    private ModCompat() {
    }

    public static Stream<DyeColor> colors() {
        return DyedHolders.modColors();
    }

    public static Identifier id(String namespace, String name, DyeColor color) {
        return Identifier.fromNamespaceAndPath(namespace, name + "_" + color.getSerializedName());
    }

    public static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String namespace, String name, DyeColor color) {
        return ResourceKey.create(registry, id(namespace, name, color));
    }

    public static JsonArray supplementariesConditions(String modId, String flag) {
        var conditions = new JsonArray();

        var modLoaded = new JsonObject();
        modLoaded.addProperty("condition", "fabric:all_mods_loaded");
        var values = new JsonArray();
        values.add(modId);
        modLoaded.add("values", values);
        conditions.add(modLoaded);

        var featureFlag = new JsonObject();
        featureFlag.addProperty("condition", SUPPLEMENTARIES + ":flag");
        featureFlag.addProperty("flag", flag);
        conditions.add(featureFlag);
        return conditions;
    }

    public static void addSupplementariesConditions(JsonObject json, String modId, String flag) {
        json.add("fabric:load_conditions", supplementariesConditions(modId, flag));
    }
}
