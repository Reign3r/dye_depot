package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import com.ninni.dye_depot.registry.DyedHolders;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class DDPolymerCreativeTab {
    private static final List<DyeColor> CUSTOM_COLOR_ORDER = List.of(
            DDDyes.MAROON.get(), DDDyes.ROSE.get(), DDDyes.CORAL.get(),
            DDDyes.GINGER.get(), DDDyes.TAN.get(), DDDyes.BEIGE.get(),
            DDDyes.AMBER.get(), DDDyes.OLIVE.get(), DDDyes.FOREST.get(),
            DDDyes.VERDANT.get(), DDDyes.TEAL.get(), DDDyes.MINT.get(),
            DDDyes.AQUA.get(), DDDyes.SLATE.get(), DDDyes.NAVY.get(),
            DDDyes.INDIGO.get()
    );
    private static final List<DyeColor> BASKET_COLOR_ORDER = List.of(
            DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BROWN,
            DDDyes.MAROON.get(), DDDyes.ROSE.get(), DyeColor.RED, DDDyes.CORAL.get(),
            DDDyes.GINGER.get(), DyeColor.ORANGE, DDDyes.TAN.get(), DDDyes.BEIGE.get(),
            DyeColor.YELLOW, DDDyes.AMBER.get(), DDDyes.OLIVE.get(), DyeColor.LIME,
            DDDyes.FOREST.get(), DyeColor.GREEN, DDDyes.VERDANT.get(), DDDyes.TEAL.get(),
            DyeColor.CYAN, DDDyes.MINT.get(), DDDyes.AQUA.get(), DyeColor.LIGHT_BLUE,
            DyeColor.BLUE, DDDyes.SLATE.get(), DDDyes.NAVY.get(), DDDyes.INDIGO.get(),
            DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK
    );
    private static final List<Item> ITEMS = createItems();

    private DDPolymerCreativeTab() {
    }

    static void register() {
        CreativeModeTab.Builder builder = PolymerCreativeModeTabUtils.builder();
        builder.icon(() -> new ItemStack(DDItems.DYES.getOrThrow(DDDyes.AMBER.get())));
        builder.title(Component.literal("Dye Depot"));
        builder.displayItems((context, output) -> ITEMS.forEach(output::accept));
        PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(DyeDepot.modLoc("items"), builder.build());
    }

    public static List<Item> items() {
        return ITEMS;
    }

    private static List<Item> createItems() {
        List<Item> items = new ArrayList<>(240);
        addCustomColors(items, DDItems.DYES);
        BASKET_COLOR_ORDER.forEach(color -> items.add(DDBlocks.DYE_BASKETS.getOrThrow(color).asItem()));
        addCustomColors(items, DDBlocks.WOOL);
        addCustomColors(items, DDBlocks.CARPETS);
        addCustomColors(items, DDBlocks.TERRACOTTA);
        addCustomColors(items, DDBlocks.GLAZED_TERRACOTTA);
        addCustomColors(items, DDBlocks.CONCRETE);
        addCustomColors(items, DDBlocks.CONCRETE_POWDER);
        addCustomColors(items, DDBlocks.STAINED_GLASS);
        addCustomColors(items, DDBlocks.STAINED_GLASS_PANES);
        addCustomColors(items, DDItems.SHULKER_BOXES);
        addCustomColors(items, DDItems.BEDS);
        addCustomColors(items, DDBlocks.CANDLES);
        addCustomColors(items, DDItems.BANNERS);
        return List.copyOf(items);
    }

    private static void addCustomColors(List<Item> output, DyedHolders<?, ? extends ItemLike> holders) {
        CUSTOM_COLOR_ORDER.forEach(color -> output.add(holders.getOrThrow(color).asItem()));
    }
}
