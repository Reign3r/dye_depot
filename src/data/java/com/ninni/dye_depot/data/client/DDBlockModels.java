package com.ninni.dye_depot.data.client;

import static net.minecraft.client.data.models.BlockModelGenerators.NOP;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_180;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_270;
import static net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_90;
import static net.minecraft.client.data.models.BlockModelGenerators.createBed;
import static net.minecraft.client.data.models.BlockModelGenerators.createSimpleBlock;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

import com.mojang.math.Transformation;
import com.ninni.dye_depot.registry.DDBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Block-model portion of the single Fabric model provider.
 */
public final class DDBlockModels {

    private final BlockModelGenerators generator;

    public DDBlockModels(BlockModelGenerators generator) {
        this.generator = generator;
    }

    public void registerStatesAndModels() {
        DDBlocks.CARPETS.forEachWith(DDBlocks.WOOL, this::woolAndCarpet);
        DDBlocks.TERRACOTTA.values().forEach(this::simpleBlock);
        DDBlocks.CONCRETE.values().forEach(this::simpleBlock);
        DDBlocks.CONCRETE_POWDER.values().forEach(this::simpleBlock);
        DDBlocks.GLAZED_TERRACOTTA.holders().forEach(this::glazedTerracotta);
        DDBlocks.STAINED_GLASS_PANES.forEachWith(DDBlocks.STAINED_GLASS, this::stainedGlassPane);
        DDBlocks.SHULKER_BOXES.holders().forEach(this::shulkerBox);
        DDBlocks.CANDLES.forEachWith(DDBlocks.CANDLE_CAKES, this::candleAndCake);
        DDBlocks.BANNERS.forEachWith(DDBlocks.WALL_BANNERS, this::banners);
        DDBlocks.BEDS.holders().forEach(this::bed);
        DDBlocks.DYE_BASKETS.holders().forEach(this::basket);
    }

    private void simpleBlock(Block block) {
        generator.createTrivialCube(block);
    }

    private void woolAndCarpet(Holder<? extends Block> carpet, Holder<? extends Block> wool) {
        generator.createFullAndCarpetBlocks(wool.value(), carpet.value());
    }

    private void glazedTerracotta(Holder<? extends Block> block) {
        Identifier model = TexturedModel.GLAZED_TERRACOTTA.get(block.value()).create(block.value(), generator.modelOutput);
        var rotation = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                .select(Direction.SOUTH, NOP)
                .select(Direction.WEST, Y_ROT_90)
                .select(Direction.NORTH, Y_ROT_180)
                .select(Direction.EAST, Y_ROT_270);
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.value(), plainVariant(model)).with(rotation));
        generator.registerSimpleItemModel(block.value(), model);
    }

    private void stainedGlassPane(Holder<? extends StainedGlassPaneBlock> pane, Holder<? extends Block> fullBlock) {
        generator.createGlassBlocks(fullBlock.value(), pane.value());
    }

    private void shulkerBox(Holder<? extends ShulkerBoxBlock> block) {
        generator.createShulkerBox(block.value(), block.value().getColor());
    }

    private void candleAndCake(Holder<? extends Block> candle, Holder<? extends Block> candleCake) {
        generator.createCandleAndCandleCake(candle.value(), candleCake.value());
    }

    private void banners(Holder<? extends BannerBlock> standing, Holder<? extends WallBannerBlock> wall) {
        MultiVariant blockModel = plainVariant(ModelLocationUtils.decorateBlockModelLocation("banner"));
        generator.blockStateOutput.accept(createSimpleBlock(standing.value(), blockModel));
        generator.blockStateOutput.accept(createSimpleBlock(wall.value(), blockModel));

        var item = standing.value().asItem();
        Identifier itemModel = ModelLocationUtils.decorateItemModelLocation("template_banner");
        generator.itemModelOutput.accept(
                item,
                ItemModelUtils.specialModel(
                        itemModel,
                        BannerRenderer.TRANSFORMATIONS.freeTransformations(0),
                        new BannerSpecialRenderer.Unbaked(
                                standing.value().getColor(),
                                BannerBlock.AttachmentType.GROUND
                        )
                )
        );
    }

    private void bed(Holder<? extends BedBlock> holder) {
        BedBlock bed = holder.value();
        Identifier head = ModelTemplates.BED_HEAD.createWithSuffix(
                bed,
                "_" + BedPart.HEAD,
                TextureMapping.bed(bed, BedPart.HEAD),
                generator.modelOutput
        );
        Identifier foot = ModelTemplates.BED_FOOT.createWithSuffix(
                bed,
                "_" + BedPart.FOOT,
                TextureMapping.bed(bed, BedPart.FOOT),
                generator.modelOutput
        );
        generator.blockStateOutput.accept(createBed(bed, plainVariant(head), plainVariant(foot)));

        Transformation footTransformation = new Transformation(
                new org.joml.Vector3f(0.0F, 0.0F, 1.0F),
                null,
                null,
                null
        );
        ItemModel.Unbaked headModel = ItemModelUtils.plainModel(head);
        ItemModel.Unbaked footModel = ItemModelUtils.plainModel(foot, footTransformation);
        generator.itemModelOutput.accept(bed.asItem(), ItemModelUtils.composite(headModel, footModel));
    }

    private void basket(Holder<? extends Block> block) {
        generator.createHorizontallyRotatedBlock(block.value(), TexturedModel.ORIENTABLE);
    }
}
