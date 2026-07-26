package com.ninni.dye_depot.test;

import static com.ninni.dye_depot.test.ResourceTestSupport.CUSTOM_COLORS;
import static com.ninni.dye_depot.test.ResourceTestSupport.json;
import static com.ninni.dye_depot.test.ResourceTestSupport.jsonFilesUnder;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredObject;
import static com.ninni.dye_depot.test.ResourceTestSupport.requiredString;
import static com.ninni.dye_depot.test.ResourceTestSupport.tagValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TradeResourceParityTest {

    private static final Set<String> SHEPHERD_LEVEL_2_DYES = Set.of("tan", "aqua", "coral", "beige", "amber");
    private static final Set<String> SHEPHERD_LEVEL_3_DYES = Set.of("rose", "slate", "mint", "forest", "ginger");
    private static final Set<String> SHEPHERD_LEVEL_4_DYES =
            Set.of("maroon", "indigo", "teal", "verdant", "navy", "olive");

    private static final Map<String, Integer> TAG_COUNTS = Map.of(
            "cartographer/level_4", 16,
            "mason/level_4", 32,
            "shepherd/level_2", 37,
            "shepherd/level_3", 21,
            "shepherd/level_4", 22,
            "wandering_trader/common", 16
    );

    @Test
    void tradeRecordAndAppendTagCountsPreserveAll144Offers() {
        assertEquals(144, jsonFilesUnder("data/dye_depot/villager_trade").size());
        assertEquals(6, jsonFilesUnder("data/minecraft/tags/villager_trade").size());

        int references = 0;
        for (var entry : TAG_COUNTS.entrySet()) {
            Set<String> values = tagValues("data/minecraft/tags/villager_trade/" + entry.getKey() + ".json");
            assertEquals(entry.getValue(), values.size(), entry.getKey());
            references += values.size();
        }
        assertEquals(144, references);
    }

    @Test
    void everyColorHasTheExactProfessionAndWanderingOffers() {
        for (String color : CUSTOM_COLORS) {
            assertTrade(
                    "cartographer/4/emerald_" + color + "_banner",
                    "minecraft:emerald",
                    3,
                    "dye_depot:" + color + "_banner",
                    1,
                    12,
                    15
            );
            assertTrade(
                    "mason/4/emerald_" + color + "_terracotta",
                    "minecraft:emerald",
                    1,
                    "dye_depot:" + color + "_terracotta",
                    1,
                    12,
                    15
            );
            assertTrade(
                    "mason/4/emerald_" + color + "_glazed_terracotta",
                    "minecraft:emerald",
                    1,
                    "dye_depot:" + color + "_glazed_terracotta",
                    1,
                    12,
                    15
            );
            assertTrade(
                    "shepherd/2/emerald_" + color + "_wool",
                    "minecraft:emerald",
                    1,
                    "dye_depot:" + color + "_wool",
                    1,
                    16,
                    5
            );
            assertTrade(
                    "shepherd/2/emerald_" + color + "_carpet",
                    "minecraft:emerald",
                    1,
                    "dye_depot:" + color + "_carpet",
                    4,
                    16,
                    5
            );
            assertTrade(
                    "shepherd/3/emerald_" + color + "_bed",
                    "minecraft:emerald",
                    3,
                    "dye_depot:" + color + "_bed",
                    1,
                    12,
                    10
            );
            assertTrade(
                    "shepherd/4/emerald_" + color + "_banner",
                    "minecraft:emerald",
                    3,
                    "dye_depot:" + color + "_banner",
                    1,
                    12,
                    15
            );
            assertTrade(
                    "wandering_trader/emerald_" + color + "_dye",
                    "minecraft:emerald",
                    1,
                    "dye_depot:" + color + "_dye",
                    3,
                    12,
                    null
            );

            assertDyePurchaseIfPresent(color, 2, SHEPHERD_LEVEL_2_DYES);
            assertDyePurchaseIfPresent(color, 3, SHEPHERD_LEVEL_3_DYES);
            assertDyePurchaseIfPresent(color, 4, SHEPHERD_LEVEL_4_DYES);
        }
    }

    @Test
    void tradeTagsReferenceEveryRecordExactlyOnce() {
        var referenced = new LinkedHashSet<String>();
        for (String tag : TAG_COUNTS.keySet()) {
            for (String id : tagValues("data/minecraft/tags/villager_trade/" + tag + ".json")) {
                assertTrue(id.startsWith("dye_depot:"), () -> tag + " has a foreign trade " + id);
                assertTrue(referenced.add(id), () -> id + " appears in more than one vanilla append tag");

                String path = "data/dye_depot/villager_trade/" + id.substring("dye_depot:".length()) + ".json";
                json(path);
            }
        }
        assertEquals(144, referenced.size());
    }

    private static void assertDyePurchaseIfPresent(String color, int level, Set<String> colorsAtLevel) {
        String path = "data/dye_depot/villager_trade/shepherd/" + level + "/" + color + "_dye_emerald.json";
        if (colorsAtLevel.contains(color)) {
            assertTrade(
                    "shepherd/" + level + "/" + color + "_dye_emerald",
                    "dye_depot:" + color + "_dye",
                    12,
                    "minecraft:emerald",
                    1,
                    16,
                    30
            );
        } else {
            assertFalse(ResourceTestSupport.exists(path), () -> path + " is assigned to the wrong shepherd level");
        }
    }

    private static void assertTrade(
            String id,
            String wanted,
            int wantedCount,
            String given,
            int givenCount,
            int maxUses,
            Integer xp
    ) {
        String path = "data/dye_depot/villager_trade/" + id + ".json";
        JsonObject trade = json(path);
        assertStack(requiredObject(trade, "wants", path), wanted, wantedCount, path);
        assertStack(requiredObject(trade, "gives", path), given, givenCount, path);
        assertEquals(maxUses, trade.get("max_uses").getAsInt(), path);
        assertEquals(0.05F, trade.get("reputation_discount").getAsFloat(), path);
        if (xp == null) {
            assertFalse(trade.has("xp"), path + " must retain the wandering-trader default XP");
        } else {
            assertEquals(xp.intValue(), trade.get("xp").getAsInt(), path);
        }
    }

    private static void assertStack(JsonObject stack, String id, int count, String path) {
        assertEquals(id, requiredString(stack, "id", path), path);
        int encodedCount = stack.has("count") ? stack.get("count").getAsInt() : 1;
        assertEquals(count, encodedCount, path);
    }
}
