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
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * The shearing context is distinct from entity-death loot in 26.2.
 */
public final class DDSheepShearingLoot extends SimpleFabricLootTableSubProvider {

    public DDSheepShearingLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, LootContextParamSets.SHEARING);
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        DDBlocks.WOOL.forEach((color, wool) ->
                consumer.accept(
                        ResourceKey.create(
                                Registries.LOOT_TABLE,
                                Identifier.fromNamespaceAndPath(DyeDepot.MOD_ID, "shearing/sheep/" + color.getSerializedName())
                        ),
                        LootTable.lootTable().withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                        .add(LootItem.lootTableItem(wool.value()))
                        )
                )
        );
    }
}
