package com.ninni.dye_depot.test;

import static com.ninni.dye_depot.test.ResourceTestSupport.bytes;
import static com.ninni.dye_depot.test.ResourceTestSupport.exists;
import static com.ninni.dye_depot.test.ResourceTestSupport.json;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackMetadataResourceTest {

    private static final int MINECRAFT_26_2_RESOURCE_FORMAT_MAJOR = 88;
    private static final int MINECRAFT_26_2_RESOURCE_FORMAT_MINOR = 0;

    @Test
    void rootAndBuiltinPackUseTheCurrent26Point2MetadataFormat() {
        assertCurrentPack("pack.mcmeta");
        assertCurrentPack("resourcepacks/dye_override/pack.mcmeta");
    }

    @Test
    void defaultBuiltinSkyAndAshOverridePayloadIsComplete() {
        assertTrue(exists("resourcepacks/dye_override/pack.png"));
        JsonObject overrides = json("resourcepacks/dye_override/assets/dye_depot/lang/en_us.json");
        assertTrue(overrides.entrySet().size() > 0, "built-in pack must provide Dye Depot name overrides");

        for (String dye : List.of(
                "yellow",
                "purple",
                "pink",
                "orange",
                "magenta",
                "lime",
                "light_gray",
                "gray",
                "cyan"
        )) {
            String path = "resourcepacks/dye_override/assets/minecraft/textures/item/" + dye + "_dye.png";
            assertTrue(bytes(path).length > 0, () -> path + " must be packaged");
        }
    }

    private static void assertCurrentPack(String path) {
        JsonObject pack = requiredObject(json(path), "pack", path);
        assertTrue(pack.has("description"), () -> path + " must have a description");
        assertTrue(pack.has("min_format"), () -> path + " must use the 26.2 min_format field");
        assertTrue(pack.has("max_format"), () -> path + " must use the 26.2 max_format field");
        assertFalse(pack.has("pack_format"), () -> path + " must not use the obsolete pack_format field");
        assertFalse(pack.has("supported_formats"), () -> path + " must not use the obsolete supported_formats field");

        assertEquals(
                List.of(MINECRAFT_26_2_RESOURCE_FORMAT_MAJOR, MINECRAFT_26_2_RESOURCE_FORMAT_MINOR),
                normalizedFormat(pack.get("min_format")),
                path + " min_format"
        );
        assertEquals(
                List.of(MINECRAFT_26_2_RESOURCE_FORMAT_MAJOR, MINECRAFT_26_2_RESOURCE_FORMAT_MINOR),
                normalizedFormat(pack.get("max_format")),
                path + " max_format"
        );
    }

    private static List<Integer> normalizedFormat(JsonElement encoded) {
        assertTrue(encoded != null, "missing pack format");
        if (encoded.isJsonPrimitive()) {
            return List.of(encoded.getAsInt(), 0);
        }
        assertTrue(encoded.isJsonArray(), "pack format must be an integer or [major, minor]");
        assertTrue(encoded.getAsJsonArray().size() >= 1, "pack format array cannot be empty");
        int major = encoded.getAsJsonArray().get(0).getAsInt();
        int minor = encoded.getAsJsonArray().size() == 1 ? 0 : encoded.getAsJsonArray().get(1).getAsInt();
        return List.of(major, minor);
    }
}
