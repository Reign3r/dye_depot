package com.ninni.dye_depot.registry;

import com.ninni.dye_depot.DyeDepot;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class DDItems {

    public static final DyedHolders<Item, Item> DYES = DyedHolders.createModded(dye ->
            register(dye + "_dye", properties -> new DyeItem(properties.component(DataComponents.DYE, dye)))
    );

    public static final DyedHolders<Item, Item> SHULKER_BOXES = DyedHolders.createModded(dye ->
            register(dye + "_shulker_box", properties -> new BlockItem(
                    DDBlocks.SHULKER_BOXES.getOrThrow(dye),
                    properties
                            .stacksTo(1)
                            .useBlockDescriptionPrefix()
                            .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
            ))
    );

    public static final DyedHolders<Item, Item> BANNERS = DyedHolders.createModded(dye ->
            register(dye + "_banner", properties -> new BannerItem(DDBlocks.BANNERS.getOrThrow(dye), DDBlocks.WALL_BANNERS.getOrThrow(dye), properties
                .useBlockDescriptionPrefix()
                .stacksTo(16)
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            ))
    );

    public static final DyedHolders<Item, Item> BEDS = DyedHolders.createModded(dye ->
            register(dye + "_bed", properties -> new BedItem(
                    DDBlocks.BEDS.getOrThrow(dye),
                    properties.stacksTo(1).useBlockDescriptionPrefix()
            ))
    );

    @SuppressWarnings("unchecked")
    private static <T extends Item> Holder<T> register(String id, Function<Item.Properties, T> item) {
        var key = DyeDepot.key(Registries.ITEM, id);
        var value = item.apply(new Item.Properties().setId(key));
        if (value instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, value);
        }
        return (Holder<T>) Registry.registerForHolder(BuiltInRegistries.ITEM, key, value);
    }
}
