package com.ninni.dye_depot.registry;

import com.google.common.collect.ImmutableMap;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.block.*;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class DDBlocks {

    public static final DyedHolders<Block, Block> WOOL = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_wool", Block::new, () -> Properties.ofFullCopy(Blocks.WOOL.pick(DyeColor.WHITE)).mapColor(dye))
    );

    public static final DyedHolders<Block, Block> CARPETS = DyedHolders.createModded(dye ->
            registerWithItem(
                    dye + "_carpet",
                    properties -> new WoolCarpetBlock(dye, properties),
                    () -> Properties.ofFullCopy(Blocks.CARPET.pick(DyeColor.WHITE)).mapColor(dye),
                    properties -> properties.component(DataComponents.EQUIPPABLE, Equippable.llamaSwag(dye))
            )
    );

    public static final DyedHolders<Block, Block> TERRACOTTA = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_terracotta", Block::new, () -> Properties.ofFullCopy(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)).mapColor(dye))
    );

    private static final Map<DyeColor, MapColor> CONCRETE_COLORS = new ImmutableMap.Builder<DyeColor, MapColor>()
            .put(DDDyes.MAROON.get(), MapColor.CRIMSON_HYPHAE)
            .put(DDDyes.ROSE.get(), MapColor.COLOR_RED)
            .put(DDDyes.CORAL.get(), MapColor.PODZOL)
            .put(DDDyes.INDIGO.get(), MapColor.WARPED_HYPHAE)
            .put(DDDyes.NAVY.get(), MapColor.TERRACOTTA_BLACK)
            .put(DDDyes.SLATE.get(), MapColor.COLOR_GRAY)
            .put(DDDyes.OLIVE.get(), MapColor.COLOR_BROWN)
            .put(DDDyes.AMBER.get(), MapColor.WOOD)
            .put(DDDyes.BEIGE.get(), MapColor.TERRACOTTA_WHITE)
            .put(DDDyes.TEAL.get(), MapColor.COLOR_GRAY)
            .put(DDDyes.MINT.get(), MapColor.DEEPSLATE)
            .put(DDDyes.AQUA.get(), MapColor.WARPED_WART_BLOCK)
            .put(DDDyes.VERDANT.get(), MapColor.TERRACOTTA_BLACK)
            .put(DDDyes.FOREST.get(), MapColor.COLOR_GREEN)
            .put(DDDyes.GINGER.get(), MapColor.NETHER)
            .put(DDDyes.TAN.get(), MapColor.DIRT)
            .build();

    public static final DyedHolders<Block, Block> CONCRETE = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_concrete", Block::new, () -> Properties.ofFullCopy(Blocks.CONCRETE.pick(DyeColor.WHITE)).mapColor(CONCRETE_COLORS.get(dye)))
    );

    public static final DyedHolders<Block, Block> CONCRETE_POWDER = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_concrete_powder", properties -> new ConcretePowderBlock(CONCRETE.getOrThrow(dye), properties), () -> Properties.ofFullCopy(Blocks.CONCRETE_POWDER.pick(DyeColor.WHITE)).mapColor(dye))
    );

    public static final DyedHolders<Block, Block> GLAZED_TERRACOTTA = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_glazed_terracotta", GlazedTerracottaBlock::new, () -> Properties.ofFullCopy(Blocks.GLAZED_TERRACOTTA.pick(DyeColor.WHITE)).mapColor(dye))
    );

    public static final DyedHolders<Block, Block> STAINED_GLASS = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_stained_glass", properties -> new StainedGlassBlock(dye, properties), () -> Properties.ofFullCopy(Blocks.STAINED_GLASS.pick(DyeColor.WHITE)))
    );

    public static final DyedHolders<StainedGlassPaneBlock, Block> STAINED_GLASS_PANES = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_stained_glass_pane", properties -> new StainedGlassPaneBlock(dye, properties), () -> Properties.ofFullCopy(Blocks.STAINED_GLASS_PANE.pick(DyeColor.WHITE)))
    );

    public static final DyedHolders<ShulkerBoxBlock, Block> SHULKER_BOXES = DyedHolders.createModded(dye ->
            register(dye + "_shulker_box", properties -> shulkerBox(dye, properties.mapColor(dye)))
    );

    public static final DyedHolders<CandleBlock, Block> CANDLES = DyedHolders.createModded(dye ->
            registerWithItem(dye + "_candle", CandleBlock::new, () -> Properties.ofFullCopy(Blocks.DYED_CANDLE.pick(DyeColor.WHITE)).mapColor(dye))
    );

    public static final DyedHolders<CandleCakeBlock, Block> CANDLE_CAKES = DyedHolders.createModded(dye ->
            register(dye + "_candle_cake", properties -> new CandleCakeBlock(CANDLES.getOrThrow(dye), properties), () -> Properties.ofFullCopy(Blocks.DYED_CANDLE_CAKE.pick(DyeColor.WHITE)))
    );

    public static final DyedHolders<BannerBlock, Block> BANNERS = DyedHolders.createModded(dye ->
            register(dye + "_banner", properties -> banner(dye, properties), DDBlocks::bannerProperties)
    );

    public static final DyedHolders<WallBannerBlock, Block> WALL_BANNERS = DyedHolders.createModded(dye ->
            register(dye + "_wall_banner", properties -> wallBanner(dye, properties), () -> wallBannerProperties(dye))
    );

    public static final DyedHolders<BedBlock, Block> BEDS = DyedHolders.createModded(dye ->
            register(dye + "_bed", properties -> bed(dye, properties))
    );

    public static final DyedHolders<Block, Block> DYE_BASKETS = DyedHolders.createWithVanilla(dye ->
            registerWithItem(dye + "_dye_basket", properties -> new DyeBasketBlock(dye, properties.strength(0.8f).sound(SoundType.WOOL).ignitedByLava().mapColor(dye)))
    );

    private static Properties bannerProperties() {
        return Properties.ofFullCopy(Blocks.BANNER.pick(DyeColor.WHITE));
    }

    private static Properties wallBannerProperties(DyeColor dye) {
        return Properties.ofFullCopy(Blocks.WALL_BANNER.pick(DyeColor.WHITE)).overrideLootTable(BANNERS.getOrThrow(dye).getLootTable());
    }

    private static BannerBlock banner(DyeColor dye, Properties properties) {
        return addValidBlock(BlockEntityTypes.BANNER, new BannerBlock(dye, properties));
    }

    private static WallBannerBlock wallBanner(DyeColor dye, Properties properties) {
        return addValidBlock(BlockEntityTypes.BANNER, new WallBannerBlock(dye, properties));
    }

    private static BedBlock bed(DyeColor color, Properties properties) {
        return new BedBlock(color, properties
                .mapColor(state -> state.getValue(BedBlock.PART) == BedPart.FOOT ? color.getMapColor() : MapColor.WOOL)
                .sound(SoundType.WOOD)
                .strength(0.2F)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
        );
    }

    private static ShulkerBoxBlock shulkerBox(DyeColor color, Properties properties) {
        BlockBehaviour.StatePredicate statePredicate = (blockState, blockGetter, blockPos) -> {
            BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
            if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity)
                return shulkerBoxBlockEntity.isClosed();
            else return true;
        };
        return addValidBlock(BlockEntityTypes.SHULKER_BOX, new ShulkerBoxBlock(color, properties
                .forceSolidOn()
                .strength(2.0F)
                .dynamicShape()
                .noOcclusion()
                .isSuffocating(statePredicate)
                .isViewBlocking(statePredicate)
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor(Blocks::always)
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block> T addValidBlock(BlockEntityType<?> blockEntityType, T block) {
        ((FabricBlockEntityType) blockEntityType).addValidBlock(block);
        return block;
    }

    private static <T extends Block> Holder<T> register(String id, Function<Properties, T> block) {
        return register(id, block, Properties::of);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block> Holder<T> register(String id, Function<Properties, T> block, Supplier<Properties> properties) {
        var key = DyeDepot.key(Registries.BLOCK, id);
        return (Holder<T>) Registry.registerForHolder(BuiltInRegistries.BLOCK, key, block.apply(properties.get().setId(key)));
    }

    private static <T extends Block> Holder<T> registerWithItem(String id, Function<Properties, T> block) {
        return registerWithItem(id, block, Properties::of, UnaryOperator.identity());
    }

    private static <T extends Block> Holder<T> registerWithItem(String id, Function<Properties, T> block, Supplier<Properties> blockProperties) {
        return registerWithItem(id, block, blockProperties, UnaryOperator.identity());
    }

    private static <T extends Block> Holder<T> registerWithItem(
            String id,
            Function<Properties, T> block,
            Supplier<Properties> blockProperties,
            UnaryOperator<Item.Properties> itemProperties
    ) {
        var holder = register(id, block, blockProperties);
        var itemKey = DyeDepot.key(Registries.ITEM, id);
        var properties = itemProperties.apply(new Item.Properties()).useBlockDescriptionPrefix().setId(itemKey);
        var item = new BlockItem(holder.value(), properties);
        item.registerBlocks(Item.BY_BLOCK, item);
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return holder;
    }
}
