package com.ninni.dye_depot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.polymer.DDPolymerBlockEntityNbt;
import com.ninni.dye_depot.polymer.DDPolymerBlocks;
import com.ninni.dye_depot.polymer.DDPolymerColors;
import com.ninni.dye_depot.polymer.DDPolymerCreativeTab;
import com.ninni.dye_depot.polymer.DDPolymerEntities;
import com.ninni.dye_depot.polymer.DDPolymerItemSanitizer;
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
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.impl.networking.context.PacketContextImpl;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ARGB;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
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
    private static final String LATE_PATTERN_NAMESPACE = "dye_depot_lifecycle_test";

    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void initializeMinecraftAndMod() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!BuiltInRegistries.ITEM.containsKey(DyeDepot.modLoc("maroon_dye"))) {
            new DyeDepot().onInitialize();
        }
        registerLatePatternPackContributor();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                .forEach(pending -> pending.apply());
    }

    private static void registerLatePatternPackContributor() {
        byte[] mask = ResourceTestSupport.bytes(
                "assets/dye_depot/textures/map/decorations/maroon_banner.png"
        );
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> {
            for (String target : List.of("banner", "shield")) {
                String direct = latePatternPath(target, "late_direct");
                builder.addData(direct, mask);
                builder.addStringData(
                        "assets/minecraft/atlases/" + target + "_patterns.json",
                        singleAtlas(direct.substring("assets/".length(), direct.length() - ".png".length())
                                .replace("/textures/", ":"))
                );
            }
            builder.addPreFinishTask(lateBuilder -> {
                for (String target : List.of("banner", "shield")) {
                    lateBuilder.addData(latePatternPath(target, "late_prefinish"), mask);
                }
            });
        });
    }

    private static String latePatternPath(String target, String name) {
        return "assets/" + LATE_PATTERN_NAMESPACE + "/textures/entity/" + target + "/" + name + ".png";
    }

    private static String singleAtlas(String texture) {
        return "{\"sources\":[{\"type\":\"minecraft:single\",\"resource\":\"" + texture
                + "\",\"sprite\":\"" + texture + "\"}]}";
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
        DDBlocks.BANNERS.forEach((color, holder) ->
                holder.value().getStateDefinition().getPossibleStates().forEach(state -> {
                    var carrier = DDPolymerBlocks.polymerState(state);
                    assertSame(Blocks.BANNER.pick(DDPolymerColors.vanillaColor(color)), carrier.getBlock(), state.toString());
                    assertEquals(state.getValue(BannerBlock.ROTATION), carrier.getValue(BannerBlock.ROTATION), state.toString());
                })
        );
        DDBlocks.WALL_BANNERS.forEach((color, holder) ->
                holder.value().getStateDefinition().getPossibleStates().forEach(state -> {
                    var carrier = DDPolymerBlocks.polymerState(state);
                    assertSame(Blocks.WALL_BANNER.pick(DDPolymerColors.vanillaColor(color)), carrier.getBlock(), state.toString());
                    assertEquals(state.getValue(WallBannerBlock.FACING), carrier.getValue(WallBannerBlock.FACING), state.toString());
                })
        );
        assertNotNull(BlockWithElementHolder.get(Blocks.STAINED_GLASS_PANE.pick(DyeColor.BROWN).defaultBlockState()));
        assertNotNull(BlockWithElementHolder.get(Blocks.DYED_SHULKER_BOX.pick(DyeColor.BROWN).defaultBlockState()));
    }

    @Test
    void virtualBlocksUseSharedTargetableGeometryCarriersWithoutPoolFallbacks() {
        assertEquals(9, DDPolymerBlocks.virtualCarrierCount());
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
            assertEquals(DyeColor.WHITE.getId(), safeUnsheared & 15, "white suppresses the native 26.2 undercoat");
            assertTrue((safeUnsheared & 16) != 0, "virtual coat suppresses native outer wool");
            assertEquals(DyeColor.WHITE.getId(), safeSheared & 15, "sheared proxy also suppresses native undercoat");
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
                assertVanillaSignCarrier((CompoundTag) safeSign.get("front_text"));
                assertVanillaSignCarrier((CompoundTag) safeSign.get("back_text"));
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
    void outboundSignsPreserveExactCustomRgbAndAuthoredComponentColors() {
        HolderLookup.Provider registries = VanillaRegistries.createLookup();

        for (DDDyes entry : DDDyes.values()) {
            DyeColor custom = entry.get();
            for (boolean glowing : List.of(false, true)) {
                int expectedRgb = (glowing
                        ? custom.getTextColor()
                        : ARGB.scaleRGB(custom.getTextColor(), 0.4F)) & 0xffffff;
                for (BlockEntityType<?> type : List.of(BlockEntityTypes.SIGN, BlockEntityTypes.HANGING_SIGN)) {
                    CompoundTag serverTag = signTag(custom, glowing, registries);
                    CompoundTag snapshot = serverTag.copy();
                    CompoundTag outbound = DDPolymerBlockEntityNbt.sanitize(type, serverTag, registries);

                    assertNotSame(serverTag, outbound);
                    assertEquals(snapshot, serverTag, "outbound sanitization must not mutate server sign NBT");
                    for (String sectionName : List.of("front_text", "back_text")) {
                        CompoundTag section = (CompoundTag) outbound.get(sectionName);
                        assertNotNull(section);
                        assertEquals(glowing, section.getBooleanOr("has_glowing_text", false));
                        assertVanillaSignCarrier(section);
                        assertSignMessageColors(section, "messages", registries, expectedRgb);
                        assertSignMessageColors(section, "filtered_messages", registries, expectedRgb);
                    }
                }
            }
        }

        CompoundTag vanilla = signTag(DyeColor.RED, true, registries);
        assertSame(
                vanilla,
                DDPolymerBlockEntityNbt.sanitize(BlockEntityTypes.SIGN, vanilla, registries),
                "vanilla sign colors must not be reconstructed"
        );
    }

    @Test
    void nestedSignItemDataUsesTheRegistryAwareExactRgbSanitizer() {
        HolderLookup.Provider registries = VanillaRegistries.createLookup();
        DyeColor custom = DDDyes.MINT.get();
        CompoundTag serverTag = signTag(custom, true, registries);
        CompoundTag snapshot = serverTag.copy();
        ItemStack serverStack = new ItemStack(Items.OAK_SIGN);
        serverStack.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(BlockEntityTypes.SIGN, serverTag)
        );

        ItemStack clientStack = serverStack.copy();
        DDPolymerItemSanitizer.sanitize(
                serverStack,
                clientStack,
                registries,
                new Connection(PacketFlow.CLIENTBOUND).getPacketContext()
        );

        assertEquals(snapshot, serverStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId());
        CompoundTag outbound = clientStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
        CompoundTag front = (CompoundTag) outbound.get("front_text");
        assertVanillaSignCarrier(front);
        assertSignMessageColors(front, "messages", registries, custom.getTextColor() & 0xffffff);
    }

    @Test
    void vanillaSignItemBlockEntityDataTriggersNormalPolymerConversion() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        DyeColor custom = DDDyes.MINT.get();
        CompoundTag serverTag = signTag(custom, true, registries);
        CompoundTag snapshot = serverTag.copy();
        ItemStack serverStack = new ItemStack(Items.OAK_SIGN);
        serverStack.set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(BlockEntityTypes.SIGN, serverTag)
        );
        PacketContext context = new Connection(PacketFlow.CLIENTBOUND).getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);

        ItemStack clientStack = PolymerItemUtils.getPolymerItemStack(
                serverStack,
                TooltipFlag.NORMAL,
                context,
                registries
        );

        assertNotSame(serverStack, clientStack, "custom sign NBT must opt a vanilla sign into Polymer conversion");
        assertEquals(
                snapshot,
                serverStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId(),
                "normal Polymer conversion must not mutate the server stack"
        );
        CompoundTag outbound = clientStack.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
        for (String sectionName : List.of("front_text", "back_text")) {
            CompoundTag section = (CompoundTag) outbound.get(sectionName);
            assertNotNull(section);
            assertVanillaSignCarrier(section);
            assertSignMessageColors(section, "messages", registries, custom.getTextColor() & 0xffffff);
            assertSignMessageColors(section, "filtered_messages", registries, custom.getTextColor() & 0xffffff);
        }
    }

    @Test
    void outboundBannerTagsPrependAnExactVisualBaseWithoutMutatingServerPatterns() {
        HolderLookup.Provider registries = VanillaRegistries.createLookup();
        for (DDDyes dye : DDDyes.values()) {
            CompoundTag original = coloredPatternTag(dye.get());
            CompoundTag snapshot = original.copy();
            CompoundTag outbound = DDPolymerBlockEntityNbt.withBannerBase(original, dye.get());

            assertEquals(1, ((ListTag) original.get("patterns")).size(), dye.getName());
            ListTag layers = (ListTag) outbound.get("patterns");
            assertEquals(2, layers.size(), dye.getName());
            CompoundTag base = (CompoundTag) layers.getFirst();
            assertEquals("dye_depot:polymer_base_" + dye.getName(), stringField(base, "pattern"));
            assertEquals(DyeColor.WHITE.getName(), stringField(base, "color"));
            assertEquals(dye.getName(), stringField((CompoundTag) layers.get(1), "color"));

            CompoundTag safe = DDPolymerBlockEntityNbt.sanitize(BlockEntityTypes.BANNER, outbound, registries);
            ListTag safeLayers = (ListTag) safe.get("patterns");
            assertEquals(DyeColor.WHITE.getName(), stringField((CompoundTag) safeLayers.getFirst(), "color"));
            CompoundTag safeAuthored = (CompoundTag) safeLayers.get(1);
            assertEquals(DyeColor.WHITE.getName(), stringField(safeAuthored, "color"));
            assertEquals("preserve_me", stringField((CompoundTag) safeAuthored.get("metadata"), "marker"));
            CompoundTag inlinePattern = (CompoundTag) safeAuthored.get("pattern");
            assertEquals(
                    "minecraft:cross_dye_depot_" + dye.getName(),
                    stringField(inlinePattern, "asset_id")
            );
            assertEquals("block.minecraft.banner.cross", stringField(inlinePattern, "translation_key"));
            var decoded = BannerPattern.CODEC.parse(
                    RegistryOps.create(NbtOps.INSTANCE, registries),
                    inlinePattern
            ).getOrThrow();
            assertTrue(decoded.unwrapKey().isEmpty(), "exact visual pattern is an inline client-only holder");
            assertEquals(snapshot, original, "outbound exact-color conversion must not mutate server banner NBT");
        }

        CompoundTag vanilla = coloredTag(DyeColor.RED);
        assertSame(
                vanilla,
                DDPolymerBlockEntityNbt.withBannerBase(vanilla, DyeColor.RED),
                "vanilla banners retain their native base and do not consume a pattern layer"
        );
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
            for (String model : List.of("post", "side", "side_alt", "noside", "noside_alt")) {
                String path = "assets/minecraft/models/block/brown_stained_glass_pane_" + model + ".json";
                assertEquals(0, JsonParser.parseString(read(zip, path)).getAsJsonObject().getAsJsonArray("elements").size(), path);
            }

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
            assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_brown_shulker_base.json"));
            assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/donor_brown_shulker_lid.json"));
            assertNotNull(zip.getEntry("assets/dye_depot/textures/block/polymer/shulker_donor_brown.png"));

            Map<String, String> sheepTextureHashes = Map.of(
                    "sheep_wool", "4f76a7d14c8248288e7ad1bc73ea76bd509c19c828dd02268453406fd59d961c",
                    "sheep_wool_baby", "d0e441f198a1598b477ba1cabc29c41599d5533df4c7820df63374e8846a3e51",
                    "sheep_wool_undercoat", "579f3e6d76f3651c94b1b4d9e8d8a0d9abd1be7ea7edf6223b15c76b69a95b42"
            );
            for (var texture : sheepTextureHashes.entrySet()) {
                String name = texture.getKey();
                String packPath = "assets/dye_depot/textures/block/polymer/" + name + ".png";
                byte[] bytes = zip.getInputStream(zip.getEntry(packPath)).readAllBytes();
                assertEquals(
                        texture.getValue(),
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                        packPath + " must embed the exact 26.2 sheep-wool texture without a client-jar lookup"
                );
            }
            List<String> sheepParts = List.of(
                    "adult_head",
                    "adult_body",
                    "adult_leg",
                    "adult_right_leg",
                    "baby_head",
                    "baby_body",
                    "baby_right_hind_leg",
                    "baby_left_hind_leg",
                    "baby_right_front_leg",
                    "baby_left_front_leg",
                    "adult_undercoat_head",
                    "adult_undercoat_body",
                    "adult_undercoat_leg",
                    "adult_undercoat_right_leg"
            );
            for (String part : sheepParts) {
                String itemPath = "assets/dye_depot/items/polymer/sheep/" + part + ".json";
                String modelPath = "assets/dye_depot/models/item/polymer/sheep/" + part + ".json";
                JsonObject itemModel = JsonParser.parseString(read(zip, itemPath)).getAsJsonObject()
                        .getAsJsonObject("model");
                assertEquals("minecraft:dye", itemModel.getAsJsonArray("tints").get(0)
                        .getAsJsonObject().get("type").getAsString(), itemPath);
                JsonObject partModel = JsonParser.parseString(read(zip, modelPath)).getAsJsonObject();
                int expectedElements = Set.of("adult_head", "adult_body", "adult_leg", "adult_right_leg")
                                .contains(part)
                        ? 2
                        : 1;
                assertEquals(expectedElements, partModel.getAsJsonArray("elements").size(), modelPath);
                partModel.getAsJsonArray("elements").forEach(element ->
                        element.getAsJsonObject().getAsJsonObject("faces").entrySet().forEach(face ->
                                assertEquals(0, face.getValue().getAsJsonObject().get("tintindex").getAsInt(), modelPath)
                        )
                );
            }
            JsonObject adultHead = JsonParser.parseString(read(
                    zip,
                    "assets/dye_depot/models/item/polymer/sheep/adult_head.json"
            )).getAsJsonObject();
            JsonObject adultUndercoat = adultHead.getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals("[4.99,5.99,1.99]", adultUndercoat.getAsJsonArray("from").toString());
            assertEquals("[11.01,12.01,10.01]", adultUndercoat.getAsJsonArray("to").toString());
            assertEquals(
                    "#sheep_wool_undercoat",
                    adultUndercoat.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString()
            );
            JsonObject adultOuterWool = adultHead.getAsJsonArray("elements").get(1).getAsJsonObject();
            assertEquals("[4.4,5.4,3.4]", adultOuterWool.getAsJsonArray("from").toString());
            assertEquals("[11.6,12.6,10.6]", adultOuterWool.getAsJsonArray("to").toString());
            assertEquals(
                    "[1.5,3.0,3.0,6.0]",
                    adultOuterWool.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").toString()
            );
            var babyLegNorthUvs = new HashSet<String>();
            for (String leg : sheepParts.subList(6, 10)) {
                JsonObject legModel = JsonParser.parseString(read(
                        zip,
                        "assets/dye_depot/models/item/polymer/sheep/" + leg + ".json"
                )).getAsJsonObject();
                babyLegNorthUvs.add(legModel.getAsJsonArray("elements").get(0).getAsJsonObject()
                        .getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").toString());
            }
            assertEquals(4, babyLegNorthUvs.size(), "each baby leg must retain its separately authored native UV region");
            JsonObject leftUndercoatLeg = JsonParser.parseString(read(
                    zip,
                    "assets/dye_depot/models/item/polymer/sheep/adult_undercoat_leg.json"
            )).getAsJsonObject();
            JsonObject rightUndercoatLeg = JsonParser.parseString(read(
                    zip,
                    "assets/dye_depot/models/item/polymer/sheep/adult_undercoat_right_leg.json"
            )).getAsJsonObject();
            assertEquals(
                    "[1.0,10.0,2.0,16.0]",
                    leftUndercoatLeg.getAsJsonArray("elements").get(0).getAsJsonObject()
                            .getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").toString(),
                    "left adult undercoat leg keeps vanilla's unmirrored UVs"
            );
            assertEquals(
                    "[2.0,10.0,1.0,16.0]",
                    rightUndercoatLeg.getAsJsonArray("elements").get(0).getAsJsonObject()
                            .getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").toString(),
                    "right adult undercoat leg keeps vanilla's mirrored UVs"
            );

            assertNull(
                    zip.getEntry("assets/minecraft/textures/entity/banner/base.png"),
                    "the native base texture must stay untouched for exact vanilla banners and Loom capacity"
            );
            var paletteKey = ImageIO.read(zip.getInputStream(zip.getEntry(
                    "assets/dye_depot/textures/polymer/banner_patterns/key.png"
            )));
            assertEquals(256, paletteKey.getWidth());
            assertEquals(1, paletteKey.getHeight());
            for (int gray = 0; gray < 256; gray++) {
                assertEquals(ARGB.color(255, gray, gray, gray), paletteKey.getRGB(gray, 0));
            }

            for (String atlasName : List.of("banner_patterns", "shield_patterns")) {
                JsonObject atlas = JsonParser.parseString(read(
                        zip,
                        "assets/minecraft/atlases/" + atlasName + ".json"
                )).getAsJsonObject();
                List<JsonObject> exactSources = atlas.getAsJsonArray("sources").asList().stream()
                        .filter(JsonElement::isJsonObject)
                        .map(JsonElement::getAsJsonObject)
                        .filter(candidate -> candidate.has("palette_key")
                                && "dye_depot:polymer/banner_patterns/key".equals(
                                        candidate.get("palette_key").getAsString()
                                ))
                        .toList();
                assertEquals(1, exactSources.size(), atlasName + " must contain one refreshed exact-color source");
                JsonObject source = exactSources.getFirst();
                assertEquals("minecraft:paletted_permutations", source.get("type").getAsString());
                assertEquals(
                        "dye_depot:polymer/banner_patterns/key",
                        source.get("palette_key").getAsString()
                );
                assertEquals("_", source.get("separator").getAsString());
                String target = atlasName.equals("banner_patterns") ? "banner" : "shield";
                String texturePrefix = "minecraft:entity/" + target + "/";
                Set<String> patternTextures = new HashSet<>();
                source.getAsJsonArray("textures").forEach(texture -> patternTextures.add(texture.getAsString()));
                assertEquals(43 + 2, patternTextures.size(), atlasName);
                assertTrue(patternTextures.contains(texturePrefix + "cross"));
                String lateDirect = LATE_PATTERN_NAMESPACE + ":entity/" + target + "/late_direct";
                String latePrefinish = LATE_PATTERN_NAMESPACE + ":entity/" + target + "/late_prefinish";
                assertTrue(patternTextures.contains(lateDirect), atlasName + " must include later listener assets");
                assertTrue(
                        patternTextures.contains(latePrefinish),
                        atlasName + " must include assets made by the later listener's pre-finish task"
                );
                assertTrue(
                        atlas.getAsJsonArray("sources").asList().stream()
                                .filter(JsonElement::isJsonObject)
                                .map(JsonElement::getAsJsonObject)
                                .anyMatch(candidate -> candidate.has("resource")
                                        && lateDirect.equals(candidate.get("resource").getAsString())),
                        atlasName + " must preserve foreign merged atlas sources"
                );
                assertNotNull(zip.getEntry(latePatternPath(target, "late_direct")));
                assertNotNull(zip.getEntry(latePatternPath(target, "late_prefinish")));
                JsonObject permutations = source.getAsJsonObject("permutations");
                assertEquals(DDDyes.values().length, permutations.size());
                for (DDDyes dye : DDDyes.values()) {
                    assertEquals(
                            "dye_depot:polymer/banner_patterns/" + dye.getName(),
                            permutations.get("dye_depot_" + dye.getName()).getAsString()
                    );
                }
            }
            for (String color : ResourceTestSupport.CUSTOM_COLORS) {
                String palettePath = "assets/dye_depot/textures/polymer/banner_patterns/" + color + ".png";
                var palette = ImageIO.read(zip.getInputStream(zip.getEntry(palettePath)));
                assertEquals(256, palette.getWidth(), palettePath);
                assertEquals(1, palette.getHeight(), palettePath);
                DyeColor paletteColor = DyeColor.byName(color, null);
                assertNotNull(paletteColor, color);
                for (int gray = 0; gray < 256; gray++) {
                    assertEquals(
                            expectedShieldPatternPixel(paletteColor, gray),
                            palette.getRGB(gray, 0),
                            palettePath + " grayscale entry " + gray
                    );
                }

                String texturePath = "assets/dye_depot/textures/entity/banner/polymer_base_" + color + ".png";
                var texture = ImageIO.read(zip.getInputStream(zip.getEntry(texturePath)));
                assertEquals(64, texture.getWidth(), texturePath);
                assertEquals(64, texture.getHeight(), texturePath);
                assertTrue(hasVisiblePixel(texture), texturePath);

                String shieldTexturePath = "assets/dye_depot/textures/entity/shield/polymer_base_" + color + ".png";
                var shieldTexture = ImageIO.read(zip.getInputStream(zip.getEntry(shieldTexturePath)));
                assertEquals(64, shieldTexture.getWidth(), shieldTexturePath);
                assertEquals(64, shieldTexture.getHeight(), shieldTexturePath);
                for (int y = 0; y < shieldTexture.getHeight(); y++) {
                    for (int x = 0; x < shieldTexture.getWidth(); x++) {
                        int expectedAlpha = x >= 2 && x <= 11 && y >= 2 && y <= 21 ? 255 : 0;
                        assertEquals(
                                expectedAlpha,
                                ARGB.alpha(shieldTexture.getRGB(x, y)),
                                shieldTexturePath + " face mask at " + x + ',' + y
                        );
                    }
                }
                DyeColor shieldColor = DyeColor.byName(color, null);
                assertNotNull(shieldColor, color);
                assertEquals(
                        // ImageIO exposes the grayscale PNG's raw 229 sample
                        // as sRGB 243, which is also what pack generation uses.
                        expectedShieldPatternPixel(shieldColor, 243),
                        shieldTexture.getRGB(2, 2),
                        shieldTexturePath + " must compensate its exact base tint"
                );
                assertEquals(
                        // The mask's raw 242 highlight becomes sRGB 249.
                        expectedShieldPatternPixel(shieldColor, 249),
                        shieldTexture.getRGB(4, 3),
                        shieldTexturePath + " must preserve the native mask shading"
                );

                JsonObject pattern = ResourceTestSupport.json(
                        "data/dye_depot/banner_pattern/polymer_base_" + color + ".json"
                );
                assertEquals("dye_depot:polymer_base_" + color, pattern.get("asset_id").getAsString());
                assertEquals("block.minecraft.banner.base", pattern.get("translation_key").getAsString());
            }

            for (String color : ResourceTestSupport.CUSTOM_COLORS) {
                assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/" + color + "_shulker_base.json"));
                assertNotNull(zip.getEntry("assets/dye_depot/items/polymer/" + color + "_shulker_lid.json"));
                assertNotNull(zip.getEntry("assets/dye_depot/models/block/polymer/" + color + "_shulker_base.json"));
                assertNotNull(zip.getEntry("assets/dye_depot/models/block/polymer/" + color + "_shulker_lid.json"));
                assertNotNull(zip.getEntry("assets/dye_depot/textures/block/polymer/shulker_" + color + ".png"));
                JsonObject shulkerBase = JsonParser.parseString(read(
                        zip,
                        "assets/dye_depot/models/block/polymer/" + color + "_shulker_base.json"
                )).getAsJsonObject();
                JsonObject baseFaces = shulkerBase.getAsJsonArray("elements")
                        .get(0).getAsJsonObject().getAsJsonObject("faces");
                assertEquals("[4.0,7.0,8.0,11.0]", baseFaces.getAsJsonObject("up").getAsJsonArray("uv").toString());
                assertEquals("[8.0,11.0,12.0,7.0]", baseFaces.getAsJsonObject("down").getAsJsonArray("uv").toString());
                assertEquals(6, shulkerBase.getAsJsonArray("elements").size());
                JsonObject baseInterior = shulkerBase.getAsJsonArray("elements").get(1).getAsJsonObject();
                assertEquals("[0.0,0.01,0.0]", baseInterior.getAsJsonArray("from").toString());
                assertEquals(
                        "[8.0,11.0,12.0,7.0]",
                        baseInterior.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv").toString()
                );
                assertEquals(
                        "[0.0,11.0,4.0,13.0]",
                        shulkerBase.getAsJsonArray("elements").get(2).getAsJsonObject()
                                .getAsJsonObject("faces").getAsJsonObject("east").getAsJsonArray("uv").toString()
                );
                assertEquals(
                        "[8.0,11.0,12.0,13.0]",
                        shulkerBase.getAsJsonArray("elements").get(3).getAsJsonObject()
                                .getAsJsonObject("faces").getAsJsonObject("west").getAsJsonArray("uv").toString()
                );
                assertEquals(
                        "[4.0,11.0,8.0,13.0]",
                        shulkerBase.getAsJsonArray("elements").get(4).getAsJsonObject()
                                .getAsJsonObject("faces").getAsJsonObject("south").getAsJsonArray("uv").toString()
                );
                assertEquals(
                        "[12.0,11.0,16.0,13.0]",
                        shulkerBase.getAsJsonArray("elements").get(5).getAsJsonObject()
                                .getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv").toString()
                );
                JsonObject shulkerLid = JsonParser.parseString(read(
                        zip,
                        "assets/dye_depot/models/block/polymer/" + color + "_shulker_lid.json"
                )).getAsJsonObject();
                JsonObject lidFaces = shulkerLid.getAsJsonArray("elements")
                        .get(0).getAsJsonObject().getAsJsonObject("faces");
                assertEquals(2, shulkerLid.getAsJsonArray("elements").size());
                assertEquals("[4.0,0.0,8.0,4.0]", lidFaces.getAsJsonObject("up").getAsJsonArray("uv").toString());
                assertEquals("[8.0,4.0,12.0,0.0]", lidFaces.getAsJsonObject("down").getAsJsonArray("uv").toString());
                JsonObject lidInterior = shulkerLid.getAsJsonArray("elements").get(1).getAsJsonObject();
                assertEquals("[0.0,15.99,0.0]", lidInterior.getAsJsonArray("from").toString());
                assertEquals(
                        "[4.0,0.0,8.0,4.0]",
                        lidInterior.getAsJsonObject("faces").getAsJsonObject("down").getAsJsonArray("uv").toString()
                );
                String bannerPath = "assets/dye_depot/items/" + color + "_banner.json";
                String bannerJson = read(zip, bannerPath);
                JsonObject banner = JsonParser.parseString(bannerJson).getAsJsonObject();
                assertFalse(bannerJson.contains("\"color\":\"" + color + "\""), bannerPath);

                String safeColor = DDPolymerColors.vanillaColor(
                        DDDyes.valueOf(color.toUpperCase(Locale.ROOT)).get()
                ).getName();
                for (var patterned : Map.of(
                        bannerPath, "ground",
                        "assets/dye_depot/items/polymer/" + color + "_banner_patterned.json", "ground"
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

                for (String path : List.of(
                        "assets/minecraft/equipment/" + color + "_carpet.json",
                        "assets/dye_depot/textures/entity/equipment/llama_body/" + color + ".png",
                        "assets/dye_depot/items/polymer/" + color + "_carpet.json",
                        "assets/dye_depot/items/polymer/" + color + "_pane_0.json",
                        "assets/dye_depot/models/block/polymer/" + color + "_pane_0.json"
                )) {
                    assertNotNull(zip.getEntry(path), path);
                }
                assertNull(zip.getEntry("assets/dye_depot/items/polymer/" + color + "_sheep_wool.json"));
                assertNull(zip.getEntry("assets/dye_depot/models/item/polymer/" + color + "_sheep_wool.json"));

                for (int mask = 0; mask < 16; mask++) {
                    String path = "assets/dye_depot/models/block/polymer/" + color + "_pane_" + mask + ".json";
                    JsonObject model = JsonParser.parseString(read(zip, path)).getAsJsonObject();
                    var elements = model.getAsJsonArray("elements");
                    assertEquals(1 + Integer.bitCount(mask), elements.size(), path);
                    elements.forEach(element -> {
                        JsonObject part = element.getAsJsonObject();
                        JsonObject faces = part.getAsJsonObject("faces");
                        faces.entrySet().forEach(face ->
                                assertEquals(4, face.getValue().getAsJsonObject().getAsJsonArray("uv").size(), path + " " + face.getKey())
                        );
                        var from = part.getAsJsonArray("from");
                        var to = part.getAsJsonArray("to");
                        if (from.get(2).getAsDouble() == 0.0) assertFalse(faces.has("north"), path + " north connection cap");
                        if (to.get(0).getAsDouble() == 16.0) assertFalse(faces.has("east"), path + " east connection cap");
                        if (to.get(2).getAsDouble() == 16.0) assertFalse(faces.has("south"), path + " south connection cap");
                        if (from.get(0).getAsDouble() == 0.0) assertFalse(faces.has("west"), path + " west connection cap");
                    });
                    if (mask == 10) {
                        JsonObject eastFaces = elements.get(1).getAsJsonObject().getAsJsonObject("faces");
                        assertEquals("[16,0,9,16]", eastFaces.getAsJsonObject("north").getAsJsonArray("uv").toString(), path);
                        assertEquals("[9,0,16,16]", eastFaces.getAsJsonObject("south").getAsJsonArray("uv").toString(), path);
                        JsonObject westFaces = elements.get(2).getAsJsonObject().getAsJsonObject("faces");
                        assertEquals("[7,0,0,16]", westFaces.getAsJsonObject("north").getAsJsonArray("uv").toString(), path);
                        assertEquals("[0,0,7,16]", westFaces.getAsJsonObject("south").getAsJsonArray("uv").toString(), path);
                    }
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

    private static CompoundTag coloredPatternTag(DyeColor color) {
        CompoundTag tag = new CompoundTag();
        ListTag patterns = new ListTag();
        CompoundTag layer = new CompoundTag();
        layer.putString("pattern", BannerPatterns.CROSS.identifier().toString());
        layer.putString("color", color.getName());
        CompoundTag metadata = new CompoundTag();
        metadata.putString("marker", "preserve_me");
        layer.put("metadata", metadata);
        patterns.add(layer);
        tag.put("patterns", patterns);
        return tag;
    }

    private static CompoundTag signTag(
            DyeColor color,
            boolean glowing,
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();
        CompoundTag text = new CompoundTag();
        text.putString("color", color.getName());
        text.putBoolean("has_glowing_text", glowing);

        List<Component> messages = List.of(
                Component.literal("unstyled"),
                Component.literal("authored").withColor(0x123456),
                Component.literal("parent").append(Component.literal("child").withColor(0x654321)),
                Component.translatable("block.minecraft.oak_sign")
        );
        List<Component> filteredMessages = List.of(
                Component.literal("filtered unstyled"),
                Component.literal("filtered authored").withColor(0x123456),
                Component.literal("filtered parent").append(Component.literal("filtered child").withColor(0x654321)),
                Component.translatable("block.minecraft.spruce_sign")
        );
        var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        text.put("messages", ComponentSerialization.CODEC.listOf().encodeStart(ops, messages).getOrThrow());
        text.put(
                "filtered_messages",
                ComponentSerialization.CODEC.listOf().encodeStart(ops, filteredMessages).getOrThrow()
        );
        tag.put("front_text", text);
        tag.put("back_text", text.copy());
        return tag;
    }

    private static void assertSignMessageColors(
            CompoundTag section,
            String field,
            HolderLookup.Provider registries,
            int expectedRgb
    ) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        List<Component> messages = ComponentSerialization.CODEC.listOf()
                .parse(ops, section.get(field))
                .getOrThrow();
        assertEquals(4, messages.size(), field);
        assertEquals(expectedRgb, messages.get(0).getStyle().getColor().getValue(), field + " unstyled root");
        assertEquals(0x123456, messages.get(1).getStyle().getColor().getValue(), field + " authored root");
        assertEquals(expectedRgb, messages.get(2).getStyle().getColor().getValue(), field + " nested root");
        assertEquals(
                0x654321,
                messages.get(2).getSiblings().getFirst().getStyle().getColor().getValue(),
                field + " authored child"
        );
        assertEquals(expectedRgb, messages.get(3).getStyle().getColor().getValue(), field + " translated root");
    }

    private static void assertVanillaSignCarrier(CompoundTag section) {
        DyeColor safeColor = DyeColor.byName(stringField(section, "color"), null);
        assertNotNull(safeColor);
        assertTrue(safeColor.getId() < 16, "sign carrier color must be vanilla-codec safe");
        assertTrue(safeColor != DyeColor.BLACK, "custom sign colors must not trigger BLACK's pale glow outline");
    }

    private static int expectedShieldPatternPixel(DyeColor color, int sourceGray) {
        int tint = color.getTextureDiffuseColor();
        int white = DyeColor.WHITE.getTextureDiffuseColor();
        return ARGB.color(
                255,
                compensatedPatternChannel(sourceGray, ARGB.red(tint), ARGB.red(white)),
                compensatedPatternChannel(sourceGray, ARGB.green(tint), ARGB.green(white)),
                compensatedPatternChannel(sourceGray, ARGB.blue(tint), ARGB.blue(white))
        );
    }

    private static int compensatedPatternChannel(int source, int tint, int white) {
        int desired = Math.round(source * tint / 255.0f);
        return Math.min(255, Math.round(desired * 255.0f / white));
    }

    private static String read(ZipFile zip, String path) throws Exception {
        var entry = zip.getEntry(path);
        assertNotNull(entry, path);
        try (var input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
