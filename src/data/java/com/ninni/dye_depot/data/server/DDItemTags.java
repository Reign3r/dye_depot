package com.ninni.dye_depot.data.server;

import com.ninni.dye_depot.data.ModCompat;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDItems;
import com.ninni.dye_depot.registry.DDTags;
import com.ninni.dye_depot.registry.DyedHolders;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class DDItemTags extends FabricTagsProvider.ItemTagsProvider {

    public DDItemTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup, FabricTagsProvider.BlockTagsProvider blockTags) {
        super(output, lookup, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        groupDyedTag("dyed");
        groupDyedTag("dyes");

        tagDyed(
                DDItems.DYES,
                loaderTag("dyes"),
                ItemTags.DYES,
                ItemTags.LOOM_DYES,
                ItemTags.CAT_COLLAR_DYES,
                ItemTags.WOLF_COLLAR_DYES
        );

        tagDyed(DDBlocks.SHULKER_BOXES, loaderTag("shulker_boxes"));
        tagDyed(DDBlocks.BANNERS, ItemTags.BANNERS);
        tagDyed(DDBlocks.CARPETS, ItemTags.WOOL_CARPETS);
        tagDyed(DDBlocks.CANDLES, ItemTags.CANDLES);
        tagDyed(DDBlocks.BEDS, ItemTags.BEDS);
        tagDyed(DDBlocks.WOOL, ItemTags.WOOL);
        tagDyed(DDBlocks.TERRACOTTA, ItemTags.TERRACOTTA);
        tagDyed(DDBlocks.GLAZED_TERRACOTTA);
        tagDyed(DDBlocks.CONCRETE, loaderTag("concretes"));
        tagDyed(DDBlocks.CONCRETE_POWDER, loaderTag("concrete_powder"), loaderTag("concrete_powders"));
        tagDyed(DDBlocks.STAINED_GLASS, loaderTag("glass_blocks"));
        tagDyed(DDBlocks.STAINED_GLASS_PANES, loaderTag("glass_panes"));
        tagDyed(DDBlocks.DYE_BASKETS, supplementariesTag("non_cleanable"));

        tagCompat(ModCompat.SUPPLEMENTARIES, "candle_holder", supplementariesTag("candle_holders"));
        tagCompat(ModCompat.SUPPLEMENTARIES_SQUARED, "gold_candle_holder", supplementariesTag("candle_holders"), ItemTags.PIGLIN_LOVED);
        tagCompat(ModCompat.SUPPLEMENTARIES, "flag", supplementariesTag("flags"));
        tagCompat(ModCompat.SUPPLEMENTARIES, "present", supplementariesTag("presents"));
        tagCompat(ModCompat.SUPPLEMENTARIES, "trapped_present", supplementariesTag("trapped_presents"));

        tag(DDTags.SMELTS_INTO_CORAL_DYE).add(
                Items.TUBE_CORAL.builtInRegistryHolder().key(),
                Items.BRAIN_CORAL.builtInRegistryHolder().key(),
                Items.BUBBLE_CORAL.builtInRegistryHolder().key(),
                Items.FIRE_CORAL.builtInRegistryHolder().key(),
                Items.HORN_CORAL.builtInRegistryHolder().key(),
                Items.TUBE_CORAL_FAN.builtInRegistryHolder().key(),
                Items.BRAIN_CORAL_FAN.builtInRegistryHolder().key(),
                Items.BUBBLE_CORAL_FAN.builtInRegistryHolder().key(),
                Items.FIRE_CORAL_FAN.builtInRegistryHolder().key(),
                Items.HORN_CORAL_FAN.builtInRegistryHolder().key(),
                Items.TUBE_CORAL_BLOCK.builtInRegistryHolder().key(),
                Items.BRAIN_CORAL_BLOCK.builtInRegistryHolder().key(),
                Items.BUBBLE_CORAL_BLOCK.builtInRegistryHolder().key(),
                Items.FIRE_CORAL_BLOCK.builtInRegistryHolder().key(),
                Items.HORN_CORAL_BLOCK.builtInRegistryHolder().key()
        );
    }

    private void tag(DyedHolders<?, ? extends ItemLike> values, TagKey<Item> tag) {
        values.holders()
                .map(it -> ResourceKey.create(Registries.ITEM, it.unwrapKey().orElseThrow().identifier()))
                .forEach(it -> tag(tag).addOptional(it));
    }

    private void groupDyedTag(String base) {
        Stream.concat(DyedHolders.vanillaColors(), DyedHolders.modColors()).forEach(color ->
            tag(loaderTag(base)).addOptionalTag(loaderTag(base + "/" + color.getSerializedName()))
        );
    }

    @SafeVarargs
    private void tagDyed(DyedHolders<?, ? extends ItemLike> values, TagKey<Item>... additionalTags) {
        tagDyed(values, "dyed", additionalTags);
    }

    @SafeVarargs
    private void tagDyed(DyedHolders<?, ? extends ItemLike> values, String base, TagKey<Item>... additionalTags) {
        values.forEach((dye, item) -> {
            var key = ResourceKey.create(Registries.ITEM, item.unwrapKey().orElseThrow().identifier());
            var tag = loaderTag(base + "/" + dye.getSerializedName());
            tag(tag).addOptional(key);
        });

        for (var tag : additionalTags) {
            tag(values, tag);
        }
    }

    @SafeVarargs
    private void tagCompat(String namespace, String name, TagKey<Item>... additionalTags) {
        ModCompat.colors().forEach(color -> {
            var key = ModCompat.key(Registries.ITEM, namespace, name, color);
            tag(loaderTag("dyed/" + color.getSerializedName())).addOptional(key);
            for (var additionalTag : additionalTags) {
                tag(additionalTag).addOptional(key);
            }
        });
    }

    private TagKey<Item> loaderTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private TagKey<Item> supplementariesTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModCompat.SUPPLEMENTARIES, path));
    }

}
