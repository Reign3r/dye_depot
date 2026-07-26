package com.ninni.dye_depot.data.server;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDBlocks;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class DDEntityLoot extends SimpleFabricLootTableSubProvider {

    private static final ResourceKey<LootTable> VANILLA_SHEEP = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.withDefaultNamespace("entities/sheep")
    );

    public DDEntityLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, LootContextParamSets.ENTITY);
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        DDBlocks.WOOL.forEach((color, wool) ->
                consumer.accept(
                        ResourceKey.create(
                                Registries.LOOT_TABLE,
                                Identifier.fromNamespaceAndPath(DyeDepot.MOD_ID, "entities/sheep/" + color.getSerializedName())
                        ),
                        LootTable.lootTable()
                                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(wool.value())))
                                .withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(VANILLA_SHEEP)))
                )
        );
    }

}
