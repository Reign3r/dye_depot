package com.ninni.dye_depot.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Runtime integration checks that require the server's fully reloaded data.
 */
public final class DyeDepotRuntimeGameTests {

    @GameTest
    public void modifiedVanillaLootTablesContainDyeDepotEntries(GameTestHelper helper) {
        JsonObject shepherdGift = encodeLootTable(helper, BuiltInLootTables.SHEPHERD_GIFT);
        for (DDDyes color : DDDyes.values()) {
            JsonObject entry = findItemEntry(
                    helper,
                    shepherdGift,
                    DyeDepot.modLoc(color.getName() + "_wool").toString(),
                    "shepherd gift"
            );
            helper.assertValueEqual(entryWeight(entry), 1, color.getName() + " shepherd-gift wool weight");
        }

        JsonObject desertArchaeology = encodeLootTable(helper, BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY);
        JsonObject beigeDye = findItemEntry(
                helper,
                desertArchaeology,
                DyeDepot.modLoc("beige_dye").toString(),
                "desert-pyramid archaeology"
        );
        helper.assertValueEqual(entryWeight(beigeDye), 2, "beige archaeology weight");

        JsonObject coldOceanArchaeology = encodeLootTable(helper, BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY);
        JsonObject verdantDye = findItemEntry(
                helper,
                coldOceanArchaeology,
                DyeDepot.modLoc("verdant_dye").toString(),
                "cold-ocean-ruin archaeology"
        );
        helper.assertValueEqual(entryWeight(verdantDye), 3, "verdant archaeology weight");
        helper.succeed();
    }

    private static JsonObject encodeLootTable(GameTestHelper helper, ResourceKey<LootTable> key) {
        var registries = helper.getLevel().getServer().reloadableRegistries();
        LootTable table = registries.getLootTable(key);
        JsonElement encoded = LootTable.DIRECT_CODEC
                .encodeStart(registries.lookup().createSerializationContext(JsonOps.INSTANCE), table)
                .getOrThrow();
        return encoded.getAsJsonObject();
    }

    private static JsonObject findItemEntry(GameTestHelper helper, JsonObject table, String itemId, String tableName) {
        for (JsonElement poolElement : table.getAsJsonArray("pools")) {
            JsonObject pool = poolElement.getAsJsonObject();
            for (JsonElement entryElement : pool.getAsJsonArray("entries")) {
                JsonObject entry = entryElement.getAsJsonObject();
                if (entry.has("name") && itemId.equals(entry.get("name").getAsString())) {
                    return entry;
                }
            }
        }

        helper.assertTrue(false, "Missing " + itemId + " in the modified " + tableName + " loot table");
        throw new AssertionError("unreachable");
    }

    private static int entryWeight(JsonObject entry) {
        return entry.has("weight") ? entry.get("weight").getAsInt() : 1;
    }
}
