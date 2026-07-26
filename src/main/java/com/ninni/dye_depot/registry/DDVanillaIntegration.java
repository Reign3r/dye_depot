package com.ninni.dye_depot.registry;

import com.ninni.dye_depot.mixin.CauldronInteractionsAccessor;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;

public class DDVanillaIntegration {

    public static void commonInit() {
        registerLootTableAdditions();
        registerCauldronInteractions();
    }

    private static void registerLootTableAdditions() {
        LootTableEvents.MODIFY.register((id, tableBuilder, source, provider) -> {
            if (id.equals(BuiltInLootTables.SHEPHERD_GIFT))
                tableBuilder.modifyPools(builder ->
                        DDBlocks.WOOL.forEach((dye, block) ->
                                builder.add(LootItem.lootTableItem(block.value()))
                        )
                );
            if (id.equals(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY))
                tableBuilder.modifyPools(builder -> builder.add(LootItem.lootTableItem(DDItems.DYES.getOrThrow(DDDyes.BEIGE.get())).setWeight(2)));
            if (id.equals(BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY))
                tableBuilder.modifyPools(builder -> builder.add(LootItem.lootTableItem(DDItems.DYES.getOrThrow(DDDyes.VERDANT.get())).setWeight(3)));
        });
    }

    private static void registerCauldronInteractions() {
        var interactions = CauldronInteractions.WATER;

        DDItems.SHULKER_BOXES.values().forEach(item ->
                interactions.put(item, CauldronInteractionsAccessor::DD$shulkerBoxInteraction)
        );
        DDItems.BANNERS.values().forEach(item ->
                interactions.put(item, CauldronInteractionsAccessor::DD$bannerInteraction)
        );
    }

}
