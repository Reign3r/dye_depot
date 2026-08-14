package com.ninni.dye_depot.test;

import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.block.DyeBasketBlock;
import com.ninni.dye_depot.polymer.DDPolymerBlocks;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import com.ninni.dye_depot.registry.DDMapDecorationType;
import com.ninni.dye_depot.registry.DDPoiTypes;
import com.ninni.dye_depot.registry.DDTags;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.impl.networking.context.PacketContextImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.phys.Vec3;

public final class DyeDepotGameTests {
    private static final int CUSTOM_COLOR_COUNT = 16;
    private static final int MOD_BLOCK_COUNT = 256;
    private static final int MOD_ITEM_COUNT = 240;
    private static final int MOD_TRADE_COUNT = 144;
    private static final List<DyeColor> COLOR_ORDER = List.of(
            DyeColor.WHITE,
            DyeColor.LIGHT_GRAY,
            DyeColor.GRAY,
            DyeColor.BLACK,
            DyeColor.BROWN,
            DDDyes.MAROON.get(),
            DDDyes.ROSE.get(),
            DyeColor.RED,
            DDDyes.CORAL.get(),
            DDDyes.GINGER.get(),
            DyeColor.ORANGE,
            DDDyes.TAN.get(),
            DDDyes.BEIGE.get(),
            DyeColor.YELLOW,
            DDDyes.AMBER.get(),
            DDDyes.OLIVE.get(),
            DyeColor.LIME,
            DDDyes.FOREST.get(),
            DyeColor.GREEN,
            DDDyes.VERDANT.get(),
            DDDyes.TEAL.get(),
            DyeColor.CYAN,
            DDDyes.MINT.get(),
            DDDyes.AQUA.get(),
            DyeColor.LIGHT_BLUE,
            DyeColor.BLUE,
            DDDyes.SLATE.get(),
            DDDyes.NAVY.get(),
            DDDyes.INDIGO.get(),
            DyeColor.PURPLE,
            DyeColor.MAGENTA,
            DyeColor.PINK
    );

    @GameTest
    public void dyeColorExtensionMatchesTheBaselineContract(GameTestHelper helper) {
        helper.assertValueEqual(DyeColor.values().length, 32, "DyeColor enum size");
        helper.assertValueEqual(DyeColor.VALUES.size(), 32, "DyeColor values list size");

        for (DDDyes expected : DDDyes.values()) {
            DyeColor actual = expected.get();
            helper.assertValueEqual(actual.getId(), expected.getId(), expected + " numeric ID");
            helper.assertValueEqual(actual.getName(), expected.getName(), expected + " serialized name");
            helper.assertValueEqual(DyeColor.byId(expected.getId()), actual, expected + " ID lookup");
            helper.assertValueEqual(actual.getMapColor(), expected.getMapColor(), expected + " map color");
            helper.assertValueEqual(actual.getTerracottaColor(), expected.getMapColor(), expected + " terracotta map color");
            helper.assertValueEqual(actual.getTextureDiffuseColor(), ARGB.opaque(expected.getColor()), expected + " diffuse color");
            helper.assertValueEqual(actual.getFireworkColor(), expected.getFireworkColor(), expected + " firework color");
            helper.assertValueEqual(actual.getTextColor(), ARGB.opaque(expected.getTextColor()), expected + " text color");
            helper.assertValueEqual(
                    DyeColor.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(expected.getName())).getOrThrow(),
                    actual,
                    expected + " string codec"
            );
            helper.assertValueEqual(
                    DyeColor.LEGACY_ID_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(expected.getId())).getOrThrow(),
                    actual,
                    expected + " legacy ID codec"
            );
            helper.assertTrue(DDDyes.isModDye(actual), expected + " is recognized as a Dye Depot color");
        }

        helper.succeed();
    }

    @GameTest
    public void vanillaColorCollectionsResolveEveryCustomFamily(GameTestHelper helper) {
        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();

            helper.assertValueEqual(Blocks.BED.pick(color), DDBlocks.BEDS.getOrThrow(color), color + " bed block lookup");
            helper.assertValueEqual(Blocks.WOOL.pick(color), DDBlocks.WOOL.getOrThrow(color), color + " wool block lookup");
            helper.assertValueEqual(Blocks.STAINED_GLASS.pick(color), DDBlocks.STAINED_GLASS.getOrThrow(color), color + " glass block lookup");
            helper.assertValueEqual(Blocks.DYED_TERRACOTTA.pick(color), DDBlocks.TERRACOTTA.getOrThrow(color), color + " terracotta block lookup");
            helper.assertValueEqual(Blocks.STAINED_GLASS_PANE.pick(color), DDBlocks.STAINED_GLASS_PANES.getOrThrow(color), color + " pane block lookup");
            helper.assertValueEqual(Blocks.CARPET.pick(color), DDBlocks.CARPETS.getOrThrow(color), color + " carpet block lookup");
            helper.assertValueEqual(Blocks.BANNER.pick(color), DDBlocks.BANNERS.getOrThrow(color), color + " banner block lookup");
            helper.assertValueEqual(Blocks.WALL_BANNER.pick(color), DDBlocks.WALL_BANNERS.getOrThrow(color), color + " wall banner block lookup");
            helper.assertValueEqual(Blocks.DYED_SHULKER_BOX.pick(color), DDBlocks.SHULKER_BOXES.getOrThrow(color), color + " shulker block lookup");
            helper.assertValueEqual(Blocks.GLAZED_TERRACOTTA.pick(color), DDBlocks.GLAZED_TERRACOTTA.getOrThrow(color), color + " glazed terracotta block lookup");
            helper.assertValueEqual(Blocks.CONCRETE.pick(color), DDBlocks.CONCRETE.getOrThrow(color), color + " concrete block lookup");
            helper.assertValueEqual(Blocks.CONCRETE_POWDER.pick(color), DDBlocks.CONCRETE_POWDER.getOrThrow(color), color + " powder block lookup");
            helper.assertValueEqual(Blocks.DYED_CANDLE.pick(color), DDBlocks.CANDLES.getOrThrow(color), color + " candle block lookup");
            helper.assertValueEqual(Blocks.DYED_CANDLE_CAKE.pick(color), DDBlocks.CANDLE_CAKES.getOrThrow(color), color + " candle cake block lookup");

            helper.assertValueEqual(Items.WOOL.pick(color), DDBlocks.WOOL.getOrThrow(color).asItem(), color + " wool item lookup");
            helper.assertValueEqual(Items.DYED_TERRACOTTA.pick(color), DDBlocks.TERRACOTTA.getOrThrow(color).asItem(), color + " terracotta item lookup");
            helper.assertValueEqual(Items.CARPET.pick(color), DDBlocks.CARPETS.getOrThrow(color).asItem(), color + " carpet item lookup");
            helper.assertValueEqual(Items.STAINED_GLASS.pick(color), DDBlocks.STAINED_GLASS.getOrThrow(color).asItem(), color + " glass item lookup");
            helper.assertValueEqual(Items.STAINED_GLASS_PANE.pick(color), DDBlocks.STAINED_GLASS_PANES.getOrThrow(color).asItem(), color + " pane item lookup");
            helper.assertValueEqual(Items.DYED_SHULKER_BOX.pick(color), DDItems.SHULKER_BOXES.getOrThrow(color), color + " shulker item lookup");
            helper.assertValueEqual(Items.GLAZED_TERRACOTTA.pick(color), DDBlocks.GLAZED_TERRACOTTA.getOrThrow(color).asItem(), color + " glazed terracotta item lookup");
            helper.assertValueEqual(Items.CONCRETE.pick(color), DDBlocks.CONCRETE.getOrThrow(color).asItem(), color + " concrete item lookup");
            helper.assertValueEqual(Items.CONCRETE_POWDER.pick(color), DDBlocks.CONCRETE_POWDER.getOrThrow(color).asItem(), color + " powder item lookup");
            helper.assertValueEqual(Items.DYE.pick(color), DDItems.DYES.getOrThrow(color), color + " dye item lookup");
            helper.assertValueEqual(Items.BED.pick(color), DDItems.BEDS.getOrThrow(color), color + " bed item lookup");
            helper.assertValueEqual(Items.BANNER.pick(color), DDItems.BANNERS.getOrThrow(color), color + " banner item lookup");
            helper.assertValueEqual(Items.DYED_CANDLE.pick(color), DDBlocks.CANDLES.getOrThrow(color).asItem(), color + " candle item lookup");
        }

        helper.succeed();
    }

    @GameTest
    public void allBlocksItemsAndColorBindingsAreRegistered(GameTestHelper helper) {
        long blockCount = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals(DyeDepot.MOD_ID))
                .count();
        long itemCount = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(DyeDepot.MOD_ID))
                .count();

        helper.assertValueEqual(blockCount, (long) MOD_BLOCK_COUNT, "Dye Depot block registry size");
        helper.assertValueEqual(itemCount, (long) MOD_ITEM_COUNT, "Dye Depot item registry size");
        helper.assertValueEqual(DDBlocks.DYE_BASKETS.values().count(), 32L, "dye basket color count");
        helper.assertValueEqual(DDItems.DYES.values().count(), (long) CUSTOM_COLOR_COUNT, "custom dye item count");

        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();

            helper.assertValueEqual(DDItems.DYES.getOrThrow(color).components().get(DataComponents.DYE), color, color + " dye component");
            helper.assertValueEqual(((StainedGlassBlock) DDBlocks.STAINED_GLASS.getOrThrow(color)).getColor(), color, color + " glass color");
            helper.assertValueEqual(((ShulkerBoxBlock) DDBlocks.SHULKER_BOXES.getOrThrow(color)).getColor(), color, color + " shulker color");
            helper.assertValueEqual(((BannerBlock) DDBlocks.BANNERS.getOrThrow(color)).getColor(), color, color + " banner color");
            helper.assertValueEqual(((BedBlock) DDBlocks.BEDS.getOrThrow(color)).getColor(), color, color + " bed color");
            helper.assertValueEqual(((DyeBasketBlock) DDBlocks.DYE_BASKETS.getOrThrow(color)).getDyeColor(), color, color + " basket color");

            helper.assertValueEqual(DDBlocks.WOOL.getOrThrow(color).asItem(), BuiltInRegistries.ITEM.getValue(DyeDepot.modLoc(color + "_wool")), color + " wool item mapping");
            helper.assertValueEqual(DDBlocks.BANNERS.getOrThrow(color).asItem(), DDItems.BANNERS.getOrThrow(color), color + " banner item mapping");
            helper.assertValueEqual(DDBlocks.WALL_BANNERS.getOrThrow(color).asItem(), DDItems.BANNERS.getOrThrow(color), color + " wall banner item mapping");
            helper.assertTrue(BlockEntityTypes.BANNER.isValid(DDBlocks.BANNERS.getOrThrow(color).defaultBlockState()), color + " banner block entity support");
            helper.assertTrue(BlockEntityTypes.SHULKER_BOX.isValid(DDBlocks.SHULKER_BOXES.getOrThrow(color).defaultBlockState()), color + " shulker block entity support");
        }

        DyeBasketBlock basket = (DyeBasketBlock) DDBlocks.DYE_BASKETS.getOrThrow(DyeColor.WHITE);
        double basketHeight = basket.defaultBlockState()
                .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                .bounds()
                .maxY;
        helper.assertValueEqual(basketHeight, 15.0 / 16.0, "dye basket collision height");
        helper.succeed();
    }

    @GameTest
    public void glazedTerracottaMatchesVanillaPistonResolutionOnServerAndClientCarrier(GameTestHelper helper) {
        BlockState custom = DDBlocks.GLAZED_TERRACOTTA.getOrThrow(DDDyes.MAROON.get())
                .defaultBlockState()
                .setValue(GlazedTerracottaBlock.FACING, Direction.NORTH);
        BlockState carrier = DDPolymerBlocks.polymerState(custom);

        helper.assertValueEqual(custom.getPistonPushReaction(), PushReaction.PUSH_ONLY, "custom push reaction");
        helper.assertValueEqual(carrier.getPistonPushReaction(), PushReaction.PUSH_ONLY, "client carrier push reaction");
        assertGlazedPistonResolution(helper, custom, "authoritative custom block");
        assertGlazedPistonResolution(helper, carrier, "outbound Polymer carrier");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stickyPistonResendsAnUnchangedGlazedSourceAfterItsBlockEvent(GameTestHelper helper) {
        Direction direction = Direction.EAST;
        BlockPos piston = new BlockPos(2, 2, 2);
        BlockPos power = piston.relative(direction.getOpposite());
        BlockPos initialTarget = piston.relative(direction);
        BlockPos extendedTarget = piston.relative(direction, 2);
        BlockState custom = DDBlocks.GLAZED_TERRACOTTA.getOrThrow(DDDyes.MAROON.get())
                .defaultBlockState()
                .setValue(GlazedTerracottaBlock.FACING, Direction.NORTH);
        RecordingConnection connection = makeRecordingConnection(helper, helper.absolutePos(piston));

        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        helper.setBlock(initialTarget, custom);
        helper.setBlock(
                piston,
                Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, direction)
        );

        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(helper.getBlockState(extendedTarget), custom, "direct push reaches the extended position");
            helper.assertTrue(helper.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED), "sticky piston extended");
            connection.clearPackets();

            helper.setBlock(power, Blocks.AIR);
            helper.runAfterDelay(4, () -> {
                BlockPos absolutePiston = helper.absolutePos(piston);
                BlockPos absoluteSource = helper.absolutePos(extendedTarget);
                List<Packet<?>> packets = connection.packets();
                int eventIndex = -1;
                int correctionIndex = -1;
                Packet<?> correction = null;

                for (int index = 0; index < packets.size(); index++) {
                    Packet<?> packet = packets.get(index);
                    if (eventIndex < 0
                            && packet instanceof ClientboundBlockEventPacket blockEvent
                            && blockEvent.getPos().equals(absolutePiston)
                            && blockEvent.getBlock() == Blocks.STICKY_PISTON
                            && blockEvent.getB0() == PistonBaseBlock.TRIGGER_CONTRACT) {
                        eventIndex = index;
                    } else if (eventIndex >= 0
                            && packetStateAt(packet, absoluteSource) != null) {
                        correctionIndex = index;
                        correction = packet;
                        break;
                    }
                }

                helper.assertTrue(eventIndex >= 0, "sticky contraction emits its client block event");
                helper.assertTrue(
                        correctionIndex > eventIndex,
                        "the unchanged glazed source is corrected after client piston prediction"
                );
                if (correction == null) {
                    helper.fail("missing unchanged glazed-source correction packet");
                    return;
                }

                helper.assertValueEqual(helper.getBlockState(extendedTarget), custom, "sticky retraction leaves glazed terracotta in place");
                helper.assertBlockPresent(Blocks.AIR, initialTarget);

                BlockState outbound = roundTripClientboundState(
                        correction,
                        connection,
                        helper.getLevel().registryAccess(),
                        absoluteSource
                );
                helper.assertValueEqual(
                        outbound.getBlock(),
                        Blocks.GLAZED_TERRACOTTA.pick(DyeColor.ORANGE),
                        "correction packet uses the hidden native glazed carrier"
                );
                helper.assertValueEqual(
                        outbound.getValue(GlazedTerracottaBlock.FACING),
                        custom.getValue(GlazedTerracottaBlock.FACING),
                        "correction packet preserves glazed facing"
                );
                helper.assertValueEqual(
                        outbound.getPistonPushReaction(),
                        PushReaction.PUSH_ONLY,
                        "correction packet preserves vanilla glazed piston semantics"
                );
                helper.succeed();
            });
        });
    }

    @GameTest
    public void sheepRetainCustomColorsAndUseCustomLoot(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 1, 1, 1);
        DyeColor color = DDDyes.AQUA.get();

        sheep.setColor(color);
        helper.assertValueEqual(sheep.getColor(), color, "five-bit sheep color persistence");
        sheep.setSheared(true);
        helper.assertTrue(sheep.isSheared(), "moved sheep sheared bit");
        helper.assertValueEqual(sheep.getColor(), color, "color survives setting the moved sheared bit");
        sheep.setSheared(false);
        helper.assertFalse(sheep.isSheared(), "moved sheep sheared bit clears");
        helper.assertValueEqual(
                sheep.getLootTable().orElseThrow().identifier(),
                DyeDepot.modLoc("entities/sheep/aqua"),
                "custom sheep death loot route"
        );

        sheep.shear(helper.getLevel(), SoundSource.PLAYERS, new ItemStack(Items.SHEARS));
        int woolCount = helper.getEntities(EntityTypes.ITEM).stream()
                .filter(entity -> entity.getItem().is(DDBlocks.WOOL.getOrThrow(color).asItem()))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(sheep.isSheared(), "shearing updates the moved sheared bit");
        helper.assertValueInBetween(1, woolCount, 3, "custom wool shearing drop count");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void naturalSheepColorDistributionIncludesBeigeAndAqua(GameTestHelper helper) {
        int beige = 0;
        int aqua = 0;
        int samples = 50_000;

        for (int i = 0; i < samples; i++) {
            DyeColor color = Sheep.getRandomSheepColor(helper.getLevel(), BlockPos.ZERO);
            if (color == DDDyes.BEIGE.get()) {
                beige++;
            } else if (color == DDDyes.AQUA.get()) {
                aqua++;
            }
        }

        helper.assertValueInBetween(10_000, beige, 13_000, "beige natural-spawn count");
        helper.assertValueInBetween(25, aqua, 250, "aqua natural-spawn count");
        helper.succeed();
    }

    @GameTest
    public void creativeTabsContainEveryDyedFamilyInBaselineOrder(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(
                helper.getLevel().enabledFeatures(),
                false,
                helper.getLevel().registryAccess()
        );

        List<Item> ingredients = tabItems(CreativeModeTabs.INGREDIENTS);
        List<Item> coloredBlocks = tabItems(CreativeModeTabs.COLORED_BLOCKS);
        List<Item> functionalBlocks = tabItems(CreativeModeTabs.FUNCTIONAL_BLOCKS);
        List<DyeColor> customColors = COLOR_ORDER.stream().filter(DDDyes::isModDye).toList();

        List<Item> expectedIngredients = customColors.stream().map(Items.DYE::pick).toList();
        assertExactItemContents(
                helper,
                modItems(ingredients),
                expectedIngredients,
                "ingredients tab custom items"
        );
        assertColorFamilyAnchors(helper, ingredients, Items.DYE, "ingredients tab dyes");

        List<Item> expectedColoredBlocks = new ArrayList<>();
        COLOR_ORDER.stream()
                .map(DDBlocks.DYE_BASKETS::getOrThrow)
                .map(Block::asItem)
                .forEach(expectedColoredBlocks::add);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.WOOL);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.CARPET);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.DYED_TERRACOTTA);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.GLAZED_TERRACOTTA);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.CONCRETE);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.CONCRETE_POWDER);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.STAINED_GLASS);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.STAINED_GLASS_PANE);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.DYED_SHULKER_BOX);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.BED);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.DYED_CANDLE);
        appendCustomFamily(expectedColoredBlocks, customColors, Items.BANNER);
        assertExactItemContents(
                helper,
                modItems(coloredBlocks),
                expectedColoredBlocks,
                "colored blocks tab custom items"
        );

        List<Item> expectedBaskets = COLOR_ORDER.stream()
                .map(DDBlocks.DYE_BASKETS::getOrThrow)
                .map(Block::asItem)
                .toList();
        assertItemSubsequence(helper, coloredBlocks, expectedBaskets, "colored blocks tab dye baskets");
        assertItemsImmediatelyBefore(
                helper,
                coloredBlocks,
                Items.WOOL.pick(DyeColor.WHITE),
                expectedBaskets,
                "dye baskets before white wool"
        );
        assertColorFamilyOrder(helper, coloredBlocks, Items.WOOL, "colored blocks tab wool");
        assertColorFamilyOrder(helper, coloredBlocks, Items.CARPET, "colored blocks tab carpets");
        assertColorFamilyOrder(helper, coloredBlocks, Items.DYED_TERRACOTTA, "colored blocks tab terracotta");
        assertColorFamilyOrder(helper, coloredBlocks, Items.GLAZED_TERRACOTTA, "colored blocks tab glazed terracotta");
        assertColorFamilyOrder(helper, coloredBlocks, Items.CONCRETE, "colored blocks tab concrete");
        assertColorFamilyOrder(helper, coloredBlocks, Items.CONCRETE_POWDER, "colored blocks tab concrete powder");
        assertColorFamilyOrder(helper, coloredBlocks, Items.STAINED_GLASS, "colored blocks tab stained glass");
        assertColorFamilyOrder(helper, coloredBlocks, Items.STAINED_GLASS_PANE, "colored blocks tab stained glass panes");
        assertColorFamilyOrder(helper, coloredBlocks, Items.DYED_SHULKER_BOX, "colored blocks tab shulker boxes");
        assertColorFamilyOrder(helper, coloredBlocks, Items.BED, "colored blocks tab beds");
        assertColorFamilyOrder(helper, coloredBlocks, Items.DYED_CANDLE, "colored blocks tab candles");
        assertColorFamilyOrder(helper, coloredBlocks, Items.BANNER, "colored blocks tab banners");

        List<Item> expectedFunctionalBlocks = new ArrayList<>();
        appendCustomFamily(expectedFunctionalBlocks, customColors, Items.DYED_SHULKER_BOX);
        appendCustomFamily(expectedFunctionalBlocks, customColors, Items.BED);
        appendCustomFamily(expectedFunctionalBlocks, customColors, Items.DYED_CANDLE);
        appendCustomFamily(expectedFunctionalBlocks, customColors, Items.BANNER);
        assertExactItemContents(
                helper,
                modItems(functionalBlocks),
                expectedFunctionalBlocks,
                "functional blocks tab custom items"
        );
        assertColorFamilyOrder(helper, functionalBlocks, Items.DYED_SHULKER_BOX, "functional blocks tab shulker boxes");
        assertColorFamilyOrder(helper, functionalBlocks, Items.BED, "functional blocks tab beds");
        assertColorFamilyOrder(helper, functionalBlocks, Items.DYED_CANDLE, "functional blocks tab candles");
        assertColorFamilyOrder(helper, functionalBlocks, Items.BANNER, "functional blocks tab banners");
        helper.succeed();
    }

    @GameTest
    public void everyGeneratedTradeIsDecodedAndIncludedAtItsBaselineLevel(GameTestHelper helper) {
        Registry<VillagerTrade> trades = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        Set<ResourceKey<VillagerTrade>> decodedModTrades = trades.registryKeySet().stream()
                .filter(key -> key.identifier().getNamespace().equals(DyeDepot.MOD_ID))
                .collect(java.util.stream.Collectors.toSet());
        helper.assertValueEqual(decodedModTrades.size(), MOD_TRADE_COUNT, "decoded Dye Depot villager trade count");

        Set<ResourceKey<VillagerTrade>> taggedModTrades = new HashSet<>();
        assertTradeTagCount(helper, trades, VillagerTradeTags.CARTOGRAPHER_LEVEL_4, 16, taggedModTrades);
        assertTradeTagCount(helper, trades, VillagerTradeTags.MASON_LEVEL_4, 32, taggedModTrades);
        assertTradeTagCount(helper, trades, VillagerTradeTags.SHEPHERD_LEVEL_2, 37, taggedModTrades);
        assertTradeTagCount(helper, trades, VillagerTradeTags.SHEPHERD_LEVEL_3, 21, taggedModTrades);
        assertTradeTagCount(helper, trades, VillagerTradeTags.SHEPHERD_LEVEL_4, 22, taggedModTrades);
        assertTradeTagCount(helper, trades, VillagerTradeTags.WANDERING_TRADER_COMMON, 16, taggedModTrades);
        helper.assertValueEqual(taggedModTrades, decodedModTrades, "every decoded custom trade is assigned exactly once");
        helper.succeed();
    }

    @GameTest
    public void customHomePoiContainsEveryCustomBedHeadState(GameTestHelper helper) {
        Set<BlockState> expectedStates = DDBlocks.BEDS.values()
                .map(Block::getStateDefinition)
                .flatMap(definition -> definition.getPossibleStates().stream())
                .filter(state -> state.getValue(BedBlock.PART) == BedPart.HEAD)
                .collect(java.util.stream.Collectors.toSet());
        helper.assertValueEqual(expectedStates.size(), 128, "custom bed head-state count");

        Registry<PoiType> poiTypes = helper.getLevel().registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE);
        Holder.Reference<PoiType> customHome = poiTypes.getOrThrow(DDPoiTypes.HOME);
        helper.assertValueEqual(customHome.value().matchingStates(), expectedStates, "custom home POI matching states");

        for (BlockState state : expectedStates) {
            helper.assertValueEqual(PoiTypes.forState(state).orElseThrow(), customHome, "global POI lookup for " + state);
        }
        helper.succeed();
    }

    @GameTest
    public void customShulkerBoxesUseVanillaDispenserPlacement(GameTestHelper helper) {
        for (DDDyes entry : DDDyes.values()) {
            Item item = DDItems.SHULKER_BOXES.getOrThrow(entry.get());
            helper.assertTrue(
                    DispenserBlock.DISPENSER_REGISTRY.get(item) instanceof ShulkerBoxDispenseBehavior,
                    entry + " shulker box dispenser registration"
            );
        }

        DyeColor color = DDDyes.AQUA.get();
        Block expectedBlock = DDBlocks.SHULKER_BOXES.getOrThrow(color);
        Item expectedItem = DDItems.SHULKER_BOXES.getOrThrow(color);
        BlockPos dispenserPos = new BlockPos(1, 1, 1);
        helper.setBlock(
                dispenserPos,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST)
        );

        DispenserBlockEntity dispenser = helper.getBlockEntity(dispenserPos, DispenserBlockEntity.class);
        BlockState dispenserState = helper.getBlockState(dispenserPos);
        BlockSource source = new BlockSource(
                helper.getLevel(),
                helper.absolutePos(dispenserPos),
                dispenserState,
                dispenser
        );
        DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(expectedItem);
        ItemStack remainder = behavior.dispense(source, new ItemStack(expectedItem));
        BlockPos outputPos = dispenserPos.relative(dispenserState.getValue(DispenserBlock.FACING));

        helper.assertTrue(remainder.isEmpty(), "shulker placement consumes the dispensed item");
        helper.assertValueEqual(helper.getBlockState(outputPos).getBlock(), expectedBlock, "dispenser places the custom shulker box");
        helper.succeed();
    }

    @GameTest
    public void mapAndPoiIntegrationUseCustomEntries(GameTestHelper helper) {
        DyeColor color = DDDyes.MAROON.get();
        MapBanner mapBanner = new MapBanner(BlockPos.ZERO, color, Optional.empty());

        helper.assertValueEqual(
                mapBanner.getDecoration(),
                DDMapDecorationType.BANNERS.holderOrThrow(color),
                "custom map banner decoration"
        );

        var poiLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE);
        helper.assertTrue(poiLookup.getOrThrow(DDPoiTypes.HOME).is(DDTags.BEDS), "custom home POI is in the Dye Depot bed tag");
        helper.succeed();
    }

    private static List<Item> tabItems(ResourceKey<CreativeModeTab> tabKey) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tabKey).getDisplayItems().stream()
                .map(ItemStack::getItem)
                .toList();
    }

    private static void assertGlazedPistonResolution(
            GameTestHelper helper,
            BlockState glazed,
            String description
    ) {
        Direction direction = Direction.EAST;
        BlockPos piston = new BlockPos(1, 2, 1);
        BlockPos directTarget = piston.relative(direction);
        BlockPos pullTarget = piston.relative(direction, 2);
        BlockPos stickyTarget = directTarget;
        BlockPos glazedNeighbor = stickyTarget.above();
        BlockPos movableControl = stickyTarget.below();

        helper.setBlock(
                piston,
                Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, direction)
        );
        helper.setBlock(directTarget, glazed);
        PistonStructureResolver direct = new PistonStructureResolver(
                helper.getLevel(),
                helper.absolutePos(piston),
                direction,
                true
        );
        helper.assertTrue(direct.resolve(), description + " can be pushed directly");
        helper.assertTrue(
                direct.getToPush().contains(helper.absolutePos(directTarget)),
                description + " is included in a direct push"
        );

        clearPistonFixture(helper, piston, directTarget, pullTarget, glazedNeighbor, movableControl);
        helper.setBlock(
                piston,
                Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, direction)
        );
        helper.setBlock(pullTarget, glazed);
        PistonStructureResolver pull = new PistonStructureResolver(
                helper.getLevel(),
                helper.absolutePos(piston),
                direction,
                false
        );
        helper.assertFalse(pull.resolve(), description + " cannot be pulled by a sticky piston");
        helper.assertFalse(
                pull.getToPush().contains(helper.absolutePos(pullTarget)),
                description + " is excluded from sticky retraction"
        );

        for (Block stickyBlock : List.of(Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK)) {
            clearPistonFixture(helper, piston, directTarget, pullTarget, glazedNeighbor, movableControl);
            helper.setBlock(
                    piston,
                    Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, direction)
            );
            helper.setBlock(stickyTarget, stickyBlock);
            helper.setBlock(glazedNeighbor, glazed);
            helper.setBlock(movableControl, Blocks.NOTE_BLOCK);

            PistonStructureResolver lateral = new PistonStructureResolver(
                    helper.getLevel(),
                    helper.absolutePos(piston),
                    direction,
                    true
            );
            helper.assertTrue(lateral.resolve(), description + " " + stickyBlock + " structure resolves");
            helper.assertTrue(
                    lateral.getToPush().contains(helper.absolutePos(stickyTarget)),
                    stickyBlock + " is pushed"
            );
            helper.assertTrue(
                    lateral.getToPush().contains(helper.absolutePos(movableControl)),
                    "note-block control sticks to " + stickyBlock
            );
            helper.assertFalse(
                    lateral.getToPush().contains(helper.absolutePos(glazedNeighbor)),
                    description + " does not stick to " + stickyBlock
            );
        }

        clearPistonFixture(helper, piston, directTarget, pullTarget, glazedNeighbor, movableControl);
    }

    private static void clearPistonFixture(GameTestHelper helper, BlockPos... positions) {
        for (BlockPos pos : positions) {
            helper.setBlock(pos, Blocks.AIR);
        }
    }

    private static RecordingConnection makeRecordingConnection(GameTestHelper helper, BlockPos position) {
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "glaze-" + id.toString().substring(0, 8));
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                profile,
                cookie.clientInformation()
        );
        Vec3 center = Vec3.atCenterOf(position);
        player.setPos(center.x, center.y, center.z);

        RecordingConnection connection = new RecordingConnection();
        new EmbeddedChannel(connection);
        PacketContext context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, helper.getLevel().registryAccess());
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, profile);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return connection;
    }

    private static BlockState packetStateAt(Packet<?> packet, BlockPos target) {
        if (packet instanceof ClientboundBlockUpdatePacket update) {
            return update.getPos().equals(target) ? update.getBlockState() : null;
        }
        if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
            AtomicReference<BlockState> result = new AtomicReference<>();
            sectionUpdate.runUpdates((pos, state) -> {
                if (pos.equals(target)) {
                    result.set(state);
                }
            });
            return result.get();
        }
        return null;
    }

    private static BlockState roundTripClientboundState(
            Packet<?> packet,
            Connection connection,
            RegistryAccess registries,
            BlockPos target
    ) {
        if (packet instanceof ClientboundBlockUpdatePacket update) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                PacketContext.supplyWithContext(connection, () -> {
                    ClientboundBlockUpdatePacket.STREAM_CODEC.encode(buffer, update);
                    return null;
                });
                return packetStateAt(ClientboundBlockUpdatePacket.STREAM_CODEC.decode(buffer), target);
            } finally {
                buffer.release();
            }
        }
        if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                PacketContext.supplyWithContext(connection, () -> {
                    ClientboundSectionBlocksUpdatePacket.STREAM_CODEC.encode(buffer, sectionUpdate);
                    return null;
                });
                return packetStateAt(ClientboundSectionBlocksUpdatePacket.STREAM_CODEC.decode(buffer), target);
            } finally {
                buffer.release();
            }
        }
        throw new IllegalArgumentException("unsupported correction packet: " + packet.type());
    }

    private static final class RecordingConnection extends Connection {
        private final List<Packet<?>> sentPackets = new ArrayList<>();

        private RecordingConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public synchronized void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
            this.sentPackets.add(packet);
            super.send(packet, listener, flush);
        }

        private synchronized void clearPackets() {
            this.sentPackets.clear();
        }

        private synchronized List<Packet<?>> packets() {
            return List.copyOf(this.sentPackets);
        }
    }

    private static List<Item> modItems(List<Item> items) {
        return items.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(DyeDepot.MOD_ID))
                .toList();
    }

    private static void appendCustomFamily(
            List<Item> output,
            List<DyeColor> customColors,
            ColorCollection<Item> family
    ) {
        customColors.stream().map(family::pick).forEach(output::add);
    }

    private static void assertColorFamilyOrder(
            GameTestHelper helper,
            List<Item> tabItems,
            ColorCollection<Item> family,
            String description
    ) {
        List<Item> expected = COLOR_ORDER.stream().map(family::pick).toList();
        Set<Item> expectedItems = Set.copyOf(expected);
        // 26.2's functional tab repeats the vanilla white banner; compare the
        // first occurrence of each family member while exact mod-item checks
        // above continue to reject custom duplicates.
        List<Item> actual = tabItems.stream()
                .filter(expectedItems::contains)
                .distinct()
                .toList();
        helper.assertValueEqual(actual, expected, description);
        assertColorFamilyAnchors(helper, tabItems, family, description);
    }

    private static void assertColorFamilyAnchors(
            GameTestHelper helper,
            List<Item> tabItems,
            ColorCollection<Item> family,
            String description
    ) {
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.RED),
                List.of(family.pick(DDDyes.MAROON.get()), family.pick(DDDyes.ROSE.get())),
                description + " before red"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.RED),
                List.of(family.pick(DDDyes.CORAL.get())),
                description + " after red"
        );
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.ORANGE),
                List.of(family.pick(DDDyes.GINGER.get())),
                description + " before orange"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.ORANGE),
                List.of(family.pick(DDDyes.TAN.get())),
                description + " after orange"
        );
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.YELLOW),
                List.of(family.pick(DDDyes.BEIGE.get())),
                description + " before yellow"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.YELLOW),
                List.of(family.pick(DDDyes.AMBER.get()), family.pick(DDDyes.OLIVE.get())),
                description + " after yellow"
        );
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.GREEN),
                List.of(family.pick(DDDyes.FOREST.get())),
                description + " before green"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.GREEN),
                List.of(family.pick(DDDyes.VERDANT.get())),
                description + " after green"
        );
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.CYAN),
                List.of(family.pick(DDDyes.TEAL.get())),
                description + " before cyan"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.CYAN),
                List.of(family.pick(DDDyes.MINT.get()), family.pick(DDDyes.AQUA.get())),
                description + " after cyan"
        );
        assertItemsImmediatelyAfter(
                helper,
                tabItems,
                family.pick(DyeColor.BLUE),
                List.of(family.pick(DDDyes.SLATE.get()), family.pick(DDDyes.NAVY.get())),
                description + " after blue"
        );
        assertItemsImmediatelyBefore(
                helper,
                tabItems,
                family.pick(DyeColor.PURPLE),
                List.of(family.pick(DDDyes.INDIGO.get())),
                description + " before purple"
        );
    }

    private static void assertItemSubsequence(
            GameTestHelper helper,
            List<Item> tabItems,
            List<Item> expected,
            String description
    ) {
        Set<Item> expectedItems = Set.copyOf(expected);
        List<Item> actual = tabItems.stream().filter(expectedItems::contains).toList();
        helper.assertValueEqual(actual, expected, description);
    }

    private static void assertExactItemContents(
            GameTestHelper helper,
            List<Item> actual,
            List<Item> expected,
            String description
    ) {
        helper.assertValueEqual(actual.size(), expected.size(), description + " count");
        helper.assertValueEqual(Set.copyOf(actual), Set.copyOf(expected), description + " set");
    }

    private static void assertItemsImmediatelyBefore(
            GameTestHelper helper,
            List<Item> tabItems,
            Item anchor,
            List<Item> expected,
            String description
    ) {
        int anchorIndex = tabItems.indexOf(anchor);
        helper.assertTrue(anchorIndex >= expected.size(), description + " anchor and prefix exist");
        helper.assertValueEqual(
                tabItems.subList(anchorIndex - expected.size(), anchorIndex),
                expected,
                description
        );
    }

    private static void assertItemsImmediatelyAfter(
            GameTestHelper helper,
            List<Item> tabItems,
            Item anchor,
            List<Item> expected,
            String description
    ) {
        int anchorIndex = tabItems.indexOf(anchor);
        int firstAfterAnchor = anchorIndex + 1;
        helper.assertTrue(
                anchorIndex >= 0 && firstAfterAnchor + expected.size() <= tabItems.size(),
                description + " anchor and suffix exist"
        );
        helper.assertValueEqual(
                tabItems.subList(firstAfterAnchor, firstAfterAnchor + expected.size()),
                expected,
                description
        );
    }

    private static void assertTradeTagCount(
            GameTestHelper helper,
            Registry<VillagerTrade> trades,
            TagKey<VillagerTrade> tag,
            int expectedCount,
            Set<ResourceKey<VillagerTrade>> allTaggedTrades
    ) {
        int actualCount = 0;
        for (Holder<VillagerTrade> trade : trades.getTagOrEmpty(tag)) {
            ResourceKey<VillagerTrade> key = trade.unwrapKey().orElseThrow();
            if (key.identifier().getNamespace().equals(DyeDepot.MOD_ID)) {
                actualCount++;
                helper.assertTrue(allTaggedTrades.add(key), key.identifier() + " occurs in more than one tested trade tag");
            }
        }
        helper.assertValueEqual(actualCount, expectedCount, tag.location() + " custom trade count");
    }
}
