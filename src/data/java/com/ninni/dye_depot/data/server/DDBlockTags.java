package com.ninni.dye_depot.data.server;

import com.ninni.dye_depot.data.ModCompat;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DyedHolders;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class DDBlockTags extends FabricTagsProvider.BlockTagsProvider {

    public DDBlockTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        groupDyedTag("dyed");

        tagDyed(DDBlocks.SHULKER_BOXES, BlockTags.SHULKER_BOXES);
        tagDyed(DDBlocks.BANNERS, BlockTags.BANNERS);
        tagDyed(DDBlocks.WALL_BANNERS, BlockTags.BANNERS);
        tagDyed(DDBlocks.CARPETS, BlockTags.WOOL_CARPETS);
        tagDyed(DDBlocks.CANDLES, BlockTags.CANDLES);
        tagDyed(DDBlocks.CANDLE_CAKES, BlockTags.CANDLE_CAKES);
        tagDyed(DDBlocks.BEDS, BlockTags.BEDS);
        tagDyed(DDBlocks.WOOL, BlockTags.WOOL);
        tagDyed(DDBlocks.TERRACOTTA, BlockTags.TERRACOTTA, BlockTags.MINEABLE_WITH_PICKAXE);
        tagDyed(DDBlocks.GLAZED_TERRACOTTA, BlockTags.MINEABLE_WITH_PICKAXE);
        tagDyed(DDBlocks.CONCRETE, BlockTags.MINEABLE_WITH_PICKAXE, loaderTag("concretes"));
        tagDyed(DDBlocks.CONCRETE_POWDER, BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.CONCRETE_POWDERS, BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS);
        tagDyed(DDBlocks.STAINED_GLASS, loaderTag("glass_blocks"), BlockTags.IMPERMEABLE);
        tagDyed(DDBlocks.STAINED_GLASS_PANES, loaderTag("glass_panes"), BlockTags.IMPERMEABLE);
        tagDyed(DDBlocks.DYE_BASKETS, BlockTags.MINEABLE_WITH_HOE, supplementariesTag("non_cleanable"));

        tagCompat(ModCompat.SUPPLEMENTARIES, "candle_holder", supplementariesTag("candle_holders"));
        tagCompat(ModCompat.SUPPLEMENTARIES_SQUARED, "gold_candle_holder", supplementariesTag("candle_holders"), BlockTags.GUARDED_BY_PIGLINS);
        tagCompat(ModCompat.SUPPLEMENTARIES, "flag", supplementariesTag("flags"));
        tagCompat(ModCompat.SUPPLEMENTARIES, "present", supplementariesTag("presents"));
        tagCompat(ModCompat.SUPPLEMENTARIES, "trapped_present", supplementariesTag("trapped_presents"));
    }

    private void tag(DyedHolders<?, Block> values, TagKey<Block> tag) {
        values.holders()
                .map(it -> ResourceKey.create(Registries.BLOCK, it.unwrapKey().orElseThrow().identifier()))
                .forEach(it -> tag(tag).addOptional(it));
    }

    private void groupDyedTag(String base) {
        Stream.concat(DyedHolders.vanillaColors(), DyedHolders.modColors()).forEach(color ->
                tag(loaderTag(base)).addOptionalTag(loaderTag(base + "/" + color.getSerializedName()))
        );
    }

    @SafeVarargs
    private void tagDyed(DyedHolders<?, Block> values, TagKey<Block>... additionalTags) {
        values.forEach((dye, block) -> {
            var key = ResourceKey.create(Registries.BLOCK, block.unwrapKey().orElseThrow().identifier());
            var tag = loaderTag("dyed/" + dye.getSerializedName());
            tag(tag).addOptional(key);
        });

        for (var tag : additionalTags) {
            tag(values, tag);
        }
    }

    @SafeVarargs
    private void tagCompat(String namespace, String name, TagKey<Block>... additionalTags) {
        ModCompat.colors().forEach(color -> {
            var key = ModCompat.key(Registries.BLOCK, namespace, name, color);
            tag(loaderTag("dyed/" + color.getSerializedName())).addOptional(key);
            for (var additionalTag : additionalTags) {
                tag(additionalTag).addOptional(key);
            }
        });
    }

    private TagKey<Block> loaderTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
    }

    private TagKey<Block> supplementariesTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ModCompat.SUPPLEMENTARIES, path));
    }

}
