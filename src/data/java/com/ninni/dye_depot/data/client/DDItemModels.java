package com.ninni.dye_depot.data.client;

import com.ninni.dye_depot.registry.DDItems;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

/**
 * Item-model portion of the single Fabric model provider.
 */
public final class DDItemModels {

    private final ItemModelGenerators generator;

    public DDItemModels(ItemModelGenerators generator) {
        this.generator = generator;
    }

    public void registerModels() {
        DDItems.DYES.values().forEach(item -> generator.generateFlatItem(item, ModelTemplates.FLAT_ITEM));
    }
}
