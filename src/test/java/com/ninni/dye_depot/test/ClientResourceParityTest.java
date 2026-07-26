package com.ninni.dye_depot.test;

import static com.ninni.dye_depot.test.ResourceTestSupport.ALL_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.CUSTOM_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.VANILLA_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.allStrings;
import static com.ninni.dye_depot.test.ResourceTestSupport.bytes;
import static com.ninni.dye_depot.test.ResourceTestSupport.exists;
import static com.ninni.dye_depot.test.ResourceTestSupport.filesUnder;
import static com.ninni.dye_depot.test.ResourceTestSupport.json;
import static com.ninni.dye_depot.test.ResourceTestSupport.jsonFilesUnder;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ClientResourceParityTest {

    private static final List<String> CUSTOM_BLOCK_SUFFIXES = List.of(
            "wool",
            "carpet",
            "terracotta",
            "concrete",
            "concrete_powder",
            "glazed_terracotta",
            "stained_glass",
            "stained_glass_pane",
            "shulker_box",
            "candle",
            "candle_cake",
            "banner",
            "wall_banner",
            "bed",
            "dye_basket"
    );

    private static final List<String> CUSTOM_ITEM_SUFFIXES = List.of(
            "dye",
            "wool",
            "carpet",
            "terracotta",
            "concrete",
            "concrete_powder",
            "glazed_terracotta",
            "stained_glass",
            "stained_glass_pane",
            "shulker_box",
            "candle",
            "banner",
            "bed",
            "dye_basket"
    );

    private static final List<String> BED_FACES = List.of(
            "head_up",
            "head_east",
            "head_west",
            "foot_up",
            "foot_south",
            "foot_east",
            "foot_west"
    );

    private static final List<String> SIMPLE_BLOCK_MODELS = List.of(
            "wool",
            "carpet",
            "terracotta",
            "concrete",
            "concrete_powder",
            "glazed_terracotta",
            "stained_glass",
            "dye_basket"
    );

    private static final List<String> PANE_MODELS = List.of(
            "stained_glass_pane_noside",
            "stained_glass_pane_noside_alt",
            "stained_glass_pane_post",
            "stained_glass_pane_side",
            "stained_glass_pane_side_alt"
    );

    private static final List<String> CANDLE_MODELS = List.of(
            "candle_one_candle",
            "candle_one_candle_lit",
            "candle_two_candles",
            "candle_two_candles_lit",
            "candle_three_candles",
            "candle_three_candles_lit",
            "candle_four_candles",
            "candle_four_candles_lit",
            "candle_cake",
            "candle_cake_lit"
    );

    @Test
    void generatedClientResourceCountsMatchTheRegistryManifest() {
        assertEquals(256, jsonFilesUnder("assets/dye_depot/blockstates").size(), "one blockstate per registered block");
        assertEquals(432, jsonFilesUnder("assets/dye_depot/models/block").size(), "26.2 block-model manifest");
        assertEquals(96, jsonFilesUnder("assets/dye_depot/models/item").size(), "64 used plus 32 preserved quirk models");
        assertEquals(240, jsonFilesUnder("assets/dye_depot/items").size(), "one 26.2 client item definition per item");

        assertEquals(64, jsonFilesUnder("assets/supplementaries/blockstates").size());
        assertEquals(448, jsonFilesUnder("assets/supplementaries/models/block").size());
        assertEquals(64, jsonFilesUnder("assets/supplementaries/models/item").size());
        assertEquals(64, jsonFilesUnder("assets/supplementaries/items").size());

        assertEquals(16, jsonFilesUnder("assets/suppsquared/blockstates").size());
        assertEquals(384, jsonFilesUnder("assets/suppsquared/models/block").size());
        assertEquals(16, jsonFilesUnder("assets/suppsquared/models/item").size());
        assertEquals(16, jsonFilesUnder("assets/suppsquared/items").size());
    }

    @Test
    void everyRegisteredBlockAndItemHasIts26Point2ClientDefinition() {
        for (String color : CUSTOM_COLORS) {
            for (String suffix : CUSTOM_BLOCK_SUFFIXES) {
                assertBlockState(color + "_" + suffix);
            }
            for (String suffix : CUSTOM_ITEM_SUFFIXES) {
                assertItemDefinition(color + "_" + suffix);
            }
        }

        for (String color : VANILLA_COLORS) {
            assertBlockState(color + "_dye_basket");
            assertItemDefinition(color + "_dye_basket");
        }
    }

    @Test
    void everyGeneratedBlockModelFamilyIsComplete() {
        for (String color : CUSTOM_COLORS) {
            for (String suffix : SIMPLE_BLOCK_MODELS) {
                assertModel("assets/dye_depot/models/block/" + color + "_" + suffix + ".json");
            }
            for (String suffix : PANE_MODELS) {
                assertModel("assets/dye_depot/models/block/" + color + "_" + suffix + ".json");
            }
            for (String suffix : CANDLE_MODELS) {
                assertModel("assets/dye_depot/models/block/" + color + "_" + suffix + ".json");
            }
            assertShulkerModelSet(color);
            assertModel("assets/dye_depot/models/block/" + color + "_bed_head.json");
            assertModel("assets/dye_depot/models/block/" + color + "_bed_foot.json");
        }

        for (String color : VANILLA_COLORS) {
            assertModel("assets/dye_depot/models/block/" + color + "_dye_basket.json");
        }
    }

    @Test
    void everyGlassTextureSlotUsesThe26Point2TranslucencySchema() {
        for (String color : CUSTOM_COLORS) {
            var glassModels = new java.util.ArrayList<String>();
            glassModels.add("stained_glass");
            glassModels.addAll(PANE_MODELS);

            for (String modelName : glassModels) {
                String path = "assets/dye_depot/models/block/" + color + "_" + modelName + ".json";
                JsonObject textures = requiredObject(json(path), "textures", path);
                assertFalse(textures.entrySet().isEmpty(), path + " must define glass textures");

                for (var entry : textures.entrySet()) {
                    assertTrue(
                            entry.getValue().isJsonObject(),
                            () -> path + " texture slot '" + entry.getKey()
                                    + "' must use an object so force_translucent is not lost"
                    );
                    JsonObject texture = entry.getValue().getAsJsonObject();
                    String textureId = ResourceTestSupport.requiredString(texture, "sprite", path);
                    assertTrue(
                            textureId.startsWith("dye_depot:block/" + color + "_stained_glass"),
                            () -> path + " slot '" + entry.getKey() + "' references unexpected texture " + textureId
                    );
                    assertTrue(
                            texture.has("force_translucent")
                                    && texture.get("force_translucent").isJsonPrimitive()
                                    && texture.get("force_translucent").getAsBoolean(),
                            () -> path + " texture slot '" + entry.getKey()
                                    + "' must set force_translucent to true"
                    );
                }
            }
        }
    }

    @Test
    void usedAndPreservedItemModelsAreExactlyTheExpectedFamilies() {
        for (String color : CUSTOM_COLORS) {
            for (String suffix : List.of("dye", "candle", "stained_glass_pane", "shulker_box")) {
                assertModel("assets/dye_depot/models/item/" + color + "_" + suffix + ".json");
            }
            assertModel("assets/dye_depot/models/item/" + color + "_wall_banner.json");
            assertModel("assets/dye_depot/models/item/flag_" + color + ".json");
        }
    }

    @Test
    void optionalCompatibilityClientManifestsCoverEveryCustomColor() {
        for (String color : CUSTOM_COLORS) {
            for (String family : List.of("candle_holder", "flag", "present", "trapped_present")) {
                assertCompatibilityBlockAndItem("supplementaries", family + "_" + color);
            }
            assertCompatibilityBlockAndItem("suppsquared", "gold_candle_holder_" + color);

            assertModel("assets/dye_depot/models/item/" + color + "_wall_banner.json");
            assertModel("assets/dye_depot/models/item/flag_" + color + ".json");
        }
    }

    @Test
    void everyBedModelUsesAllSevenConvertedFacesAndEveryFaceIsA16PixelTexture() throws IOException {
        for (String color : CUSTOM_COLORS) {
            String headPath = "assets/dye_depot/models/block/" + color + "_bed_head.json";
            String footPath = "assets/dye_depot/models/block/" + color + "_bed_foot.json";
            Set<String> modelReferences = new java.util.LinkedHashSet<>(allStrings(json(headPath)));
            modelReferences.addAll(allStrings(json(footPath)));

            for (String face : BED_FACES) {
                String textureId = "dye_depot:block/" + color + "_bed_" + face;
                assertTrue(modelReferences.contains(textureId), () -> color + " bed models do not reference " + textureId);

                String texturePath = "assets/dye_depot/textures/block/" + color + "_bed_" + face + ".png";
                BufferedImage image = image(texturePath);
                assertEquals(16, image.getWidth(), texturePath + " width");
                assertEquals(16, image.getHeight(), texturePath + " height");
                assertFalse(isFullyTransparent(image), texturePath + " must contain visible pixels");
            }
        }

        long convertedBedFaces = filesUnder("assets/dye_depot/textures/block").stream()
                .filter(path -> path.matches(".*/(" + String.join("|", CUSTOM_COLORS) + ")_bed_(?:head|foot)_.+\\.png"))
                .count();
        assertEquals(112, convertedBedFaces, "16 colors * seven 26.2 bed faces");
    }

    @Test
    void authoredPerColorTexturesCoverEveryRenderedFeature() throws IOException {
        List<String> blockTextures = List.of(
                "wool",
                "terracotta",
                "concrete",
                "concrete_powder",
                "glazed_terracotta",
                "stained_glass",
                "stained_glass_pane_top",
                "shulker_box",
                "candle",
                "candle_lit",
                "dye_basket_bottom",
                "dye_basket_front",
                "dye_basket_side",
                "dye_basket_top"
        );

        for (String color : CUSTOM_COLORS) {
            for (String suffix : blockTextures) {
                image("assets/dye_depot/textures/block/" + color + "_" + suffix + ".png");
            }
            image("assets/dye_depot/textures/item/" + color + "_dye.png");
            image("assets/dye_depot/textures/item/" + color + "_candle.png");
            image("assets/dye_depot/textures/entity/bed/" + color + ".png");
            image("assets/dye_depot/textures/entity/llama/decor/" + color + ".png");
            image("assets/dye_depot/textures/entity/equipment/llama_body/" + color + ".png");
            image("assets/dye_depot/textures/entity/shulker/shulker_" + color + ".png");

            BufferedImage mapMarker = image("assets/dye_depot/textures/map/decorations/" + color + "_banner.png");
            assertEquals(8, mapMarker.getWidth(), color + " map marker width");
            assertEquals(8, mapMarker.getHeight(), color + " map marker height");
        }

        for (String color : ALL_COLORS) {
            for (String face : List.of("bottom", "front", "side", "top")) {
                image("assets/dye_depot/textures/block/" + color + "_dye_basket_" + face + ".png");
            }
        }
    }

    @Test
    void authoredPngCountsPinTheNecessary26Point2AssetDelta() {
        assertEquals(513, pngCount("assets/dye_depot"));
        assertEquals(98, pngCount("assets/supplementaries"));
        assertEquals(16, pngCount("assets/suppsquared"));
    }

    @Test
    void everyCustomCarpetHasA26Point2LlamaEquipmentDefinition() {
        assertEquals(16, jsonFilesUnder("assets/minecraft/equipment").size());
        for (String color : CUSTOM_COLORS) {
            String path = "assets/minecraft/equipment/" + color + "_carpet.json";
            JsonObject layers = requiredObject(json(path), "layers", path);
            var llamaBody = ResourceTestSupport.requiredArray(layers, "llama_body", path);
            assertEquals(1, llamaBody.size(), path);
            JsonObject layer = llamaBody.get(0).getAsJsonObject();
            assertEquals(
                    "dye_depot:" + color,
                    ResourceTestSupport.requiredString(layer, "texture", path),
                    path
            );
        }
    }

    @Test
    void everyProjectClientJsonParsesAndSpecialAssetSchemasArePresent() {
        ResourceTestSupport.assertAllJsonParses("assets/dye_depot");
        ResourceTestSupport.assertAllJsonParses("assets/supplementaries");
        ResourceTestSupport.assertAllJsonParses("assets/suppsquared");

        JsonObject particle = json("assets/dye_depot/particles/dye_poof.json");
        assertEquals(8, ResourceTestSupport.requiredArray(particle, "textures", "dye poof").size());

        JsonObject atlas = json("assets/dye_depot/atlases/shulker_boxes.json");
        assertFalse(ResourceTestSupport.requiredArray(atlas, "sources", "shulker atlas").isEmpty());

        String soundsPath = "assets/dye_depot/sounds.json";
        JsonObject soundEvent = requiredObject(json(soundsPath), "block.dye_basket.poof", soundsPath);
        var sounds = ResourceTestSupport.requiredArray(soundEvent, "sounds", soundsPath);
        assertEquals(5, sounds.size());
        for (JsonElement sound : sounds) {
            String id = sound.getAsString();
            assertTrue(id.startsWith("dye_depot:"), () -> "foreign basket sound " + id);
            assertTrue(exists("assets/dye_depot/sounds/" + id.substring("dye_depot:".length()) + ".ogg"));
        }
        assertEquals(
                5,
                filesUnder("assets/dye_depot/sounds").stream().filter(path -> path.endsWith(".ogg")).count()
        );
    }

    @Test
    void generatedEnglishAndBuiltinOverrideLanguageManifestsRemainComplete() {
        String mainPath = "assets/dye_depot/lang/en_us.json";
        JsonObject main = json(mainPath);
        assertEquals(1_122, main.entrySet().size(), "generated English translation count");

        for (String color : CUSTOM_COLORS) {
            for (String suffix : List.of(
                    "banner",
                    "bed",
                    "candle",
                    "candle_cake",
                    "carpet",
                    "concrete",
                    "concrete_powder",
                    "dye_basket",
                    "glazed_terracotta",
                    "shulker_box",
                    "stained_glass",
                    "stained_glass_pane",
                    "terracotta",
                    "wool"
            )) {
                assertNonBlankTranslation(main, "block.dye_depot." + color + "_" + suffix, mainPath);
            }
            assertNonBlankTranslation(main, "item.dye_depot." + color + "_dye", mainPath);
        }

        String overridePath = "resourcepacks/dye_override/assets/dye_depot/lang/en_us.json";
        JsonObject overrides = json(overridePath);
        assertEquals(138, overrides.entrySet().size(), "Sky/Ash built-in language override count");
        overrides.entrySet().forEach(entry -> assertTrue(
                entry.getValue().isJsonPrimitive() && !entry.getValue().getAsString().isBlank(),
                () -> overridePath + " has a blank value for " + entry.getKey()
        ));
    }

    private static void assertBlockState(String id) {
        String path = "assets/dye_depot/blockstates/" + id + ".json";
        JsonObject root = json(path);
        assertTrue(root.has("variants") || root.has("multipart"), () -> path + " needs variants or multipart");
    }

    private static void assertItemDefinition(String id) {
        String path = "assets/dye_depot/items/" + id + ".json";
        JsonObject root = json(path);
        JsonObject model = requiredObject(root, "model", path);
        assertTrue(model.has("type"), () -> path + " model must declare its 26.2 item-model type");
    }

    private static void assertModel(String path) {
        JsonObject root = json(path);
        assertTrue(
                root.has("parent") || root.has("elements"),
                () -> path + " must inherit a parent or define model elements"
        );
        if (root.has("textures")) {
            assertTrue(root.get("textures").isJsonObject(), () -> path + " textures must be an object");
        }
    }

    private static void assertShulkerModelSet(String color) {
        String blockPath = "assets/dye_depot/models/block/" + color + "_shulker_box.json";
        JsonObject blockModel = json(blockPath);
        assertEquals(Set.of("textures"), blockModel.keySet(), blockPath);
        JsonObject textures = requiredObject(blockModel, "textures", blockPath);
        assertEquals(Set.of("particle"), textures.keySet(), blockPath);
        assertEquals(
                "dye_depot:block/" + color + "_shulker_box",
                ResourceTestSupport.requiredString(textures, "particle", blockPath),
                blockPath
        );

        String itemPath = "assets/dye_depot/items/" + color + "_shulker_box.json";
        JsonObject special = requiredObject(json(itemPath), "model", itemPath);
        assertEquals(Set.of("type", "base", "model", "transformation"), special.keySet(), itemPath);
        assertEquals("minecraft:special", ResourceTestSupport.requiredString(special, "type", itemPath), itemPath);
        assertEquals(
                "dye_depot:item/" + color + "_shulker_box",
                ResourceTestSupport.requiredString(special, "base", itemPath),
                itemPath
        );

        JsonObject renderer = requiredObject(special, "model", itemPath);
        assertEquals(Set.of("type", "texture"), renderer.keySet(), itemPath);
        assertEquals(
                "minecraft:shulker_box",
                ResourceTestSupport.requiredString(renderer, "type", itemPath),
                itemPath
        );
        assertEquals(
                "dye_depot:shulker_" + color,
                ResourceTestSupport.requiredString(renderer, "texture", itemPath),
                itemPath
        );

        JsonObject transformation = requiredObject(special, "transformation", itemPath);
        assertEquals(
                Set.of("left_rotation", "right_rotation", "scale", "translation"),
                transformation.keySet(),
                itemPath
        );
        assertEquals(4, ResourceTestSupport.requiredArray(transformation, "left_rotation", itemPath).size());
        assertEquals(4, ResourceTestSupport.requiredArray(transformation, "right_rotation", itemPath).size());
        assertEquals(3, ResourceTestSupport.requiredArray(transformation, "scale", itemPath).size());
        assertEquals(3, ResourceTestSupport.requiredArray(transformation, "translation", itemPath).size());
    }

    private static void assertCompatibilityBlockAndItem(String namespace, String id) {
        String blockStatePath = "assets/" + namespace + "/blockstates/" + id + ".json";
        JsonObject blockState = json(blockStatePath);
        assertTrue(blockState.has("variants") || blockState.has("multipart"), blockStatePath);

        assertModel("assets/" + namespace + "/models/item/" + id + ".json");

        String itemPath = "assets/" + namespace + "/items/" + id + ".json";
        JsonObject item = json(itemPath);
        assertTrue(requiredObject(item, "model", itemPath).has("type"), itemPath);
    }

    private static void assertNonBlankTranslation(JsonObject language, String key, String path) {
        assertTrue(language.has(key), () -> path + " is missing " + key);
        JsonElement value = language.get(key);
        assertTrue(value.isJsonPrimitive(), () -> path + " value for " + key + " must be text");
        assertFalse(value.getAsString().isBlank(), () -> path + " value for " + key + " is blank");
    }

    private static long pngCount(String prefix) {
        return filesUnder(prefix).stream().filter(path -> path.endsWith(".png")).count();
    }

    private static BufferedImage image(String path) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes(path)));
        assertNotNull(image, () -> path + " is not a readable PNG image");
        return image;
    }

    private static boolean isFullyTransparent(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
