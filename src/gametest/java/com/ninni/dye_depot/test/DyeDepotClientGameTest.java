package com.ninni.dye_depot.test;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * End-to-end client bootstrap and resource-baking checks for every registered
 * Dye Depot block and item.
 */
@SuppressWarnings("UnstableApiUsage")
public final class DyeDepotClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var modelManager = client.getModelManager();
            var missingItemModel = modelManager.getItemModel(DyeDepot.modLoc("__missing_item_model__"));
            var blockModels = modelManager.getBlockStateModelSet();
            var missingBlockModel = blockModels.missingModel();

            BuiltInRegistries.ITEM.keySet().stream()
                    .filter(id -> id.getNamespace().equals(DyeDepot.MOD_ID))
                    .forEach(id -> require(
                            modelManager.getItemModel(id) != missingItemModel,
                            "Missing baked item model: " + id
                    ));

            BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(id -> id.getNamespace().equals(DyeDepot.MOD_ID))
                    .forEach(id -> BuiltInRegistries.BLOCK.getValue(id)
                            .getStateDefinition()
                            .getPossibleStates()
                            .forEach(state -> require(
                                    blockModels.get(state) != missingBlockModel,
                                    "Missing baked block-state model: " + id + " " + state.getValues()
                            )));

            var resources = client.getResourceManager();
            for (DDDyes entry : DDDyes.values()) {
                String color = entry.getName();
                requireResource(resources, DyeDepot.modLoc("textures/entity/equipment/llama_body/" + color + ".png"));
                requireResource(resources, Identifier.fromNamespaceAndPath("minecraft", "equipment/" + color + "_carpet.json"));
                requireResource(resources, DyeDepot.modLoc("textures/entity/shulker/shulker_" + color + ".png"));
                requireResource(resources, DyeDepot.modLoc("textures/map/decorations/" + color + "_banner.png"));

                for (String face : new String[]{
                        "head_up",
                        "head_east",
                        "head_west",
                        "foot_up",
                        "foot_south",
                        "foot_east",
                        "foot_west",
                }) {
                    requireResource(resources, DyeDepot.modLoc("textures/block/" + color + "_bed_" + face + ".png"));
                }
            }

            var packs = client.getResourcePackRepository();
            String overridePack = packs.getAvailableIds().stream()
                    .filter(id -> id.contains("dye_override"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Built-in dye_override resource pack was not discovered"));
            require(
                    packs.getSelectedIds().contains(overridePack),
                    "Built-in dye_override resource pack is not enabled by default: " + overridePack
            );
        });
    }

    private static void requireResource(
            net.minecraft.server.packs.resources.ResourceManager resources,
            Identifier id
    ) {
        require(resources.getResource(id).isPresent(), "Missing client resource: " + id);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
