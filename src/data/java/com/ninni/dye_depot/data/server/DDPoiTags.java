package com.ninni.dye_depot.data.server;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDTags;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class DDPoiTags extends FabricTagsProvider<PoiType> {

    public DDPoiTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, Registries.POINT_OF_INTEREST_TYPE, lookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DDTags.BEDS)
                .add(ResourceKey.create(
                        Registries.POINT_OF_INTEREST_TYPE,
                        Identifier.fromNamespaceAndPath(DyeDepot.MOD_ID, "home")
                ))
                .add(PoiTypes.HOME);

        tag(TagKey.create(Registries.POINT_OF_INTEREST_TYPE, Identifier.withDefaultNamespace("village")))
                .addTag(DDTags.BEDS);
    }

}
