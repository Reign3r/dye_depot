package com.ninni.dye_depot.data.server;

import com.ninni.dye_depot.registry.DDBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BedPart;

public class DDBlockLoot extends FabricBlockLootSubProvider {

    public DDBlockLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public void generate() {
        DDBlocks.BANNERS.values().forEach(this::dropBanner);
        DDBlocks.BEDS.values().forEach(this::dropBed);
        DDBlocks.CANDLES.values().forEach(this::dropCandle);
        DDBlocks.CANDLE_CAKES.forEachWith(DDBlocks.CANDLES, this::dropCandleCake);
        DDBlocks.CARPETS.values().forEach(this::dropSelf);
        DDBlocks.CONCRETE.values().forEach(this::dropSelf);
        DDBlocks.CONCRETE_POWDER.values().forEach(this::dropSelf);
        DDBlocks.DYE_BASKETS.values().forEach(this::dropSelf);
        DDBlocks.GLAZED_TERRACOTTA.values().forEach(this::dropSelf);
        DDBlocks.SHULKER_BOXES.values().forEach(this::dropShulkerBox);
        DDBlocks.STAINED_GLASS.values().forEach(this::dropWhenSilkTouch);
        DDBlocks.STAINED_GLASS_PANES.values().forEach(this::dropWhenSilkTouch);
        DDBlocks.TERRACOTTA.values().forEach(this::dropSelf);
        DDBlocks.WOOL.values().forEach(this::dropSelf);
    }

    private void dropBanner(Block block) {
        add(block, createBannerDrop(block));
    }

    private void dropBed(Block block) {
        add(block, createSinglePropConditionTable(block, BedBlock.PART, BedPart.HEAD));
    }

    private void dropCandle(Block block) {
        add(block, createCandleDrops(block));
    }

    private void dropCandleCake(Holder<? extends Block> block, Holder<? extends Block> candle) {
        add(block.value(), createCandleCakeDrops(candle.value()));
    }

    private void dropShulkerBox(Block block) {
        add(block, createShulkerBoxDrop(block));
    }

}
