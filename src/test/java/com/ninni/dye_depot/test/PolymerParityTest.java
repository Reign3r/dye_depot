package com.ninni.dye_depot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.polymer.DDPolymerBlockEntityNbt;
import com.ninni.dye_depot.polymer.DDPolymerBlocks;
import com.ninni.dye_depot.polymer.DDPolymerColors;
import com.ninni.dye_depot.polymer.DDPolymerCreativeTab;
import com.ninni.dye_depot.polymer.DDPolymerEntities;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import com.ninni.dye_depot.registry.DDMapDecorationType;
import com.ninni.dye_depot.registry.DDParticles;
import com.ninni.dye_depot.registry.DDPoiTypes;
import com.ninni.dye_depot.registry.DDSoundEvents;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.BlockMapper;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PolymerParticleType;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolymerParityTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void initializeMinecraftAndMod() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!BuiltInRegistries.ITEM.containsKey(DyeDepot.modLoc("maroon_dye"))) {
            new DyeDepot().onInitialize();
        }
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                .forEach(pending -> pending.apply());
    }

    @Test
    void metadataDeclaresAServerOnlyPolymerModWithoutClientEntrypointsOrMixins() {
        JsonObject metadata = ResourceTestSupport.json("fabric.mod.json");
        assertEquals("server", metadata.get("environment").getAsString());
        assertFalse(metadata.getAsJsonObject("entrypoints").has("client"));
        JsonObject dependencies = metadata.getAsJsonObject("depends");
        for (String dependency : List.of(
                "polymer-core", "polymer-blocks", "polymer-resource-pack",
                "polymer-resource-pack-extras", "polymer-virtual-entity"
        )) {
            assertTrue(dependencies.has(dependency), dependency);
        }

        JsonObject mixins = ResourceTestSupport.json("dye_depot.mixins.json");
        assertFalse(mixins.has("client"));
        assertTrue(mixins.getAsJsonArray("mixins").asList().stream()
                .noneMatch(entry -> entry.getAsString().startsWith("client.")));
    }

    @Test
    void vanillaEntityPacketOverlaysDoNotRemoveVanillaRegistryEntries() {
        for (EntityType<?> type : List.of(EntityTypes.SHEEP, EntityTypes.CAT, EntityTypes.WOLF)) {
            assertNotNull(
                    PolymerEntityUtils.getPolymerEntityConstructor(type),
                    () -> BuiltInRegistries.ENTITY_TYPE.getKey(type) + " packet overlay"
            );
            assertFalse(
                    PolymerEntityUtils.isPolymerEntityType(type),
                    () -> BuiltInRegistries.ENTITY_TYPE.getKey(type) + " must remain client-visible"
            );
        }
    }

    @Test
    void everyCustomItemAndBlockHasAnOutboundOverlayAndEveryStateMapsToVanilla() {
        long itemCount = BuiltInRegistries.ITEM.stream()
                .filter(item -> isDyeDepot(BuiltInRegistries.ITEM.getKey(item)))
                .peek(item -> assertInstanceOf(
                        PolymerItem.class,
                        PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, item),
                        BuiltInRegistries.ITEM.getKey(item).toString()
                ))
                .count();
        assertEquals(240, itemCount);

        int expectedStates = 0;
        int blockCount = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!isDyeDepot(BuiltInRegistries.BLOCK.getKey(block))) {
                continue;
            }
            blockCount++;
            expectedStates += block.getStateDefinition().getPossibleStates().size();
            assertInstanceOf(
                    PolymerTexturedBlock.class,
                    PolymerSyncedObject.getSyncedObject(BuiltInRegistries.BLOCK, block),
                    BuiltInRegistries.BLOCK.getKey(block).toString()
            );
            block.getStateDefinition().getPossibleStates().forEach(state -> {
                var replacement = DDPolymerBlocks.polymerState(state);
                assertNotNull(replacement, state.toString());
                assertEquals("minecraft", BuiltInRegistries.BLOCK.getKey(replacement.getBlock()).getNamespace(), state.toString());
            });
        }
        assertEquals(256, blockCount);
        assertEquals(blockCount, DDPolymerBlocks.registeredBlockCount());
        assertEquals(expectedStates, DDPolymerBlocks.mappedStateCount());
    }

    @Test
    void defaultPolymerMapperPreservesEveryRequestedTexturedCarrier() {
        PacketContext context = new Connection(PacketFlow.CLIENTBOUND).getPacketContext();
        BlockMapper mapper = BlockMapper.getDefault(context);

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!isDyeDepot(BuiltInRegistries.BLOCK.getKey(block))) {
                continue;
            }
            block.getStateDefinition().getPossibleStates().forEach(state -> assertSame(
                    DDPolymerBlocks.polymerState(state),
                    mapper.toClientSideState(state, context),
                    state.toString()
            ));
        }
    }

    @Test
    void dyesBannersAndShulkersUseFamilySafePerColorVanillaCarriers() {
        PacketContext context = new Connection(PacketFlow.CLIENTBOUND).getPacketContext();

        for (DDDyes entry : DDDyes.values()) {
            DyeColor custom = entry.get();
            DyeColor safe = DDPolymerColors.vanillaColor(custom);

            Item customDye = DDItems.DYES.getOrThrow(custom);
            PolymerItem dyeOverlay = assertInstanceOf(
                    PolymerItem.class,
                    PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customDye)
            );
            Item dyeCarrier = dyeOverlay.getPolymerItem(new ItemStack(customDye), context);
            assertInstanceOf(DyeItem.class, dyeCarrier, custom.getName() + " dye carrier");
            assertSame(Items.DYE.pick(safe), dyeCarrier, custom.getName() + " dye carrier color");
            assertEquals(safe, dyeCarrier.components().get(DataComponents.DYE), custom.getName() + " safe dye component");

            Item customBanner = DDItems.BANNERS.getOrThrow(custom);
            PolymerItem bannerOverlay = assertInstanceOf(
                    PolymerItem.class,
                    PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customBanner)
            );
            Item bannerCarrier = bannerOverlay.getPolymerItem(new ItemStack(customBanner), context);
            BannerItem vanillaBanner = assertInstanceOf(BannerItem.class, bannerCarrier, custom.getName() + " banner carrier");
            assertSame(Items.BANNER.pick(safe), bannerCarrier, custom.getName() + " banner carrier color");
            assertEquals(safe, vanillaBanner.getColor(), custom.getName() + " banner carrier base color");

            Item customShulker = DDItems.SHULKER_BOXES.getOrThrow(custom);
            PolymerItem shulkerOverlay = assertInstanceOf(
                    PolymerItem.class,
                    PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customShulker)
            );
            Item shulkerCarrier = shulkerOverlay.getPolymerItem(new ItemStack(customShulker), context);
            assertSame(Items.DYED_SHULKER_BOX.pick(safe), shulkerCarrier, custom.getName() + " shulker carrier color");
            assertFalse(shulkerCarrier.canFitInsideContainerItems(), custom.getName() + " shulker cannot be nested");
            assertFalse(
                    new ShulkerBoxSlot(new SimpleContainer(1), 0, 0, 0).mayPlace(new ItemStack(shulkerCarrier)),
                    custom.getName() + " shulker is rejected by shulker-box slots"
            );
        }
    }

    @Test
    void thinAndPartialBlocksNeverUseAFullCubeCarrier() {
        DDBlocks.CARPETS.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.CANDLES.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.CANDLE_CAKES.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.STAINED_GLASS_PANES.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.BEDS.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.BANNERS.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
        DDBlocks.WALL_BANNERS.values().forEach(block -> assertFalse(DDPolymerBlocks.polymerState(block.defaultBlockState()).isCollisionShapeFullBlock(null, null)));
    }

    @Test
    void transparentAndThinFamiliesUseClientGeometryThatCannotExposeHiddenFaces() {
        DDBlocks.STAINED_GLASS.values().forEach(block -> assertFalse(
                DDPolymerBlocks.polymerState(block.defaultBlockState()).canOcclude(),
                block.toString()
        ));
        DDBlocks.CARPETS.values().forEach(block -> {
            var carrier = DDPolymerBlocks.polymerState(block.defaultBlockState());
            assertSame(Blocks.CARPET.pick(DyeColor.ORANGE), carrier.getBlock(), block.toString());
            var bounds = carrier.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            assertTrue(bounds.getXsize() >= 14.0 / 16.0, carrier.toString());
            assertTrue(bounds.getZsize() >= 14.0 / 16.0, carrier.toString());
            assertTrue(bounds.maxY <= 1.0 / 16.0, carrier.toString());
            assertEquals(
                    block.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                    carrier.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                    block.toString()
            );
        });
        DDBlocks.CANDLES.values().forEach(block ->
                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    var carrier = DDPolymerBlocks.polymerState(state);
                    assertSame(Blocks.DYED_CANDLE.pick(DyeColor.ORANGE), carrier.getBlock(), state.toString());
                    assertEquals(
                            state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                            carrier.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                            state.toString()
                    );
                })
        );
        assertNotNull(BlockWithElementHolder.get(Blocks.CARPET.pick(DyeColor.ORANGE).defaultBlockState()));
        assertNotNull(BlockWithElementHolder.get(Blocks.DYED_CANDLE.pick(DyeColor.ORANGE).defaultBlockState()));
        DDBlocks.STAINED_GLASS_PANES.values().forEach(block ->
                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    var carrier = DDPolymerBlocks.polymerState(state);
                    assertSame(Blocks.STAINED_GLASS_PANE.pick(DyeColor.BROWN), carrier.getBlock(), state.toString());
                    assertEquals(
                            state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                            carrier.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs(),
                            state.toString()
                    );
                })
        );
        DDBlocks.SHULKER_BOXES.values().forEach(block -> assertSame(
                Blocks.DYED_SHULKER_BOX.pick(DyeColor.BROWN),
                DDPolymerBlocks.polymerState(block.defaultBlockState()).getBlock(),
                block.toString()
        ));
        assertNotNull(BlockWithElementHolder.get(Blocks.STAINED_GLASS_PANE.pick(DyeColor.BROWN).defaultBlockState()));
        assertNotNull(BlockWithElementHolder.get(Blocks.DYED_SHULKER_BOX.pick(DyeColor.BROWN).defaultBlockState()));
    }

    @Test
    void virtualBlocksUseSharedTargetableGeometryCarriersWithoutPoolFallbacks() {
        assertEquals(10, DDPolymerBlocks.virtualCarrierCount());
        assertEquals(0, DDPolymerBlocks.cosmeticFallbackCount());

        for (var family : List.of(
                DDBlocks.CANDLES.values(),
                DDBlocks.CANDLE_CAKES.values(),
                DDBlocks.CARPETS.values(),
                DDBlocks.STAINED_GLASS_PANES.values(),
                DDBlocks.BEDS.values(),
                DDBlocks.SHULKER_BOXES.values(),
                DDBlocks.BANNERS.values(),
                DDBlocks.WALL_BANNERS.values()
        )) {
            family.forEach(block -> block.getStateDefinition().getPossibleStates().forEach(state -> {
                var carrier = DDPolymerBlocks.polymerState(state);
                assertFalse(carrier.is(Blocks.AIR), state.toString());
                assertFalse(carrier.is(Blocks.LIGHT), state.toString());
                assertFalse(carrier.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(), state.toString());
            }));
        }

        DDBlocks.STAINED_GLASS_PANES.values().forEach(block ->
                block.getStateDefinition().getPossibleStates().stream()
                        .filter(state -> !state.getValue(BlockStateProperties.WATERLOGGED))
                        .forEach(dry -> {
                            var wet = dry.setValue(BlockStateProperties.WATERLOGGED, true);
                            var dryCarrier = DDPolymerBlocks.polymerState(dry);
                            var wetCarrier = DDPolymerBlocks.polymerState(wet);
                            assertNotSame(dryCarrier, wetCarrier, dry.toString());
                            for (var property : List.of(
                                    BlockStateProperties.NORTH,
                                    BlockStateProperties.EAST,
                                    BlockStateProperties.SOUTH,
                                    BlockStateProperties.WEST,
                                    BlockStateProperties.WATERLOGGED
                            )) {
                                assertTrue(dryCarrier.hasProperty(property), dryCarrier.toString());
                                assertTrue(wetCarrier.hasProperty(property), wetCarrier.toString());
                                assertEquals(dry.getValue(property), dryCarrier.getValue(property), dry.toString());
                                assertEquals(wet.getValue(property), wetCarrier.getValue(property), wet.toString());
                            }
                            assertFalse(dryCarrier.getFluidState().is(Fluids.WATER), dry.toString());
                            assertTrue(wetCarrier.getFluidState().is(Fluids.WATER), wet.toString());
                        })
        );

        DDBlocks.CANDLES.values().forEach(block -> {
            var dry = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
            var wet = dry.setValue(BlockStateProperties.WATERLOGGED, true);
            var dryCarrier = DDPolymerBlocks.polymerState(dry);
            var wetCarrier = DDPolymerBlocks.polymerState(wet);
            assertNotSame(dryCarrier, wetCarrier, block.toString());
            assertTrue(wetCarrier.hasProperty(BlockStateProperties.WATERLOGGED), wetCarrier.toString());
            assertFalse(dryCarrier.getFluidState().is(Fluids.WATER), dry.toString());
            assertTrue(wetCarrier.getFluidState().is(Fluids.WATER), wet.toString());
        });
    }

    @Test
    void bedDisplayYawMatchesAuthoredModelsAfterVanillaRendererCompensation() {
        Map<Direction, Float> authoredRotations = Map.of(
                Direction.NORTH, 0.0f,
                Direction.EAST, 90.0f,
                Direction.SOUTH, 180.0f,
                Direction.WEST, 270.0f
        );
        Block bed = DDBlocks.BEDS.getOrThrow(DDDyes.MAROON.get());

        for (var entry : authoredRotations.entrySet()) {
            for (BedPart part : BedPart.values()) {
                var state = bed.defaultBlockState()
                        .setValue(BedBlock.FACING, entry.getKey())
                        .setValue(BedBlock.PART, part);
                var overlay = BlockWithElementHolder.get(state);
                assertNotNull(overlay);
                var holder = overlay.createElementHolder(null, BlockPos.ZERO, state);
                var display = assertInstanceOf(ItemDisplayElement.class, holder.getElements().getFirst());

                // ItemDisplayRenderer applies a 180-degree model correction and
                // DisplayRenderer applies the negative entity yaw. Compare that
                // effective transform with the authored blockstate rotation.
                assertEquals(
                        normalizeDegrees(-entry.getValue()),
                        normalizeDegrees(180.0f - display.getYaw()),
                        0.001f,
                        state.toString()
                );
                holder.destroy();
            }
        }
    }

    @Test
    void polymerCreativeTabContainsAll240ItemsExactlyOnceInBaselineOrder() {
        List<Item> items = DDPolymerCreativeTab.items();
        assertEquals(240, items.size());
        assertEquals(240, new HashSet<>(items).size());
        assertTrue(items.stream().allMatch(item -> isDyeDepot(BuiltInRegistries.ITEM.getKey(item))));
        assertEquals("maroon_dye", BuiltInRegistries.ITEM.getKey(items.getFirst()).getPath());
        assertEquals("white_dye_basket", BuiltInRegistries.ITEM.getKey(items.get(16)).getPath());
        assertEquals("indigo_banner", BuiltInRegistries.ITEM.getKey(items.getLast()).getPath());
        assertTrue(PolymerCreativeModeTabUtils.contains(DyeDepot.modLoc("items")));
    }

    @Test
    void extendedEntityAndBlockEntityColorsAreVanillaCodecSafe() {
        for (DDDyes dye : DDDyes.values()) {
            byte rawUnsheared = (byte) dye.getId();
            byte rawSheared = (byte) (dye.getId() | 32);
            byte safeUnsheared = DDPolymerEntities.vanillaSheepData(rawUnsheared);
            byte safeSheared = DDPolymerEntities.vanillaSheepData(rawSheared);
            assertTrue((safeUnsheared & 15) < 16);
            assertTrue((safeUnsheared & 16) != 0, "virtual coat suppresses nearest-color wool");
            assertTrue((safeSheared & 16) != 0);
            assertTrue(DDPolymerEntities.vanillaCollarData(dye.getId()) < 16);

            CompoundTag sign = new CompoundTag();
            CompoundTag front = new CompoundTag();
            front.putString("color", dye.getName());
            sign.put("front_text", front);
            CompoundTag back = new CompoundTag();
            back.putString("color", dye.getName());
            sign.put("back_text", back);
            ListTag patterns = new ListTag();
            CompoundTag layer = new CompoundTag();
            layer.putString("color", dye.getName());
            CompoundTag layerMetadata = new CompoundTag();
            layerMetadata.putString("color", dye.getName());
            layer.put("metadata", layerMetadata);
            patterns.add(layer);
            sign.put("patterns", patterns);
            sign.putString("color", dye.getName());
            CompoundTag foreignData = new CompoundTag();
            foreignData.putString("color", dye.getName());
            sign.put("foreign_data", foreignData);

            String vanilla = DDPolymerColors.vanillaColor(dye.get()).getName();
            for (BlockEntityType<?> signType : List.of(BlockEntityTypes.SIGN, BlockEntityTypes.HANGING_SIGN)) {
                CompoundTag safeSign = DDPolymerBlockEntityNbt.sanitize(signType, sign);
                assertNotSame(sign, safeSign);
                assertEquals(vanilla, stringField((CompoundTag) safeSign.get("front_text"), "color"));
                assertEquals(vanilla, stringField((CompoundTag) safeSign.get("back_text"), "color"));
                assertEquals(dye.getName(), patternColor(safeSign));
                assertEquals(dye.getName(), stringField(safeSign, "color"));
                assertEquals(dye.getName(), stringField((CompoundTag) safeSign.get("foreign_data"), "color"));
            }

            CompoundTag safeBanner = DDPolymerBlockEntityNbt.sanitize(BlockEntityTypes.BANNER, sign);
            assertNotSame(sign, safeBanner);
            assertEquals(vanilla, patternColor(safeBanner));
            assertEquals(dye.getName(), stringField((CompoundTag) safeBanner.get("front_text"), "color"));
            CompoundTag safeLayer = (CompoundTag) ((ListTag) safeBanner.get("patterns")).getFirst();
            assertEquals(dye.getName(), stringField((CompoundTag) safeLayer.get("metadata"), "color"));

            CompoundTag snapshot = sign.copy();
            CompoundTag unrelated = DDPolymerBlockEntityNbt.sanitize(BlockEntityTypes.SHULKER_BOX, sign);
            assertSame(sign, unrelated, "unrelated typed data must retain identity");
            assertEquals(snapshot, unrelated, "unrelated typed data must retain byte/tag equality");
            assertTrue(sign.toString().contains(dye.getName()), "sanitization must not mutate server state");
        }
    }

    @Test
    void outboundItemsSanitizeEveryDyeColorComponentAndNestedBlockEntityData() {
        DyeColor custom = DDDyes.MAROON.get();
        List<DataComponentType<DyeColor>> colorComponents = List.of(
                DataComponents.DYE,
                DataComponents.BASE_COLOR,
                DataComponents.WOLF_COLLAR,
                DataComponents.TROPICAL_FISH_BASE_COLOR,
                DataComponents.TROPICAL_FISH_PATTERN_COLOR,
                DataComponents.CAT_COLLAR,
                DataComponents.SHEEP_COLOR,
                DataComponents.SHULKER_COLOR
        );

        ItemStack nested = new ItemStack(Items.PAPER);
        colorComponents.forEach(component -> nested.set(component, custom));
        CompoundTag nestedTag = coloredTag(custom);
        nested.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.BANNER, nestedTag));

        ItemStack serverStack = new ItemStack(Blocks.DYED_SHULKER_BOX.pick(DyeColor.WHITE));
        colorComponents.forEach(component -> serverStack.set(component, custom));
        CompoundTag outerTag = coloredTag(custom);
        serverStack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.BANNER, outerTag));
        serverStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nested)));

        ItemStack clientStack = PolymerItemUtils.ITEM_MODIFICATION_EVENT.invoker().modifyItem(
                serverStack,
                serverStack.copy(),
                new Connection(PacketFlow.CLIENTBOUND).getPacketContext()
        );

        colorComponents.forEach(component -> {
            assertEquals(custom, serverStack.get(component), component.toString());
            assertTrue(clientStack.get(component).getId() < 16, component.toString());
        });
        assertTrue(serverStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId().toString().contains(custom.getName()));
        CompoundTag safeOuterTag = clientStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
        assertEquals(DDPolymerColors.vanillaColor(custom).getName(), patternColor(safeOuterTag));
        assertEquals(custom.getName(), stringField((CompoundTag) safeOuterTag.get("front_text"), "color"));

        ItemStack safeNested = clientStack.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().findFirst().orElseThrow();
        colorComponents.forEach(component -> assertTrue(safeNested.get(component).getId() < 16, "nested " + component));
        CompoundTag safeNestedTag = safeNested.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
        assertEquals(DDPolymerColors.vanillaColor(custom).getName(), patternColor(safeNestedTag));
        assertEquals(custom.getName(), stringField((CompoundTag) safeNestedTag.get("front_text"), "color"));

        ItemStack originalNested = serverStack.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().findFirst().orElseThrow();
        assertEquals(custom, originalNested.get(DataComponents.BASE_COLOR));
        assertTrue(originalNested.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId().toString().contains(custom.getName()));
    }

    @Test
    void opaqueAndUnrelatedTypedItemDataRetainsIdentityAndTagEquality() {
        CompoundTag shaped = coloredTag(DDDyes.MAROON.get());
        CompoundTag snapshot = shaped.copy();
        CustomData bucketData = CustomData.of(shaped);
        TypedEntityData<EntityType<?>> entityData = TypedEntityData.<EntityType<?>>of(EntityTypes.SHEEP, shaped);
        TypedEntityData<BlockEntityType<?>> blockEntityData = TypedEntityData.<BlockEntityType<?>>of(
                BlockEntityTypes.SHULKER_BOX,
                shaped
        );

        ItemStack serverStack = new ItemStack(Items.PAPER);
        serverStack.set(DataComponents.BUCKET_ENTITY_DATA, bucketData);
        serverStack.set(DataComponents.ENTITY_DATA, entityData);
        serverStack.set(DataComponents.BLOCK_ENTITY_DATA, blockEntityData);
        ItemStack clientStack = PolymerItemUtils.ITEM_MODIFICATION_EVENT.invoker().modifyItem(
                serverStack,
                serverStack.copy(),
                new Connection(PacketFlow.CLIENTBOUND).getPacketContext()
        );

        assertSame(bucketData, clientStack.get(DataComponents.BUCKET_ENTITY_DATA));
        assertSame(entityData, clientStack.get(DataComponents.ENTITY_DATA));
        assertSame(blockEntityData, clientStack.get(DataComponents.BLOCK_ENTITY_DATA));
        assertEquals(snapshot, clientStack.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
        assertEquals(snapshot, clientStack.get(DataComponents.ENTITY_DATA).copyTagWithoutId());
        assertEquals(snapshot, clientStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId());
    }

    @Test
    void blockEntityPacketMixinSanitizesOnlyInsideAnOutboundPacketContext() throws Exception {
        CompoundTag serverTag = coloredTag(DDDyes.MAROON.get());
        Constructor<ClientboundBlockEntityDataPacket> constructor = ClientboundBlockEntityDataPacket.class
                .getDeclaredConstructor(BlockPos.class, BlockEntityType.class, CompoundTag.class);
        constructor.setAccessible(true);
        ClientboundBlockEntityDataPacket packet = constructor.newInstance(BlockPos.ZERO, BlockEntityTypes.BANNER, serverTag);

        CompoundTag unwrapped = PacketContext.supplyWithoutContext(packet::getTag);
        assertSame(serverTag, unwrapped);

        CompoundTag outbound = PacketContext.supplyWithContext(new Connection(PacketFlow.CLIENTBOUND), packet::getTag);
        assertNotSame(serverTag, outbound);
        assertTrue(serverTag.toString().contains(DDDyes.MAROON.getName()));
        assertEquals(DDPolymerColors.vanillaColor(DDDyes.MAROON.get()).getName(), patternColor(outbound));
        assertEquals(
                DDDyes.MAROON.getName(),
                stringField((CompoundTag) outbound.get("front_text"), "color"),
                "banner packets must not rewrite sign-shaped foreign fields"
        );

        ClientboundBlockEntityDataPacket signPacket = constructor.newInstance(
                BlockPos.ZERO,
                BlockEntityTypes.SIGN,
                serverTag
        );
        CompoundTag signOutbound = PacketContext.supplyWithContext(
                new Connection(PacketFlow.CLIENTBOUND),
                signPacket::getTag
        );
        assertEquals(
                DDPolymerColors.vanillaColor(DDDyes.MAROON.get()).getName(),
                stringField((CompoundTag) signOutbound.get("front_text"), "color")
        );
        assertEquals(
                DDDyes.MAROON.getName(),
                patternColor(signOutbound),
                "sign packets must not rewrite banner-shaped foreign fields"
        );

        CompoundTag unrelatedTag = serverTag.copy();
        CompoundTag unrelatedSnapshot = unrelatedTag.copy();
        ClientboundBlockEntityDataPacket unrelatedPacket = constructor.newInstance(
                BlockPos.ZERO,
                BlockEntityTypes.SHULKER_BOX,
                unrelatedTag
        );
        CompoundTag unrelatedOutbound = PacketContext.supplyWithContext(
                new Connection(PacketFlow.CLIENTBOUND),
                unrelatedPacket::getTag
        );
        assertSame(unrelatedTag, unrelatedOutbound);
        assertEquals(unrelatedSnapshot, unrelatedOutbound);
    }

    @Test
    void particlesSoundsAndMapDecorationsHaveSafeProtocolOverlays() {
        assertNotNull(PolymerParticleType.getOverlay(DDParticles.DYE_POOF));
        assertNotNull(PolymerSyncedObject.getSyncedObject(BuiltInRegistries.SOUND_EVENT, DDSoundEvents.DYE_BASKET_POOF));
        DDMapDecorationType.BANNERS.forEach((color, holder) -> {
            var overlay = PolymerSyncedObject.getSyncedObject(BuiltInRegistries.MAP_DECORATION_TYPE, holder.value());
            assertNotNull(overlay, color.getName());
            MapDecorationType replacement = overlay.getPolymerReplacement(holder.value(), null);
            assertEquals("minecraft", BuiltInRegistries.MAP_DECORATION_TYPE.getKey(replacement).getNamespace());
        });
    }

    @Test
    void customHomePoiRemainsServerOnlyForVanillaRegistrySync() {
        assertTrue(RegistrySyncUtils.isServerEntry(
                BuiltInRegistries.POINT_OF_INTEREST_TYPE,
                DDPoiTypes.HOME.identifier()
        ));
    }

    @Test
    void generatedPackContainsSafeBannerDefinitionsAndEveryVirtualModel() throws Exception {
        assertTrue(PolymerResourcePackUtils.isRequired());
        Path output = temporaryDirectory.resolve("dye-depot-polymer.zip");
        var result = PolymerResourcePackUtils.getInstance().build(output);
        assertEquals(output.toAbsolutePath(), result.path());
        assertFalse(result.hadIssues());
        assertTrue(Files.size(output) > 0);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertNotNull(zip.getEntry("pack.mcmeta"));
            assertNotNull(zip.getEntry("assets/minecraft/textures/item/yellow_dye.png"), "Sky/Ash override pack must be merged");
            JsonObject english = JsonParser.parseString(read(zip, "assets/dye_depot/lang/en_us.json")).getAsJsonObject();
            assertEquals("Sky Dye Basket", english.get("block.dye_depot.light_blue_dye_basket").getAsString());
            assertEquals("Ash Dye Basket", english.get("block.dye_depot.light_gray_dye_basket").getAsString());

            String donorEmpty = "dye_depot:block/polymer/donor_empty";
            assertNotNull(zip.getEntry("assets/dye_depot/models/block/polymer/donor_empty.json"));
            JsonObject donorCarpet = JsonParser.parseString(read(zip, "assets/minecraft/blockstates/orange_carpet.json")).getAsJsonObject();
            assertEquals(donorEmpty, donorCarpet.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
            JsonObject donorCandles = JsonParser.parseString(read(zip, "assets/minecraft/blockstates/orange_candle.json")).getAsJsonObject();
            assertEquals(8, donorCandles.getAsJsonObject("variants").size());
            donorCandles.getAsJsonObject("variants").entrySet().forEach(entry ->
                    assertEquals(donorEmpty, entry.getValue().getAsJsonObject().get("model").getAsString(), entry.getKey())
            );
            assertTrue(read(zip, "assets/dye_depot/items/polymer/donor_orange_carpet.json").contains("minecraft:block/orange_carpet"));
            for (int count = 1; count <= 4; count++) {
                assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_orange_candle_" + count + ".json"));
                assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_orange_candle_" + count + "_lit.json"));
            }
            assertEquals(0, JsonParser.parseString(read(zip, "assets/minecraft/blockstates/brown_stained_glass_pane.json"))
                    .getAsJsonObject().getAsJsonArray("multipart").size());
            assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_brown_pane_0.json"));
            assertNotNull(zip.getEntry("assets/dye_depot/models/block/polymer/donor_brown_pane_0.json"));

            var hiddenShulker = ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/entity/shulker/shulker_brown.png")));
            assertEquals(64, hiddenShulker.getWidth());
            assertEquals(64, hiddenShulker.getHeight());
            for (int y = 0; y < hiddenShulker.getHeight(); y++) {
                for (int x = 0; x < hiddenShulker.getWidth(); x++) {
                    assertEquals(0, hiddenShulker.getRGB(x, y) >>> 24, "native brown donor texture must be transparent");
                }
            }
            var restoredShulker = ImageIO.read(zip.getInputStream(zip.getEntry("assets/dye_depot/textures/entity/shulker/donor_brown.png")));
            assertEquals(64, restoredShulker.getWidth());
            assertEquals(64, restoredShulker.getHeight());
            assertTrue(hasVisiblePixel(restoredShulker), "restored brown shulker texture must remain visible");
            assertTrue(read(zip, "assets/minecraft/items/brown_shulker_box.json").contains("dye_depot:donor_brown"));
            for (int step = 0; step <= 10; step++) {
                assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_brown_shulker_box_" + step + ".json"));
            }
            for (String color : ResourceTestSupport.CUSTOM_COLORS) {
                String bannerPath = "assets/dye_depot/items/" + color + "_banner.json";
                String bannerJson = read(zip, bannerPath);
                JsonObject banner = JsonParser.parseString(bannerJson).getAsJsonObject();
                assertEquals(
                        "minecraft:model",
                        banner.getAsJsonObject("model").get("type").getAsString(),
                        bannerPath
                );
                assertFalse(bannerJson.contains("minecraft:banner"), bannerPath);
                assertFalse(bannerJson.contains("\"color\":\"" + color + "\""), bannerPath);

                String safeColor = DDPolymerColors.vanillaColor(
                        DDDyes.valueOf(color.toUpperCase(Locale.ROOT)).get()
                ).getName();
                for (var patterned : Map.of(
                        "assets/dye_depot/items/polymer/" + color + "_banner_patterned.json", "ground",
                        "assets/dye_depot/items/polymer/" + color + "_wall_banner_patterned.json", "wall"
                ).entrySet()) {
                    JsonObject definition = JsonParser.parseString(read(zip, patterned.getKey())).getAsJsonObject();
                    JsonObject model = definition.getAsJsonObject("model");
                    assertEquals("minecraft:special", model.get("type").getAsString(), patterned.getKey());
                    assertEquals("minecraft:item/template_banner", model.get("base").getAsString(), patterned.getKey());
                    JsonObject special = model.getAsJsonObject("model");
                    assertEquals("minecraft:banner", special.get("type").getAsString(), patterned.getKey());
                    assertEquals(safeColor, special.get("color").getAsString(), patterned.getKey());
                    assertEquals(patterned.getValue(), special.get("attachment").getAsString(), patterned.getKey());
                }

                for (var plain : Map.of(
                        "assets/dye_depot/items/polymer/" + color + "_banner_plain.json", "ground",
                        "assets/dye_depot/items/polymer/" + color + "_wall_banner_plain.json", "wall"
                ).entrySet()) {
                    JsonObject definition = JsonParser.parseString(read(zip, plain.getKey())).getAsJsonObject();
                    JsonObject model = definition.getAsJsonObject("model");
                    assertEquals("minecraft:composite", model.get("type").getAsString(), plain.getKey());
                    var models = model.getAsJsonArray("models");
                    assertEquals("minecraft:special", models.get(0).getAsJsonObject().get("type").getAsString(), plain.getKey());
                    assertEquals(plain.getValue(), models.get(0).getAsJsonObject().getAsJsonObject("model").get("attachment").getAsString(), plain.getKey());
                    assertEquals("minecraft:model", models.get(1).getAsJsonObject().get("type").getAsString(), plain.getKey());
                    assertTrue(models.get(1).getAsJsonObject().get("model").getAsString().contains(color), plain.getKey());
                }

                for (String path : List.of(
                        "assets/minecraft/equipment/" + color + "_carpet.json",
                        "assets/dye_depot/textures/entity/equipment/llama_body/" + color + ".png",
                        "assets/dye_depot/items/polymer/" + color + "_sheep_wool.json",
                        "assets/dye_depot/items/polymer/" + color + "_carpet.json",
                        "assets/dye_depot/items/polymer/" + color + "_pane_0.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_pane_0.json",
                        "assets/dye_depot/models/item/polymer/" + color + "_sheep_wool.json",
                        "assets/dye_depot/models/item/polymer/" + color + "_banner.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_banner.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_wall_banner.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_banner_exact.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_wall_banner_exact.json",
                        "assets/dye_depot/models/item/polymer/" + color + "_shulker_empty.json"
                )) {
                    assertNotNull(zip.getEntry(path), path);
                }
                String sheepModel = read(zip, "assets/dye_depot/models/item/polymer/" + color + "_sheep_wool.json");
                assertTrue(sheepModel.contains("\"elements\""));
                assertTrue(sheepModel.contains(color + "_wool"));

                for (int step = 0; step <= 10; step++) {
                    String path = "assets/dye_depot/items/polymer/" + color + "_shulker_box_" + step + ".json";
                    JsonObject definition = JsonParser.parseString(read(zip, path)).getAsJsonObject();
                    JsonObject model = definition.getAsJsonObject("model");
                    assertEquals("minecraft:special", model.get("type").getAsString(), path);
                    assertEquals("dye_depot:item/polymer/" + color + "_shulker_empty", model.get("base").getAsString(), path);
                    JsonObject special = model.getAsJsonObject("model");
                    assertEquals("minecraft:shulker_box", special.get("type").getAsString(), path);
                    assertEquals("dye_depot:shulker_" + color, special.get("texture").getAsString(), path);
                    assertEquals(step / 10.0f, special.get("openness").getAsFloat(), 0.0001f, path);
                }
            }
        }
    }

    private static boolean isDyeDepot(Identifier id) {
        return id != null && DyeDepot.MOD_ID.equals(id.getNamespace());
    }

    private static boolean hasVisiblePixel(java.awt.image.BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0f;
        return normalized < 0 ? normalized + 360.0f : normalized;
    }

    private static String stringField(CompoundTag tag, String key) {
        return ((StringTag) tag.get(key)).value();
    }

    private static String patternColor(CompoundTag tag) {
        CompoundTag layer = (CompoundTag) ((ListTag) tag.get("patterns")).getFirst();
        return stringField(layer, "color");
    }

    private static CompoundTag coloredTag(DyeColor color) {
        CompoundTag tag = new CompoundTag();
        CompoundTag front = new CompoundTag();
        front.putString("color", color.getName());
        tag.put("front_text", front);
        ListTag patterns = new ListTag();
        CompoundTag layer = new CompoundTag();
        layer.putString("color", color.getName());
        patterns.add(layer);
        tag.put("patterns", patterns);
        return tag;
    }

    private static String read(ZipFile zip, String path) throws Exception {
        var entry = zip.getEntry(path);
        assertNotNull(entry, path);
        try (var input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
