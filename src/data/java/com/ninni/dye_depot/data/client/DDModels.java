package com.ninni.dye_depot.data.client;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;

/**
 * Fabric requires one model provider to validate the complete mod registry.
 */
public final class DDModels extends FabricModelProvider {

    public DDModels(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        new DDBlockModels(generator).registerStatesAndModels();
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        new DDItemModels(generator).registerModels();
    }
}
