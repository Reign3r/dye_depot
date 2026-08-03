package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.block.DyeBasketBlock;
import com.ninni.dye_depot.registry.DDBlocks;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DDPolymerBlocks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Dye Depot/Polymer Blocks");
    private static final DyeColor DONOR_COLOR = DyeColor.ORANGE;
    private static final DyeColor SECONDARY_DONOR_COLOR = DyeColor.BROWN;
    private static final Map<Block, StateOverlay> OVERLAYS = new HashMap<>();
    private static final Set<BlockState> VIRTUAL_CARRIERS = new HashSet<>();
    private static final Set<String> COSMETIC_FALLBACKS = new LinkedHashSet<>();
    private static final Map<Integer, List<Vec3>> CANDLE_PARTICLE_OFFSETS = Map.of(
            1, List.of(sixteenths(8, 8, 8)),
            2, List.of(sixteenths(6, 7, 8), sixteenths(10, 8, 7)),
            3, List.of(sixteenths(8, 5, 10), sixteenths(6, 7, 8), sixteenths(9, 8, 7)),
            4, List.of(sixteenths(7, 5, 9), sixteenths(10, 7, 9), sixteenths(6, 7, 6), sixteenths(9, 8, 6))
    );
    private static final List<Vec3> CANDLE_CAKE_PARTICLE_OFFSETS = List.of(sixteenths(8, 16, 8));
    private static int mappedStateCount;

    private DDPolymerBlocks() {
    }

    static void register() {
        registerDonorDisplays();
        DDBlocks.WOOL.forEach((color, holder) -> registerSingle(holder.value(), BlockModelType.FULL_BLOCK, Blocks.WOOL.pick(DDPolymerColors.vanillaColor(color))));
        DDBlocks.CARPETS.forEach((color, holder) -> registerCarpet(holder.value(), color));
        DDBlocks.TERRACOTTA.forEach((color, holder) -> registerSingle(holder.value(), BlockModelType.FULL_BLOCK, Blocks.DYED_TERRACOTTA.pick(DDPolymerColors.vanillaColor(color))));
        DDBlocks.CONCRETE.forEach((color, holder) -> registerSingle(holder.value(), BlockModelType.FULL_BLOCK, Blocks.CONCRETE.pick(DDPolymerColors.vanillaColor(color))));
        DDBlocks.CONCRETE_POWDER.forEach((color, holder) -> registerSingle(holder.value(), BlockModelType.FULL_BLOCK, Blocks.CONCRETE_POWDER.pick(DDPolymerColors.vanillaColor(color))));
        DDBlocks.STAINED_GLASS.forEach((color, holder) -> registerSingle(holder.value(), BlockModelType.LEAVES, Blocks.STAINED_GLASS.pick(DDPolymerColors.vanillaColor(color))));

        DDBlocks.GLAZED_TERRACOTTA.forEach((color, holder) -> registerGlazed(holder.value(), color));
        DDBlocks.DYE_BASKETS.forEach((color, holder) -> registerBasket(holder.value(), color));
        DDBlocks.CANDLES.forEach((color, holder) -> registerCandle(holder.value(), color));
        DDBlocks.CANDLE_CAKES.forEach((color, holder) -> registerCandleCake(holder.value(), color));
        DDBlocks.STAINED_GLASS_PANES.forEach((color, holder) -> registerPane(holder.value(), color));
        DDBlocks.BEDS.forEach((color, holder) -> registerBed(holder.value(), color));

        DDBlocks.SHULKER_BOXES.forEach((color, holder) -> registerShulker(holder.value(), color));
        DDBlocks.BANNERS.forEach((color, holder) -> registerBanner(holder.value(), color));
        DDBlocks.WALL_BANNERS.forEach((color, holder) -> registerWallBanner(holder.value(), color));

        long expected = BuiltInRegistries.BLOCK.stream()
                .filter(block -> DyeDepot.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
                .count();
        if (OVERLAYS.size() != expected) {
            throw new IllegalStateException("Missing Dye Depot Polymer block overlays: " + OVERLAYS.size() + "/" + expected);
        }
        if (!COSMETIC_FALLBACKS.isEmpty()) {
            LOGGER.warn(
                    "Dye Depot started with {} Polymer carrier fallback(s); behavior remains available but these models use the nearest vanilla color: {}",
                    COSMETIC_FALLBACKS.size(),
                    String.join(", ", COSMETIC_FALLBACKS)
            );
        }
    }

    private static void registerSingle(Block block, BlockModelType type, Block breakBlock) {
        BlockState fallback = breakBlock.defaultBlockState();
        BlockState model = requestOrFallback(type, model(block), fallback, block);
        register(block, ignored -> model, state -> copySharedProperties(state, fallback));
    }

    private static void registerCarpet(Block block, DyeColor color) {
        BlockState donor = donorCarpet().defaultBlockState();
        registerVirtual(block,
                state -> donor,
                state -> donor,
                state -> displayStack(block.asItem(), "polymer/" + color.getName() + "_carpet"),
                state -> 0.0f,
                state -> false,
                state -> List.of(),
                null,
                null
        );
    }

    private static void registerGlazed(Block block, DyeColor color) {
        Map<Direction, BlockState> states = new HashMap<>();
        BlockState nearest = Blocks.GLAZED_TERRACOTTA.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        register(block, state -> states.computeIfAbsent(state.getValue(GlazedTerracottaBlock.FACING), direction ->
                        requestOrFallback(
                                BlockModelType.FULL_BLOCK,
                                model(block, yRotation(direction)),
                                copySharedProperties(state, nearest),
                                block
                        )),
                state -> copySharedProperties(state, nearest));
    }

    private static void registerBasket(Block block, DyeColor color) {
        Map<Direction, BlockState> states = new HashMap<>();
        BlockState nearest = Blocks.WOOL.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        register(block, state -> states.computeIfAbsent(state.getValue(DyeBasketBlock.FACING), direction ->
                        requestOrFallback(BlockModelType.FULL_BLOCK, model(block, yRotation(direction)), nearest, block)),
                state -> nearest);
    }

    private static void registerCandle(Block block, DyeColor color) {
        BlockState donor = donorCandle().defaultBlockState();
        registerVirtual(block,
                state -> copySharedProperties(state, donor),
                state -> copySharedProperties(state, donor),
                state -> displayStack(block.asItem(), "polymer/" + color.getName() + "_candle_" + state.getValue(CandleBlock.CANDLES) + (state.getValue(CandleBlock.LIT) ? "_lit" : "")),
                state -> 0.0f,
                state -> false,
                state -> List.of(),
                null,
                null
        );
    }

    private static void registerDonorDisplays() {
        Block carpet = donorCarpet();
        registerDisplayOnly(
                carpet,
                state -> displayStack(carpet.asItem(), "polymer/donor_orange_carpet")
        );

        Block candle = donorCandle();
        registerDisplayOnly(
                candle,
                state -> displayStack(
                        candle.asItem(),
                        "polymer/donor_orange_candle_" + state.getValue(CandleBlock.CANDLES)
                                + (state.getValue(CandleBlock.LIT) ? "_lit" : "")
                )
        );

        Block pane = donorPane();
        registerDisplayOnly(
                pane,
                state -> displayStack(pane.asItem(), "polymer/donor_brown_pane_" + paneMask(state)),
                state -> 180.0f,
                null
        );

        Block shulker = donorShulker();
        registerDisplayOnly(
                shulker,
                state -> new ItemStack(shulker.asItem()),
                state -> 0.0f,
                "donor_brown"
        );
    }

    private static void registerDisplayOnly(Block block, Function<BlockState, ItemStack> stack) {
        registerDisplayOnly(block, stack, null);
    }

    private static void registerDisplayOnly(Block block, Function<BlockState, ItemStack> stack, String shulkerColor) {
        registerDisplayOnly(block, stack, state -> 0.0f, shulkerColor);
    }

    private static void registerDisplayOnly(
            Block block,
            Function<BlockState, ItemStack> stack,
            Function<BlockState, Float> yaw,
            String shulkerColor
    ) {
        if (!BlockWithElementHolder.registerOverlay(
                block,
                new DisplayOverlay(stack, yaw, state -> false, state -> List.of(), null, shulkerColor)
        )) {
            throw new IllegalStateException("Could not reserve donor display for " + BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    private static Block donorCarpet() {
        return Blocks.CARPET.pick(DONOR_COLOR);
    }

    private static Block donorCandle() {
        return Blocks.DYED_CANDLE.pick(DONOR_COLOR);
    }

    private static Block donorPane() {
        return Blocks.STAINED_GLASS_PANE.pick(SECONDARY_DONOR_COLOR);
    }

    private static Block donorShulker() {
        return Blocks.DYED_SHULKER_BOX.pick(SECONDARY_DONOR_COLOR);
    }

    private static void registerCandleCake(Block block, DyeColor color) {
        Item displayItem = DDBlocks.CANDLES.getOrThrow(color).asItem();
        BlockState nearest = Blocks.DYED_CANDLE_CAKE.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        registerVirtual(block,
                state -> emptyOrFallback(BlockModelType.SLAB_BOTTOM, copySharedProperties(state, nearest), block),
                state -> copySharedProperties(state, nearest),
                state -> displayStack(displayItem, "polymer/" + color.getName() + "_candle_cake" + (state.getValue(CandleCakeBlock.LIT) ? "_lit" : "")),
                state -> 0.0f,
                state -> state.getValue(CandleCakeBlock.LIT),
                DDPolymerBlocks::candleParticleOffsets,
                null,
                null
        );
    }

    private static void registerBed(Block block, DyeColor color) {
        BlockState nearest = Blocks.BED.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        registerVirtual(block,
                state -> emptyOrFallback(
                        BlockModelType.getBed(state.getValue(BedBlock.FACING), state.getValue(BedBlock.PART)),
                        copySharedProperties(state, nearest),
                        block
                ),
                state -> copySharedProperties(state, nearest),
                state -> displayStack(block.asItem(), "polymer/" + color.getName() + "_bed_" + state.getValue(BedBlock.PART).getSerializedName()),
                state -> state.getValue(BedBlock.FACING).toYRot(),
                state -> false,
                state -> List.of(),
                null,
                null
        );
    }

    private static void registerPane(Block block, DyeColor color) {
        BlockState donor = donorPane().defaultBlockState();
        registerVirtual(block,
                state -> copySharedProperties(state, donor),
                state -> copySharedProperties(state, donor),
                state -> displayStack(block.asItem(), "polymer/" + color.getName() + "_pane_" + paneMask(state)),
                state -> 180.0f,
                state -> false,
                state -> List.of(),
                null,
                null
        );
    }

    private static int paneMask(BlockState state) {
        int mask = 0;
        if (state.getValue(StainedGlassPaneBlock.NORTH)) mask |= 1;
        if (state.getValue(StainedGlassPaneBlock.EAST)) mask |= 2;
        if (state.getValue(StainedGlassPaneBlock.SOUTH)) mask |= 4;
        if (state.getValue(StainedGlassPaneBlock.WEST)) mask |= 8;
        return mask;
    }

    static List<Vec3> candleParticleOffsets(BlockState state) {
        if (state.getBlock() instanceof CandleCakeBlock) {
            return CANDLE_CAKE_PARTICLE_OFFSETS;
        }
        if (state.getBlock() instanceof CandleBlock) {
            return CANDLE_PARTICLE_OFFSETS.getOrDefault(state.getValue(CandleBlock.CANDLES), List.of());
        }
        return List.of();
    }

    private static Vec3 sixteenths(double x, double y, double z) {
        return new Vec3(x / 16.0, y / 16.0, z / 16.0);
    }

    private static void registerShulker(Block block, DyeColor color) {
        BlockState donor = donorShulker().defaultBlockState();
        registerVirtual(block,
                state -> copySharedProperties(state, donor),
                state -> copySharedProperties(state, donor),
                state -> new ItemStack(block.asItem()),
                state -> state.getValue(ShulkerBoxBlock.FACING).toYRot(),
                state -> false,
                state -> List.of(),
                null,
                color.getName()
        );
    }

    private static void registerBanner(Block block, DyeColor color) {
        BlockState nearest = Blocks.BANNER.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        register(block,
                state -> copySharedProperties(state, nearest),
                state -> copySharedProperties(state, nearest)
        );
    }

    private static void registerWallBanner(Block block, DyeColor color) {
        BlockState nearest = Blocks.WALL_BANNER.pick(DDPolymerColors.vanillaColor(color)).defaultBlockState();
        register(block,
                state -> copySharedProperties(state, nearest),
                state -> copySharedProperties(state, nearest)
        );
    }

    private static void registerVirtual(
            Block block,
            Function<BlockState, BlockState> visual,
            Function<BlockState, BlockState> breakState,
            Function<BlockState, ItemStack> stack,
            Function<BlockState, Float> yaw,
            Function<BlockState, Boolean> lit,
            Function<BlockState, List<Vec3>> particleOffsets,
            String patternedBannerModel,
            String shulkerColor
    ) {
        register(block, visual, breakState);
        if (!BlockWithElementHolder.registerOverlay(
                block,
                new DisplayOverlay(stack, yaw, lit, particleOffsets, patternedBannerModel, shulkerColor)
        )) {
            throw new IllegalStateException("Could not register virtual display for " + BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    private static void register(Block block, Function<BlockState, BlockState> visual, Function<BlockState, BlockState> breakState) {
        Map<BlockState, BlockState> states = new HashMap<>();
        Map<BlockState, BlockState> breaks = new HashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            states.put(state, visual.apply(state));
            breaks.put(state, breakState.apply(state));
        }
        StateOverlay overlay = new StateOverlay(states, breaks);
        OVERLAYS.put(block, overlay);
        mappedStateCount += states.size();
        PolymerBlock.registerOverlay(block, overlay);
    }

    private static BlockState requestOrFallback(BlockModelType type, PolymerBlockModel model, BlockState fallback, Block source) {
        BlockState state = PolymerBlockResourceUtils.requestBlock(type, model);
        if (state == null) {
            recordFallback(source, type);
            return fallback;
        }
        return state;
    }

    private static BlockState emptyOrFallback(BlockModelType type, BlockState fallback, Block source) {
        try {
            BlockState state = PolymerBlockResourceUtils.requestEmpty(type);
            VIRTUAL_CARRIERS.add(state);
            return state;
        } catch (RuntimeException exception) {
            recordFallback(source, type);
            return fallback;
        }
    }

    private static void recordFallback(Block source, BlockModelType type) {
        COSMETIC_FALLBACKS.add(BuiltInRegistries.BLOCK.getKey(source) + "[" + type + "]");
    }

    private static PolymerBlockModel model(Block block) {
        return PolymerBlockModel.of(BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/"));
    }

    private static PolymerBlockModel model(Block block, int y) {
        return PolymerBlockModel.of(BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/"), 0, y);
    }

    private static ItemStack displayStack(Item item, String model) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.ITEM_MODEL, DyeDepot.modLoc(model));
        return stack;
    }

    private static int yRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> throw new IllegalArgumentException("Not horizontal: " + direction);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copySharedProperties(BlockState source, BlockState target) {
        for (Property property : source.getProperties()) {
            if (target.hasProperty(property)) {
                target = target.setValue(property, source.getValue(property));
            }
        }
        return target;
    }

    public static int registeredBlockCount() {
        return OVERLAYS.size();
    }

    public static int mappedStateCount() {
        return mappedStateCount;
    }

    public static BlockState polymerState(BlockState state) {
        StateOverlay overlay = OVERLAYS.get(state.getBlock());
        return overlay == null ? state : overlay.states.get(state);
    }

    public static int virtualCarrierCount() {
        return VIRTUAL_CARRIERS.size();
    }

    public static int cosmeticFallbackCount() {
        return COSMETIC_FALLBACKS.size();
    }

    private record StateOverlay(Map<BlockState, BlockState> states, Map<BlockState, BlockState> breakStates) implements PolymerTexturedBlock {
        @Override
        public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return states.get(state);
        }

        @Override
        public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
            return breakStates.get(state);
        }
    }

    private record DisplayOverlay(
            Function<BlockState, ItemStack> stack,
            Function<BlockState, Float> yaw,
            Function<BlockState, Boolean> lit,
            Function<BlockState, List<Vec3>> particleOffsets,
            String patternedBannerModel,
            String shulkerColor
    ) implements BlockWithElementHolder {
        @Override
        public ElementHolder createElementHolder(ServerLevel level, BlockPos pos, BlockState initialState) {
            return new StateDisplayHolder(
                    level,
                    pos,
                    initialState,
                    stack,
                    yaw,
                    lit,
                    particleOffsets,
                    patternedBannerModel,
                    shulkerColor
            );
        }

        @Override
        public boolean tickElementHolder(ServerLevel level, BlockPos pos, BlockState state) {
            // BlockBoundAttachment fixes auto-tick at creation time. Candle
            // holders therefore stay tick-enabled even when initially unlit;
            // onTick performs the cheap LIT early-return until they are lit.
            return patternedBannerModel != null || shulkerColor != null || !particleOffsets.apply(state).isEmpty();
        }
    }

    public static final class StateDisplayHolder extends ElementHolder {
        private final ServerLevel level;
        private final BlockPos pos;
        private final Function<BlockState, ItemStack> stack;
        private final Function<BlockState, Float> yaw;
        private final Function<BlockState, Boolean> lit;
        private final Function<BlockState, List<Vec3>> particleOffsets;
        private final String patternedBannerModel;
        private final String shulkerColor;
        private final ItemDisplayElement display = new ItemDisplayElement();
        private BlockState state;
        private BannerPatternLayers displayedBannerPatterns = BannerPatternLayers.EMPTY;
        private final ItemDisplayElement shulkerLid;
        private float displayedShulkerProgress = -1.0f;

        private StateDisplayHolder(
                ServerLevel level,
                BlockPos pos,
                BlockState initialState,
                Function<BlockState, ItemStack> stack,
                Function<BlockState, Float> yaw,
                Function<BlockState, Boolean> lit,
                Function<BlockState, List<Vec3>> particleOffsets,
                String patternedBannerModel,
                String shulkerColor
        ) {
            this.level = level;
            this.pos = pos.immutable();
            this.stack = stack;
            this.yaw = yaw;
            this.lit = lit;
            this.particleOffsets = particleOffsets;
            this.patternedBannerModel = patternedBannerModel;
            this.shulkerColor = shulkerColor;
            display.setItemDisplayContext(ItemDisplayContext.NONE);
            display.setDisplaySize(1.0f, 1.0f);
            display.setScale(new Vector3f(1.0f));
            if (shulkerColor != null) {
                shulkerLid = new ItemDisplayElement();
                shulkerLid.setItemDisplayContext(ItemDisplayContext.NONE);
                shulkerLid.setDisplaySize(1.0f, 1.0f);
                shulkerLid.setScale(new Vector3f(1.0f));
                shulkerLid.setInterpolationDuration(1);
            } else {
                shulkerLid = null;
            }
            update(initialState);
            addElement(display);
            if (shulkerLid != null) {
                addElement(shulkerLid);
            }
        }

        private void update(BlockState state) {
            this.state = state;
            if (patternedBannerModel != null) {
                displayedBannerPatterns = readBannerPatterns();
            }
            if (shulkerColor != null) {
                displayedShulkerProgress = readShulkerProgress();
            }
            rebuildItem();
            if (shulkerColor != null) {
                display.setYaw(0.0f);
                updateShulkerTransforms(false);
            } else {
                display.setYaw(yaw.apply(state));
            }
            // Opaque full-block carriers make a display at their center sample
            // zero light. Only shulkers use that carrier; partial displays keep
            // normal world lighting so candle light transitions stay natural.
            display.setBrightness(shulkerColor != null ? surroundingBrightness() : null);
            if (shulkerLid != null) {
                shulkerLid.setBrightness(display.getBrightness());
            }
        }

        private void rebuildItem() {
            ItemStack next = stack.apply(state);
            if (patternedBannerModel != null && !displayedBannerPatterns.layers().isEmpty()) {
                next.set(DataComponents.BANNER_PATTERNS, displayedBannerPatterns);
                next.set(DataComponents.ITEM_MODEL, DyeDepot.modLoc(patternedBannerModel));
            }
            if (shulkerColor != null) {
                next.set(DataComponents.ITEM_MODEL, DyeDepot.modLoc("polymer/" + shulkerColor + "_shulker_base"));
            }
            if (!ItemStack.isSameItemSameComponents(display.getItem(), next)) {
                display.setItem(next);
            }
            if (shulkerLid != null) {
                ItemStack lid = stack.apply(state);
                lid.set(DataComponents.ITEM_MODEL, DyeDepot.modLoc("polymer/" + shulkerColor + "_shulker_lid"));
                if (!ItemStack.isSameItemSameComponents(shulkerLid.getItem(), lid)) {
                    shulkerLid.setItem(lid);
                }
            }
        }

        private BannerPatternLayers readBannerPatterns() {
            return loadedBannerPatterns(level, pos);
        }

        static BannerPatternLayers loadedBannerPatterns(ServerLevel level, BlockPos pos) {
            // Element holders are also constructed while a chunk is being
            // deserialized. Asking the level for its block entity here would
            // synchronously request that same chunk and self-wait forever.
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
                return banner.getPatterns();
            }
            return BannerPatternLayers.EMPTY;
        }

        private void refreshBannerPatterns() {
            BannerPatternLayers next = readBannerPatterns();
            if (!next.equals(displayedBannerPatterns)) {
                displayedBannerPatterns = next;
                rebuildItem();
            }
        }

        private float readShulkerProgress() {
            if (level == null) {
                return 0.0f;
            }
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulker) {
                return Math.max(0.0f, Math.min(1.0f, shulker.getProgress(1.0f)));
            }
            return 0.0f;
        }

        private void refreshShulker() {
            float next = readShulkerProgress();
            if (Float.compare(next, displayedShulkerProgress) != 0) {
                displayedShulkerProgress = next;
                updateShulkerTransforms(true);
                display.setBrightness(surroundingBrightness());
                shulkerLid.setBrightness(display.getBrightness());
            }
        }

        private void updateShulkerTransforms(boolean interpolate) {
            Quaternionf facing = new Quaternionf(state.getValue(ShulkerBoxBlock.FACING).getRotation());
            display.setLeftRotation(facing);

            Vector3f translation = new Vector3f(0.0f, displayedShulkerProgress * 0.5f, 0.0f).rotate(facing);
            Quaternionf lidRotation = new Quaternionf(facing)
                    .rotateY((float) (Math.PI * 1.5) * displayedShulkerProgress);
            shulkerLid.setTranslation(translation);
            shulkerLid.setLeftRotation(lidRotation);
            if (interpolate) {
                shulkerLid.startInterpolation();
            }
        }

        private Brightness surroundingBrightness() {
            if (level == null) {
                return null;
            }
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            int sky = level.getBrightness(LightLayer.SKY, pos);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                block = Math.max(block, level.getBrightness(LightLayer.BLOCK, neighbor));
                sky = Math.max(sky, level.getBrightness(LightLayer.SKY, neighbor));
            }
            return new Brightness(block, sky);
        }

        @Override
        protected void onTick() {
            super.onTick();
            if (getWatchingPlayers().isEmpty()) {
                return;
            }
            if (patternedBannerModel != null) {
                refreshBannerPatterns();
            }
            if (shulkerColor != null) {
                refreshShulker();
            }
            if (!lit.apply(state) || particleOffsets.apply(state).isEmpty()) {
                return;
            }

            BlockBoundAttachment attachment = BlockBoundAttachment.get(this);
            if (attachment == null) {
                return;
            }
            ServerLevel world = attachment.getWorld();
            BlockPos blockPos = attachment.getBlockPos();
            for (Vec3 offset : particleOffsets.apply(state)) {
                emitCandleParticles(world, blockPos, offset);
            }
        }

        private static void emitCandleParticles(ServerLevel level, BlockPos pos, Vec3 offset) {
            double x = pos.getX() + offset.x;
            double y = pos.getY() + offset.y;
            double z = pos.getZ() + offset.z;
            float chance = level.getRandom().nextFloat();
            if (chance < 0.3f) {
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0, 0, 0);
                if (chance < 0.17f) {
                    level.playSound(
                            null,
                            x + 0.5,
                            y + 0.5,
                            z + 0.5,
                            SoundEvents.CANDLE_AMBIENT,
                            SoundSource.BLOCKS,
                            1.0f + level.getRandom().nextFloat(),
                            level.getRandom().nextFloat() * 0.7f + 0.3f
                    );
                }
            }
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0, 0, 0, 0);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                BlockBoundAttachment attachment = BlockBoundAttachment.get(this);
                if (attachment != null) {
                    update(attachment.getBlockState());
                    display.tick();
                }
            }
            super.notifyUpdate(updateType);
        }
    }
}
