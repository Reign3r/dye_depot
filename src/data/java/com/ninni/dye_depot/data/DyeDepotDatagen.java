package com.ninni.dye_depot.data;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.data.client.DDCarpetEquipment;
import com.ninni.dye_depot.data.client.DDCompatModels;
import com.ninni.dye_depot.data.client.DDLang;
import com.ninni.dye_depot.data.client.DDLangOverrides;
import com.ninni.dye_depot.data.client.DDModels;
import com.ninni.dye_depot.data.server.DDBlockLoot;
import com.ninni.dye_depot.data.server.DDBlockTags;
import com.ninni.dye_depot.data.server.DDCompatLoot;
import com.ninni.dye_depot.data.server.DDCompatRecipes;
import com.ninni.dye_depot.data.server.DDEntityLoot;
import com.ninni.dye_depot.data.server.DDItemTags;
import com.ninni.dye_depot.data.server.DDPoiTags;
import com.ninni.dye_depot.data.server.DDRecipes;
import com.ninni.dye_depot.data.server.DDSheepShearingLoot;
import com.ninni.dye_depot.data.server.DDTrades;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DyeDepotDatagen implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();

        pack.addProvider((output, $) -> new DDPackMetadata(output, Component.literal(DyeDepot.MOD_ID + " resources")));

        var blockTags = pack.addProvider(DDBlockTags::new);
        pack.addProvider((output, lookup) -> new DDItemTags(output, lookup, blockTags));
        pack.addProvider(DDPoiTags::new);
        pack.addProvider(DDBlockLoot::new);
        pack.addProvider(DDEntityLoot::new);
        pack.addProvider(DDSheepShearingLoot::new);
        pack.addProvider(DDRecipes.Runner::new);
        pack.addProvider(DDCompatRecipes::new);
        pack.addProvider(DDCompatLoot::new);
        pack.addProvider(DDTrades::new);

        pack.addProvider(DDModels::new);
        pack.addProvider(DDCarpetEquipment::new);
        pack.addProvider(DDCompatModels::new);
        pack.addProvider(DDLang::new);

        var overrides = generator.createBuiltinResourcePack(
                Identifier.fromNamespaceAndPath(DyeDepot.MOD_ID, "dye_override")
        );
        overrides.addProvider((output, $) -> new DDPackMetadata(output, Component.literal("Slight dye adjustments").withStyle(ChatFormatting.GRAY)));
        overrides.addProvider(DDLangOverrides::new);
    }

}
