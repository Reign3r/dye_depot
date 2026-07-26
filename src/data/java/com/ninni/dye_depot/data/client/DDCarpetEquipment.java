package com.ninni.dye_depot.data.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.data.DDJsonProvider;
import com.ninni.dye_depot.data.ModCompat;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

/**
 * Defines the equipment assets addressed by {@code Equippable.llamaSwag}.
 *
 * <p>Minecraft owns the carpet equipment keys, while Dye Depot owns the
 * corresponding textures.</p>
 */
public final class DDCarpetEquipment extends DDJsonProvider {

    public DDCarpetEquipment(FabricPackOutput output) {
        super(output);
    }

    @Override
    protected void generate(Output output) {
        ModCompat.colors().forEach(color -> {
            String name = color.getSerializedName();

            var layer = new JsonObject();
            layer.addProperty("texture", DyeDepot.MOD_ID + ":" + name);

            var llamaBody = new JsonArray();
            llamaBody.add(layer);

            var layers = new JsonObject();
            layers.add("llama_body", llamaBody);

            var equipment = new JsonObject();
            equipment.add("layers", layers);

            output.accept(asset("minecraft", "equipment", name + "_carpet"), equipment);
        });
    }

    @Override
    public String getName() {
        return "Dye Depot carpet equipment";
    }
}
