package com.ninni.dye_depot.test;

import static com.ninni.dye_depot.test.ResourceTestSupport.CUSTOM_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.VANILLA_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.allStrings;
import static com.ninni.dye_depot.test.ResourceTestSupport.json;
import static com.ninni.dye_depot.test.ResourceTestSupport.jsonFilesUnder;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredArray;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredObject;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredString;
import static com.ninni.dye_depot.test.ResourceTestSupport.tagValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DataResourceParityTest {

    private static final Map<String, Integer> RECIPE_COUNTS = Map.of(
            "dye_depot", 381,
            "minecraft", 74,
            "supplementaries", 32,
            "suppsquared", 16
    );

    private static final Map<String, Integer> RECIPE_ADVANCEMENT_COUNTS = Map.of(
            "dye_depot", 381,
            "minecraft", 58,
            "supplementaries", 32,
            "suppsquared", 16
    );

    private static final List<String> CORE_RECIPE_SUFFIXES = List.of(
            "banner",
            "bed",
            "candle",
            "carpet",
            "concrete_powder",
            "dye_basket",
            "dye_from_basket",
            "glazed_terracotta",
            "stained_glass",
            "stained_glass_pane",
            "stained_glass_pane_from_glass_pane",
            "terracotta"
    );

    private static final List<String> DYE_RECIPE_PREFIXES = List.of(
            "dye_%s_bed",
            "dye_%s_carpet",
            "dye_%s_wool"
    );

    private static final List<String> BLOCK_LOOT_SUFFIXES = List.of(
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
    );

    private static final List<String> DISABLED_VANILLA_RECIPES = List.of(
            "gray_dye",
            "lime_dye",
            "light_blue_dye_from_blue_white_dye",
            "light_gray_dye_from_oxeye_daisy",
            "magenta_dye_from_blue_red_pink",
            "magenta_dye_from_blue_red_white_dye",
            "magenta_dye_from_purple_and_pink",
            "pink_dye_from_red_white_dye",
            "purple_dye",
            "red_dye_from_rose_bush"
    );

    @Test
    void recipeAndAdvancementCountsRemainAtTheSemanticBaseline() {
        int recipes = 0;
        int advancements = 0;
        for (var entry : RECIPE_COUNTS.entrySet()) {
            String namespace = entry.getKey();
            int expected = entry.getValue();
            int namespaceRecipes = jsonFilesUnder("data/" + namespace + "/recipe").size();
            int namespaceAdvancements = jsonFilesUnder("data/" + namespace + "/advancement/recipes").size();
            assertEquals(expected, namespaceRecipes, namespace + " recipes");
            assertEquals(
                    RECIPE_ADVANCEMENT_COUNTS.get(namespace),
                    namespaceAdvancements,
                    namespace + " recipe advancements"
            );
            recipes += namespaceRecipes;
            advancements += namespaceAdvancements;
        }
        assertEquals(503, recipes);
        assertEquals(487, advancements);
    }

    @Test
    void everyRecipeAndRecipeAdvancementParsesAndDeclaresItsCoreSchema() {
        for (String namespace : RECIPE_COUNTS.keySet()) {
            for (String path : jsonFilesUnder("data/" + namespace + "/recipe")) {
                JsonObject recipe = json(path);
                requiredString(recipe, "type", path);
            }

            for (String path : jsonFilesUnder("data/" + namespace + "/advancement/recipes")) {
                JsonObject advancement = json(path);
                requiredString(advancement, "parent", path);
                requiredObject(advancement, "criteria", path);
                assertFalse(requiredArray(advancement, "requirements", path).isEmpty(), path);
                JsonArray rewards = requiredArray(requiredObject(advancement, "rewards", path), "recipes", path);
                assertEquals(1, rewards.size(), path + " must unlock exactly one recipe");

                String relative = path.substring(("data/" + namespace + "/advancement/recipes/").length());
                int categoryEnd = relative.indexOf('/');
                String recipePath = relative.substring(categoryEnd + 1, relative.length() - ".json".length());
                assertEquals(namespace + ":" + recipePath, rewards.get(0).getAsString(), path);
            }
        }
    }

    @Test
    void everyCustomColorHasTheStandardRecipeFamilies() {
        for (String color : CUSTOM_COLORS) {
            for (String suffix : CORE_RECIPE_SUFFIXES) {
                assertRecipeResult("dye_depot", color + "_" + suffix, expectedCoreResult(color, suffix));
            }
            for (String template : DYE_RECIPE_PREFIXES) {
                assertRecipeResult("dye_depot", template.formatted(color), "dye_depot:" + color + "_" + template.substring(7));
            }
        }

        for (String color : VANILLA_COLORS) {
            assertRecipeResult("dye_depot", color + "_dye_basket", "dye_depot:" + color + "_dye_basket");
            assertRecipeResult("dye_depot", color + "_dye_from_basket", "minecraft:" + color + "_dye");
        }
    }

    @Test
    void customBannerCopyingUsesTheRequiredPerBanner26Point2SpecialRecipes() {
        for (String color : CUSTOM_COLORS) {
            String path = "data/minecraft/recipe/" + color + "_banner_duplicate.json";
            JsonObject recipe = json(path);
            assertEquals("minecraft:crafting_special_bannerduplicate", requiredString(recipe, "type", path), path);
            assertEquals("dye_depot:" + color + "_banner", requiredString(recipe, "banner", path), path);
            JsonObject result = requiredObject(recipe, "result", path);
            assertEquals("dye_depot:" + color + "_banner", requiredString(result, "id", path), path);
        }
    }

    @Test
    void selectedVanillaDyeRecipesRemainExplicitlyDisabled() {
        for (String id : DISABLED_VANILLA_RECIPES) {
            String path = "data/minecraft/recipe/" + id + ".json";
            JsonObject recipe = json(path);
            assertTrue(recipe.has("fabric:load_conditions"), () -> path + " must carry an always-false load condition");
            String encoded = recipe.get("fabric:load_conditions").toString();
            assertTrue(
                    encoded.contains("false") || (encoded.contains("\"fabric:not\"") && encoded.contains("\"fabric:true\"")),
                    () -> path + " does not encode an always-false condition"
            );
        }
    }

    @Test
    void optionalRecipesAndLootKeepBothModAndFeatureConditions() {
        for (String color : CUSTOM_COLORS) {
            assertCompatibilityConditions(
                    "data/supplementaries/recipe/candle_holder_" + color + ".json",
                    "supplementaries",
                    "candle_holder"
            );
            assertCompatibilityConditions(
                    "data/supplementaries/recipe/flag_" + color + ".json",
                    "supplementaries",
                    "flag"
            );
            assertCompatibilityConditions(
                    "data/suppsquared/recipe/gold_candle_holder_" + color + ".json",
                    "suppsquared",
                    "candle_holder"
            );
            assertCompatibilityConditions(
                    "data/supplementaries/loot_table/blocks/candle_holder_" + color + ".json",
                    "supplementaries",
                    "candle_holder"
            );
            assertCompatibilityConditions(
                    "data/supplementaries/loot_table/blocks/flag_" + color + ".json",
                    "supplementaries",
                    "flag"
            );
            assertCompatibilityConditions(
                    "data/suppsquared/loot_table/blocks/gold_candle_holder_" + color + ".json",
                    "suppsquared",
                    "candle_holder"
            );
        }
    }

    @Test
    void blockEntityAndCompatibilityLootTablesStayComplete() {
        assertEquals(240, jsonFilesUnder("data/dye_depot/loot_table/blocks").size());
        assertEquals(32, jsonFilesUnder("data/supplementaries/loot_table/blocks").size());
        assertEquals(16, jsonFilesUnder("data/suppsquared/loot_table/blocks").size());

        for (String color : CUSTOM_COLORS) {
            for (String suffix : BLOCK_LOOT_SUFFIXES) {
                assertBlockLoot("dye_depot", color + "_" + suffix);
            }
            assertBlockLoot("supplementaries", "candle_holder_" + color);
            assertBlockLoot("supplementaries", "flag_" + color);
            assertBlockLoot("suppsquared", "gold_candle_holder_" + color);
        }
        for (String color : VANILLA_COLORS) {
            assertBlockLoot("dye_depot", color + "_dye_basket");
        }
    }

    @Test
    void customSheepDeathAndShearingTablesPreserveWoolAndVanillaDrops() {
        assertEquals(16, jsonFilesUnder("data/dye_depot/loot_table/entities/sheep").size());
        assertEquals(16, jsonFilesUnder("data/dye_depot/loot_table/shearing/sheep").size());

        for (String color : CUSTOM_COLORS) {
            String wool = "dye_depot:" + color + "_wool";

            String deathPath = "data/dye_depot/loot_table/entities/sheep/" + color + ".json";
            JsonObject death = json(deathPath);
            assertEquals("minecraft:entity", requiredString(death, "type", deathPath));
            assertEquals(2, requiredArray(death, "pools", deathPath).size(), deathPath);
            Set<String> deathReferences = allStrings(death);
            assertTrue(deathReferences.contains(wool), () -> deathPath + " must drop " + wool);
            assertTrue(
                    deathReferences.contains("minecraft:entities/sheep"),
                    () -> deathPath + " must nest the vanilla sheep table for mutton"
            );

            String shearingPath = "data/dye_depot/loot_table/shearing/sheep/" + color + ".json";
            JsonObject shearing = json(shearingPath);
            assertEquals("minecraft:shearing", requiredString(shearing, "type", shearingPath));
            JsonArray pools = requiredArray(shearing, "pools", shearingPath);
            assertEquals(1, pools.size(), shearingPath);
            JsonObject rolls = requiredObject(pools.get(0).getAsJsonObject(), "rolls", shearingPath);
            assertEquals(1.0F, rolls.get("min").getAsFloat(), shearingPath);
            assertEquals(3.0F, rolls.get("max").getAsFloat(), shearingPath);
            assertTrue(allStrings(shearing).contains(wool), () -> shearingPath + " must drop " + wool);
        }
    }

    @Test
    void colorAndBehaviorTagsContainEveryRequiredRegistryEntry() {
        for (String color : CUSTOM_COLORS) {
            Set<String> blockColorTag = tagValues("data/c/tags/block/dyed/" + color + ".json");
            for (String suffix : List.of(
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
            )) {
                assertTrue(
                        blockColorTag.contains("dye_depot:" + color + "_" + suffix),
                        () -> "c:dyed/" + color + " block tag is missing " + suffix
                );
            }
            for (String optional : List.of(
                    "supplementaries:candle_holder_" + color,
                    "suppsquared:gold_candle_holder_" + color,
                    "supplementaries:flag_" + color,
                    "supplementaries:present_" + color,
                    "supplementaries:trapped_present_" + color
            )) {
                assertTrue(blockColorTag.contains(optional), () -> "c:dyed/" + color + " is missing " + optional);
            }

            Set<String> itemColorTag = tagValues("data/c/tags/item/dyed/" + color + ".json");
            for (String suffix : List.of(
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
            )) {
                assertTrue(
                        itemColorTag.contains("dye_depot:" + color + "_" + suffix),
                        () -> "c:dyed/" + color + " item tag is missing " + suffix
                );
            }
            for (String optional : List.of(
                    "supplementaries:candle_holder_" + color,
                    "suppsquared:gold_candle_holder_" + color,
                    "supplementaries:flag_" + color,
                    "supplementaries:present_" + color,
                    "supplementaries:trapped_present_" + color
            )) {
                assertTrue(itemColorTag.contains(optional), () -> "c:dyed/" + color + " is missing " + optional);
            }
        }

        Set<String> dyes = tagValues("data/c/tags/item/dyes.json");
        for (String color : CUSTOM_COLORS) {
            assertTrue(dyes.contains("dye_depot:" + color + "_dye"), () -> "c:dyes is missing " + color);
        }
        for (String behaviorTag : List.of("dyes", "loom_dyes", "cat_collar_dyes", "wolf_collar_dyes")) {
            Set<String> values = tagValues("data/minecraft/tags/item/" + behaviorTag + ".json");
            for (String color : CUSTOM_COLORS) {
                assertTrue(
                        values.contains("dye_depot:" + color + "_dye"),
                        () -> "minecraft:" + behaviorTag + " is missing " + color
                );
            }
        }

        assertFamilyTag("data/minecraft/tags/block/banners.json", "banner", true);
        assertFamilyTag("data/minecraft/tags/block/beds.json", "bed", false);
        assertFamilyTag("data/minecraft/tags/block/candles.json", "candle", false);
        assertFamilyTag("data/minecraft/tags/block/terracotta.json", "terracotta", false);
        assertFamilyTag("data/minecraft/tags/block/wool.json", "wool", false);
        assertFamilyTag("data/minecraft/tags/block/wool_carpets.json", "carpet", false);
        assertFamilyTag("data/minecraft/tags/item/banners.json", "banner", false);
        assertFamilyTag("data/minecraft/tags/item/beds.json", "bed", false);
        assertFamilyTag("data/minecraft/tags/item/candles.json", "candle", false);
        assertFamilyTag("data/minecraft/tags/item/terracotta.json", "terracotta", false);
        assertFamilyTag("data/minecraft/tags/item/wool.json", "wool", false);
        assertFamilyTag("data/minecraft/tags/item/wool_carpets.json", "carpet", false);

        Set<String> hoeMineable = tagValues("data/minecraft/tags/block/mineable/hoe.json");
        Set<String> nonCleanableBlocks = tagValues("data/supplementaries/tags/block/non_cleanable.json");
        Set<String> nonCleanableItems = tagValues("data/supplementaries/tags/item/non_cleanable.json");
        for (String color : ResourceTestSupport.ALL_COLORS) {
            String basket = "dye_depot:" + color + "_dye_basket";
            assertTrue(hoeMineable.contains(basket), () -> basket + " must be hoe-mineable");
            assertTrue(nonCleanableBlocks.contains(basket), () -> basket + " must not be soap-cleanable");
            assertTrue(nonCleanableItems.contains(basket), () -> basket + " item must not be soap-cleanable");
        }

        Set<String> piglinLoved = tagValues("data/minecraft/tags/item/piglin_loved.json");
        Set<String> piglinGuarded = tagValues("data/minecraft/tags/block/guarded_by_piglins.json");
        for (String color : CUSTOM_COLORS) {
            String goldHolder = "suppsquared:gold_candle_holder_" + color;
            assertTrue(piglinLoved.contains(goldHolder), () -> goldHolder + " must be piglin-loved");
            assertTrue(piglinGuarded.contains(goldHolder), () -> goldHolder + " must be piglin-guarded");
        }
    }

    @Test
    void specializedMiningCommonAndCompatibilityTagsCoverAllCustomFamilies() {
        Set<String> concreteBlocks = tagValues("data/c/tags/block/concretes.json");
        Set<String> glassBlocks = tagValues("data/c/tags/block/glass_blocks.json");
        Set<String> glassPanes = tagValues("data/c/tags/block/glass_panes.json");
        Set<String> pickaxe = tagValues("data/minecraft/tags/block/mineable/pickaxe.json");
        Set<String> shovel = tagValues("data/minecraft/tags/block/mineable/shovel.json");
        Set<String> impermeable = tagValues("data/minecraft/tags/block/impermeable.json");
        Set<String> shulkerBlocks = tagValues("data/minecraft/tags/block/shulker_boxes.json");

        Set<String> concreteItems = tagValues("data/c/tags/item/concretes.json");
        Set<String> concretePowderItems = tagValues("data/c/tags/item/concrete_powders.json");
        Set<String> glassBlockItems = tagValues("data/c/tags/item/glass_blocks.json");
        Set<String> glassPaneItems = tagValues("data/c/tags/item/glass_panes.json");
        Set<String> shulkerItems = tagValues("data/c/tags/item/shulker_boxes.json");

        for (String color : CUSTOM_COLORS) {
            String prefix = "dye_depot:" + color + "_";
            assertTrue(concreteBlocks.contains(prefix + "concrete"));
            assertTrue(glassBlocks.contains(prefix + "stained_glass"));
            assertTrue(glassPanes.contains(prefix + "stained_glass_pane"));
            assertTrue(pickaxe.contains(prefix + "terracotta"));
            assertTrue(pickaxe.contains(prefix + "glazed_terracotta"));
            assertTrue(pickaxe.contains(prefix + "concrete"));
            assertTrue(shovel.contains(prefix + "concrete_powder"));
            assertTrue(impermeable.contains(prefix + "stained_glass"));
            assertTrue(impermeable.contains(prefix + "stained_glass_pane"));
            assertTrue(shulkerBlocks.contains(prefix + "shulker_box"));

            assertTrue(concreteItems.contains(prefix + "concrete"));
            assertTrue(concretePowderItems.contains(prefix + "concrete_powder"));
            assertTrue(glassBlockItems.contains(prefix + "stained_glass"));
            assertTrue(glassPaneItems.contains(prefix + "stained_glass_pane"));
            assertTrue(shulkerItems.contains(prefix + "shulker_box"));
        }

        assertCompatibilityTag("candle_holders", "candle_holder", "supplementaries");
        assertCompatibilityTag("candle_holders", "gold_candle_holder", "suppsquared");
        assertCompatibilityTag("flags", "flag", "supplementaries");
        assertCompatibilityTag("presents", "present", "supplementaries");
        assertCompatibilityTag("trapped_presents", "trapped_present", "supplementaries");
    }

    @Test
    void bedPoiTagsBridgeCustomAndVanillaHomes() {
        assertEquals(
                Set.of("dye_depot:home", "minecraft:home"),
                tagValues("data/dye_depot/tags/point_of_interest_type/beds.json")
        );
        assertTrue(
                tagValues("data/minecraft/tags/point_of_interest_type/village.json").contains("#dye_depot:beds")
        );
    }

    @Test
    void allGeneratedModAndCompatibilityDataJsonParses() {
        for (String prefix : List.of(
                "data/dye_depot",
                "data/c/tags",
                "data/minecraft/tags",
                "data/minecraft/recipe",
                "data/minecraft/advancement/recipes",
                "data/supplementaries",
                "data/suppsquared"
        )) {
            ResourceTestSupport.assertAllJsonParses(prefix);
        }
    }

    private static String expectedCoreResult(String color, String suffix) {
        if ("dye_from_basket".equals(suffix)) {
            return "dye_depot:" + color + "_dye";
        }
        if ("stained_glass_pane_from_glass_pane".equals(suffix)) {
            return "dye_depot:" + color + "_stained_glass_pane";
        }
        return "dye_depot:" + color + "_" + suffix;
    }

    private static void assertRecipeResult(String namespace, String id, String expectedResult) {
        String path = "data/" + namespace + "/recipe/" + id + ".json";
        JsonObject recipe = json(path);
        requiredString(recipe, "type", path);
        JsonObject result = requiredObject(recipe, "result", path);
        assertEquals(expectedResult, requiredString(result, "id", path), path);
    }

    private static void assertBlockLoot(String namespace, String id) {
        String path = "data/" + namespace + "/loot_table/blocks/" + id + ".json";
        JsonObject loot = json(path);
        assertEquals("minecraft:block", requiredString(loot, "type", path), path);
        assertFalse(requiredArray(loot, "pools", path).isEmpty(), path);
    }

    private static void assertCompatibilityConditions(String path, String requiredMod, String feature) {
        JsonObject resource = json(path);
        JsonArray conditions = requiredArray(resource, "fabric:load_conditions", path);
        assertEquals(2, conditions.size(), path + " must guard both mod and feature availability");

        JsonObject modLoaded = conditions.get(0).getAsJsonObject();
        assertEquals("fabric:all_mods_loaded", requiredString(modLoaded, "condition", path));
        JsonArray modValues = requiredArray(modLoaded, "values", path);
        assertEquals(1, modValues.size(), path);
        assertEquals(requiredMod, modValues.get(0).getAsString(), path);

        JsonObject featureFlag = conditions.get(1).getAsJsonObject();
        assertEquals("supplementaries:flag", requiredString(featureFlag, "condition", path));
        assertEquals(feature, requiredString(featureFlag, "flag", path));
    }

    private static void assertCompatibilityTag(String tag, String family, String namespace) {
        Set<String> blocks = tagValues("data/supplementaries/tags/block/" + tag + ".json");
        Set<String> items = tagValues("data/supplementaries/tags/item/" + tag + ".json");
        for (String color : CUSTOM_COLORS) {
            String id = namespace + ":" + family + "_" + color;
            assertTrue(blocks.contains(id), () -> tag + " block tag is missing " + id);
            assertTrue(items.contains(id), () -> tag + " item tag is missing " + id);
        }
    }

    private static void assertFamilyTag(String path, String suffix, boolean includeWallVariant) {
        Set<String> values = tagValues(path);
        for (String color : CUSTOM_COLORS) {
            assertTrue(values.contains("dye_depot:" + color + "_" + suffix), () -> path + " is missing " + color);
            if (includeWallVariant) {
                assertTrue(
                        values.contains("dye_depot:" + color + "_wall_" + suffix),
                        () -> path + " is missing the " + color + " wall " + suffix
                );
            }
        }
    }
}
