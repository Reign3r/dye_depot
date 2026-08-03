package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.impl.networking.context.PacketContextImpl;
import net.fabricmc.fabric.mixin.networking.accessor.ServerCommonPacketListenerImplAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

public final class DDPolymerEntityGameTests {
    @GameTest
    @SuppressWarnings("removal")
    public void polymerOutboundRoundTripPreservesExactCustomItemData(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var color = DDDyes.MAROON.get();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var originalPatterns = new BannerPatternLayers.Builder().add(pattern, color).build();
        var nestedDye = new ItemStack(DDItems.DYES.getOrThrow(color));
        var userTag = new CompoundTag();
        userTag.putString("color", color.getName());
        userTag.putString("marker", "preserve_me");

        var serverStack = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        serverStack.set(DataComponents.BANNER_PATTERNS, originalPatterns);
        serverStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nestedDye)));
        serverStack.set(DataComponents.CUSTOM_DATA, CustomData.of(userTag));

        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        ItemStack clientStack = PolymerItemUtils.getPolymerItemStack(serverStack, context, registries);
        helper.assertValueEqual(
                clientStack.get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_banner_patterned"),
                "patterned banner items select the ground banner special model"
        );
        helper.assertTrue(
                clientStack.get(DataComponents.BANNER_PATTERNS).layers().stream()
                        .allMatch(layer -> layer.color().getId() < 16),
                "client-visible banner pattern colors are vanilla-codec safe"
        );
        CompoundTag carrierData = clientStack.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(
                carrierData.contains(PolymerItemUtils.POLYMER_STACK),
                "Polymer carrier retains its reserved original-stack payload"
        );
        helper.assertTrue(
                carrierData.get(PolymerItemUtils.POLYMER_STACK).toString().contains(color.getName()),
                "reserved original-stack payload retains the exact custom color"
        );
        ItemStack clientNested = clientStack.get(DataComponents.CONTAINER)
                .nonEmptyItemCopyStream().findFirst().orElseThrow();
        helper.assertTrue(clientNested.get(DataComponents.DYE).getId() < 16, "nested client dye color is codec safe");

        ItemStack restored = PolymerItemUtils.getRealItemStack(clientStack, context, registries);
        helper.assertValueEqual(restored.getItem(), serverStack.getItem(), "round trip restores the custom banner item");
        helper.assertValueEqual(
                restored.get(DataComponents.BANNER_PATTERNS),
                originalPatterns,
                "round trip restores exact custom banner patterns"
        );
        helper.assertValueEqual(
                restored.get(DataComponents.CUSTOM_DATA).copyTag(),
                userTag,
                "round trip restores opaque user custom data"
        );
        ItemStack restoredNested = restored.get(DataComponents.CONTAINER)
                .nonEmptyItemCopyStream().findFirst().orElseThrow();
        helper.assertValueEqual(
                restoredNested.getItem(),
                nestedDye.getItem(),
                "round trip restores the exact nested custom dye"
        );
        helper.assertValueEqual(
                restoredNested.get(DataComponents.DYE),
                color,
                "round trip restores the nested custom dye color"
        );

        ItemStack plainBanner = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        ItemStack plainClientStack = PolymerItemUtils.getPolymerItemStack(plainBanner, context, registries);
        helper.assertValueEqual(
                plainClientStack.get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("maroon_banner"),
                "unpatterned banner items retain their exact static custom model"
        );
        helper.succeed();
    }

    @GameTest
    public void loomAcceptsVanillaTypedPolymerCarriersForEveryCustomColor(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var context = new Connection(PacketFlow.CLIENTBOUND).getPacketContext();
        var loom = new LoomMenu(0, player.getInventory());

        for (DDDyes entry : DDDyes.values()) {
            var color = entry.get();

            Item customDye = DDItems.DYES.getOrThrow(color);
            Object dyeSynced = PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customDye);
            helper.assertTrue(dyeSynced instanceof PolymerItem, color.getName() + " dye has a Polymer overlay");
            Item dyeCarrier = ((PolymerItem) dyeSynced).getPolymerItem(new ItemStack(customDye), context);
            helper.assertTrue(
                    loom.getDyeSlot().mayPlace(new ItemStack(dyeCarrier)),
                    color.getName() + " dye carrier is accepted by the Loom dye slot"
            );

            Item customBanner = DDItems.BANNERS.getOrThrow(color);
            Object bannerSynced = PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, customBanner);
            helper.assertTrue(bannerSynced instanceof PolymerItem, color.getName() + " banner has a Polymer overlay");
            Item bannerCarrier = ((PolymerItem) bannerSynced).getPolymerItem(new ItemStack(customBanner), context);
            helper.assertTrue(
                    bannerCarrier instanceof BannerItem,
                    color.getName() + " banner carrier is a BannerItem for Loom UI type safety"
            );
            helper.assertTrue(
                    loom.getBannerSlot().mayPlace(new ItemStack(bannerCarrier)),
                    color.getName() + " banner carrier is accepted by the Loom banner slot"
            );
        }

        loom.removed(player);
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void placedBannerDisplaysTrackPatternAddRemoveAndWallAttachment(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var color = DDDyes.MAROON.get();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var patterns = new BannerPatternLayers.Builder().add(pattern, color).build();

        BlockPos standingRelative = new BlockPos(1, 2, 1);
        BlockPos standingPos = helper.absolutePos(standingRelative);
        var standingState = DDBlocks.BANNERS.getOrThrow(color).defaultBlockState();
        helper.setBlock(standingRelative, standingState);
        var standingEntity = (BannerBlockEntity) level.getBlockEntity(standingPos);
        var standingAttachment = BlockBoundAttachment.get(level, standingPos);
        helper.assertTrue(standingAttachment != null, "standing banner has a live Polymer block attachment");
        var standingHolder = standingAttachment.holder();
        var standingDisplay = (ItemDisplayElement) standingHolder.getElements().getFirst();

        helper.assertTrue(
                standingDisplay.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().isEmpty(),
                "unpatterned placed banners start on the exact static model"
        );
        helper.assertValueEqual(
                standingDisplay.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_banner_plain"),
                "unpatterned standing banner combines native geometry with its exact custom cloth color"
        );
        ItemStack unwatchedStack = standingDisplay.getItem();
        var patternedStanding = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        patternedStanding.set(DataComponents.BANNER_PATTERNS, patterns);
        standingEntity.applyComponentsFromItemStack(patternedStanding);
        standingEntity.setChanged();
        helper.assertValueEqual(
                standingEntity.getPatterns(),
                patterns,
                "standing banner block entity accepts the added pattern layers"
        );
        standingHolder.tick();
        helper.assertTrue(
                standingDisplay.getItem() == unwatchedStack,
                "an unwatched banner tick performs no stack allocation or replacement"
        );
        helper.assertTrue(
                standingDisplay.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().isEmpty(),
                "unwatched banner defers block-entity refresh"
        );

        var player = helper.makeMockServerPlayerInLevel();
        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, level.getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        standingHolder.startWatching(player);
        helper.assertTrue(
                !standingHolder.getWatchingPlayers().isEmpty(),
                "standing banner has a tracked viewer before its deferred refresh"
        );
        standingHolder.tick();
        helper.assertValueEqual(
                standingDisplay.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_banner_patterned"),
                "standing patterned banner selects the ground attachment model"
        );
        helper.assertValueEqual(
                standingDisplay.getItem().get(DataComponents.BANNER_PATTERNS),
                patterns,
                "standing display reads exact block-entity pattern layers"
        );
        ItemStack cachedPatternedStack = standingDisplay.getItem();
        standingHolder.tick();
        helper.assertTrue(
                standingDisplay.getItem() == cachedPatternedStack,
                "unchanged watched patterns reuse the cached display stack"
        );

        standingEntity.applyComponentsFromItemStack(new ItemStack(DDItems.BANNERS.getOrThrow(color)));
        standingEntity.setChanged();
        standingHolder.tick();
        helper.assertTrue(
                standingDisplay.getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().isEmpty(),
                "removing block-entity patterns clears the dynamic layers"
        );
        helper.assertValueEqual(
                standingDisplay.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_banner_plain"),
                "removing patterns restores the exact custom-color vanilla-style renderer"
        );

        BlockPos wallRelative = new BlockPos(3, 2, 1);
        BlockPos wallPos = helper.absolutePos(wallRelative);
        var wallState = DDBlocks.WALL_BANNERS.getOrThrow(color).defaultBlockState();
        helper.setBlock(wallRelative, wallState);
        var wallEntity = (BannerBlockEntity) level.getBlockEntity(wallPos);
        wallEntity.applyComponentsFromItemStack(patternedStanding);
        wallEntity.setChanged();
        var wallAttachment = BlockBoundAttachment.get(level, wallPos);
        helper.assertTrue(wallAttachment != null, "wall banner has a live Polymer block attachment");
        var wallHolder = wallAttachment.holder();
        wallHolder.startWatching(player);
        wallHolder.tick();
        var wallDisplay = (ItemDisplayElement) wallHolder.getElements().getFirst();
        helper.assertValueEqual(
                wallDisplay.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_wall_banner_patterned"),
                "wall patterned banner selects the wall attachment model"
        );

        ItemStack clientWall = PolymerItemUtils.getPolymerItemStack(wallDisplay.getItem(), context, registries);
        helper.assertValueEqual(
                clientWall.get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_wall_banner_patterned"),
                "explicit wall model survives Polymer item conversion"
        );
        helper.assertTrue(
                clientWall.get(DataComponents.BANNER_PATTERNS).layers().stream()
                        .allMatch(layer -> layer.color().getId() < 16),
                "placed wall banner layers are vanilla-codec safe"
        );

        standingHolder.destroy();
        wallHolder.destroy();
        helper.succeed();
    }

    @GameTest
    public void bannerPatternLookupNeverLoadsAnAbsentChunk(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos absentPos = helper.absolutePos(new BlockPos(4096, 2, 4096));
        int chunkX = absentPos.getX() >> 4;
        int chunkZ = absentPos.getZ() >> 4;

        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "regression target starts outside every loaded chunk"
        );
        helper.assertValueEqual(
                DDPolymerBlocks.StateDisplayHolder.loadedBannerPatterns(level, absentPos),
                BannerPatternLayers.EMPTY,
                "an unloaded banner position has no visible patterns"
        );
        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "banner pattern inspection must not synchronously load its chunk"
        );
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void shulkerDisplayTracksNativeLidAnimationCollisionAndLighting(GameTestHelper helper) {
        var level = helper.getLevel();
        var color = DDDyes.ROSE.get();
        BlockPos relative = new BlockPos(1, 2, 1);
        BlockPos pos = helper.absolutePos(relative);
        var state = DDBlocks.SHULKER_BOXES.getOrThrow(color).defaultBlockState();
        helper.setBlock(relative, state);

        var entity = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
        var attachment = BlockBoundAttachment.get(level, pos);
        helper.assertTrue(attachment != null, "custom shulker has a live Polymer block attachment");
        var holder = attachment.holder();
        var display = (ItemDisplayElement) holder.getElements().getFirst();
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_box_0"),
                "closed shulker starts on the native closed special renderer"
        );

        var player = helper.makeMockServerPlayerInLevel();
        var connection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = connection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, level.registryAccess());
        context.set(PacketContextImpl.SERVER_INSTANCE, level.getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        holder.startWatching(player);

        entity.triggerEvent(1, 1);
        for (int tick = 0; tick < 5; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_box_5"),
                "opening shulker advances through the native special-renderer lid frames"
        );
        helper.assertTrue(entity.getBoundingBox(state).maxY > 1.0, "opening lid expands the authoritative collision box");
        helper.assertTrue(display.getBrightness() != null, "shulker display uses surrounding light instead of sampling inside its opaque carrier");

        for (int tick = 5; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_box_10"),
                "fully open shulker reaches the native fully-open special renderer"
        );

        entity.triggerEvent(1, 0);
        for (int tick = 0; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_box_0"),
                "closing shulker returns to the native closed special renderer"
        );
        holder.destroy();
        helper.succeed();
    }

    @GameTest
    public void candleDonorUsesNativeParticlesWhileCandleCakeUsesServerParticles(GameTestHelper helper) {
        Map<Integer, List<Vec3>> expected = Map.of(
                1, List.of(new Vec3(8 / 16.0, 8 / 16.0, 8 / 16.0)),
                2, List.of(new Vec3(6 / 16.0, 7 / 16.0, 8 / 16.0), new Vec3(10 / 16.0, 8 / 16.0, 7 / 16.0)),
                3, List.of(
                        new Vec3(8 / 16.0, 5 / 16.0, 10 / 16.0),
                        new Vec3(6 / 16.0, 7 / 16.0, 8 / 16.0),
                        new Vec3(9 / 16.0, 8 / 16.0, 7 / 16.0)
                ),
                4, List.of(
                        new Vec3(7 / 16.0, 5 / 16.0, 9 / 16.0),
                        new Vec3(10 / 16.0, 7 / 16.0, 9 / 16.0),
                        new Vec3(6 / 16.0, 7 / 16.0, 6 / 16.0),
                        new Vec3(9 / 16.0, 8 / 16.0, 6 / 16.0)
                )
        );
        var candle = DDBlocks.CANDLES.getOrThrow(DDDyes.MAROON.get());
        for (int count = 1; count <= 4; count++) {
            var unlit = candle.defaultBlockState()
                    .setValue(CandleBlock.CANDLES, count)
                    .setValue(CandleBlock.LIT, false);
            var lit = unlit.setValue(CandleBlock.LIT, true);
            var overlay = BlockWithElementHolder.get(lit);
            helper.assertTrue(
                    !overlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, unlit),
                    count + " unlit candle relies on its native donor state and does not server-tick particles"
            );
            helper.assertTrue(
                    !overlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, lit),
                    count + " lit candle relies on native donor particles without duplicates"
            );
            helper.assertValueEqual(
                    DDPolymerBlocks.candleParticleOffsets(lit),
                    expected.get(count),
                    count + " candle flame offsets"
            );
            var holder = overlay.createElementHolder(helper.getLevel(), BlockPos.ZERO, lit);
            var display = (ItemDisplayElement) holder.getElements().getFirst();
            helper.assertTrue(
                    display.getBrightness() == null,
                    count + " lit candle display samples world light instead of forcing 15/15"
            );
            holder.destroy();
        }

        var cake = DDBlocks.CANDLE_CAKES.getOrThrow(DDDyes.MAROON.get()).defaultBlockState();
        var litCake = cake.setValue(CandleCakeBlock.LIT, true);
        var cakeOverlay = BlockWithElementHolder.get(litCake);
        helper.assertTrue(
                cakeOverlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, cake),
                "initially unlit candle cake remains tick-enabled for a later lit transition"
        );
        helper.assertTrue(
                cakeOverlay.tickElementHolder(helper.getLevel(), BlockPos.ZERO, litCake),
                "lit candle-cake display ticks"
        );
        helper.assertValueEqual(
                DDPolymerBlocks.candleParticleOffsets(litCake),
                List.of(new Vec3(8 / 16.0, 16 / 16.0, 8 / 16.0)),
                "candle-cake flame offset"
        );
        var cakeHolder = cakeOverlay.createElementHolder(helper.getLevel(), BlockPos.ZERO, litCake);
        var cakeDisplay = (ItemDisplayElement) cakeHolder.getElements().getFirst();
        helper.assertTrue(
                cakeDisplay.getBrightness() == null,
                "lit candle-cake display samples world light instead of forcing 15/15"
        );
        cakeHolder.destroy();
        helper.succeed();
    }

    @GameTest
    public void customSheepCoatTracksStateAndCleansUpWithItsEntity(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        sheep.setColor(DDDyes.MAROON.get());

        PolymerEntity overlay = PolymerEntity.get(sheep);
        ElementHolder holder = DDPolymerEntities.sheepWoolHolder(overlay);
        helper.assertTrue(holder != null, "custom sheep overlay creates a wool element holder");
        helper.assertValueEqual(holder.getElements().size(), 1, "custom sheep wool element count");

        ItemDisplayElement coat = (ItemDisplayElement) holder.getElements().getFirst();
        holder.tick();
        helper.assertFalse(coat.getItem().isEmpty(), "unsheared custom sheep displays its exact-color coat");
        helper.assertValueEqual(
                coat.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_sheep_wool"),
                "custom sheep coat model"
        );

        sheep.setSheared(true);
        holder.tick();
        helper.assertTrue(coat.getItem().isEmpty(), "shearing hides the virtual wool coat");

        sheep.setSheared(false);
        holder.tick();
        helper.assertFalse(coat.getItem().isEmpty(), "wool regrowth restores the virtual coat");

        HolderAttachment attachment = holder.getAttachment();
        helper.assertTrue(attachment != null && !attachment.isRemoved(), "wool holder starts attached");
        sheep.discard();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(attachment.isRemoved(), "removing the sheep destroys its virtual coat attachment");
            helper.assertTrue(holder.getAttachment() == null, "destroyed coat holder releases its attachment");
            helper.succeed();
        });
    }
}
