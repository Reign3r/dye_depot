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
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
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
        helper.assertValueEqual(
                clientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "client banner prepends its exact full-cloth visual base"
        );
        helper.assertTrue(
                !clientStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
                        .shows(DataComponents.BANNER_PATTERNS),
                "the synthetic base is hidden from the normal banner-pattern tooltip"
        );
        helper.assertValueEqual(
                clientStack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().size(),
                1,
                "the authored pattern keeps one visible tooltip line"
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
                "unpatterned banner items use their native animated banner model"
        );
        helper.assertValueEqual(
                plainClientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "plain banner items receive the same exact animated base layer"
        );
        helper.assertTrue(
                plainClientStack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().isEmpty(),
                "plain banners do not expose a synthetic tooltip line"
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
    public void loomRetainsAllSixAuthoredPatternSlotsForCustomBanners(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var registries = helper.getLevel().registryAccess();
        var pattern = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
        var color = DDDyes.MAROON.get();
        var builder = new BannerPatternLayers.Builder();
        for (int index = 0; index < 5; index++) {
            builder.add(pattern, color);
        }
        var authoredPatterns = builder.build();
        var serverStack = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        serverStack.set(DataComponents.BANNER_PATTERNS, authoredPatterns);
        var loom = new LoomMenu(0, player.getInventory());
        loom.getBannerSlot().set(serverStack);
        player.containerMenu = loom;
        ItemStack loomInput = loom.getBannerSlot().getItem();

        var playerConnection = ((ServerCommonPacketListenerImplAccessor) player.connection).getConnection();
        var context = playerConnection.getPacketContext();
        context.set(PacketContextImpl.REGISTRY_ACCESS, registries);
        context.set(PacketContextImpl.SERVER_INSTANCE, helper.getLevel().getServer());
        context.set(PacketContextImpl.GAME_PROFILE, player.getGameProfile());
        ItemStack clientStack = PolymerItemUtils.getPolymerItemStack(loomInput, context, registries);

        var clientPatterns = clientStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        helper.assertValueEqual(
                clientPatterns.layers().size(),
                5,
                "five authored layers remain five client layers so the Loom offers a sixth"
        );
        helper.assertTrue(
                clientPatterns.layers().stream().noneMatch(layer -> layer.pattern().unwrapKey()
                        .map(key -> key.identifier().getNamespace().equals(DyeDepot.MOD_ID))
                        .orElse(false)),
                "the Loom-capacity fallback omits the synthetic visual base"
        );
        helper.assertValueEqual(
                loomInput.get(DataComponents.BANNER_PATTERNS),
                authoredPatterns,
                "Loom-safe client conversion does not mutate authored server patterns"
        );

        ItemStack normalClientStack = PolymerItemUtils.getPolymerItemStack(loomInput.copy(), context, registries);
        helper.assertValueEqual(
                normalClientStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .layers().size(),
                6,
                "the same five-pattern banner keeps its exact synthetic base outside the Loom input slot"
        );
        helper.assertValueEqual(
                normalClientStack.get(DataComponents.BANNER_PATTERNS).layers().getFirst()
                        .pattern().unwrapKey().orElseThrow().identifier(),
                DyeDepot.modLoc("polymer_base_maroon"),
                "normal five-pattern item icons retain the exact custom base"
        );
        loom.removed(player);
        player.containerMenu = player.inventoryMenu;
        helper.succeed();
    }

    @GameTest
    @SuppressWarnings("removal")
    public void placedBannersUseNativeCarriersAndExactOutboundBaseLayers(GameTestHelper helper) {
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
        helper.assertValueEqual(
                DDPolymerBlocks.polymerState(standingState).getBlock(),
                Blocks.BANNER.pick(DDPolymerColors.vanillaColor(color)),
                "standing banners use a native targetable banner carrier"
        );
        helper.assertTrue(
                BlockBoundAttachment.get(level, standingPos) == null,
                "native banner rendering does not allocate a duplicate display entity"
        );

        var patternedStanding = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        patternedStanding.set(DataComponents.BANNER_PATTERNS, patterns);
        standingEntity.applyComponentsFromItemStack(patternedStanding);
        standingEntity.setChanged();
        helper.assertValueEqual(
                standingEntity.getPatterns(),
                patterns,
                "standing banner block entity accepts the added pattern layers"
        );
        CompoundTag standingTag = standingEntity.getUpdateTag(registries);
        var standingLayers = (net.minecraft.nbt.ListTag) standingTag.get("patterns");
        helper.assertValueEqual(standingLayers.size(), 2, "outbound standing banner keeps base plus authored layer");
        helper.assertValueEqual(
                ((CompoundTag) standingLayers.getFirst()).getStringOr("pattern", ""),
                "dye_depot:polymer_base_maroon",
                "standing banner update tag starts with its exact visual base"
        );
        helper.assertValueEqual(
                standingEntity.getPatterns(),
                patterns,
                "client-only visual base never mutates server pattern data"
        );

        BlockPos wallRelative = new BlockPos(3, 2, 1);
        BlockPos wallPos = helper.absolutePos(wallRelative);
        var wallState = DDBlocks.WALL_BANNERS.getOrThrow(color).defaultBlockState();
        helper.setBlock(wallRelative, wallState);
        var wallEntity = (BannerBlockEntity) level.getBlockEntity(wallPos);
        wallEntity.applyComponentsFromItemStack(patternedStanding);
        wallEntity.setChanged();
        helper.assertValueEqual(
                DDPolymerBlocks.polymerState(wallState).getBlock(),
                Blocks.WALL_BANNER.pick(DDPolymerColors.vanillaColor(color)),
                "wall banners use a native wall-banner carrier"
        );
        helper.assertValueEqual(
                ((CompoundTag) ((net.minecraft.nbt.ListTag) wallEntity.getUpdateTag(registries).get("patterns"))
                        .getFirst()).getStringOr("pattern", ""),
                "dye_depot:polymer_base_maroon",
                "wall banner update tag starts with the same exact visual base"
        );
        helper.assertTrue(
                BlockBoundAttachment.get(level, wallPos) == null,
                "wall banners also avoid duplicate display entities"
        );
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
        var lid = (ItemDisplayElement) holder.getElements().get(1);
        helper.assertValueEqual(
                display.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_base"),
                "shulker base uses the exact split shell model"
        );
        helper.assertValueEqual(
                lid.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/rose_shulker_lid"),
                "shulker lid uses the independently animated shell model"
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
        helper.assertTrue(Math.abs(lid.getTranslation().y() - 0.25f) < 0.001f, "half-open lid reaches half its travel");
        helper.assertValueEqual(lid.getInterpolationDuration(), 1, "lid interpolates every server-tick transform on the client");
        helper.assertTrue(entity.getBoundingBox(state).maxY > 1.0, "opening lid expands the authoritative collision box");
        helper.assertTrue(display.getBrightness() != null, "shulker display uses surrounding light instead of sampling inside its opaque carrier");

        for (int tick = 5; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertTrue(Math.abs(lid.getTranslation().y() - 0.5f) < 0.001f, "fully open lid reaches vanilla eight-pixel travel");

        entity.triggerEvent(1, 0);
        for (int tick = 0; tick < 10; tick++) {
            ShulkerBoxBlockEntity.tick(level, pos, state, entity);
            holder.tick();
        }
        helper.assertTrue(Math.abs(lid.getTranslation().y()) < 0.001f, "closing shulker returns its lid to the closed position");
        holder.destroy();
        helper.succeed();
    }

    @GameTest
    public void adjacentPaneDisplaysCompensateItemModelOrientation(GameTestHelper helper) {
        var level = helper.getLevel();
        var pane = DDBlocks.STAINED_GLASS_PANES.getOrThrow(DDDyes.MAROON.get());
        BlockPos leftRelative = new BlockPos(1, 2, 1);
        BlockPos rightRelative = leftRelative.east();
        helper.setBlock(leftRelative, pane.defaultBlockState());
        helper.setBlock(rightRelative, pane.defaultBlockState());

        var leftHolder = BlockBoundAttachment.get(level, helper.absolutePos(leftRelative)).holder();
        var rightHolder = BlockBoundAttachment.get(level, helper.absolutePos(rightRelative)).holder();
        var left = (ItemDisplayElement) leftHolder.getElements().getFirst();
        var right = (ItemDisplayElement) rightHolder.getElements().getFirst();
        helper.assertValueEqual(
                left.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_pane_2"),
                "left pane selects its east-connected model"
        );
        helper.assertValueEqual(
                right.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/maroon_pane_8"),
                "right pane selects its west-connected model"
        );
        helper.assertTrue(Math.abs(left.getYaw() - 180.0f) < 0.001f, "left pane compensates item-display orientation");
        helper.assertTrue(Math.abs(right.getYaw() - 180.0f) < 0.001f, "right pane compensates item-display orientation");

        leftHolder.destroy();
        rightHolder.destroy();
        helper.succeed();
    }

    @GameTest
    public void brownShulkerDonorHasVisibleAnimatedOverlay(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, Blocks.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.BROWN));
        var holder = BlockBoundAttachment.get(level, helper.absolutePos(relative)).holder();
        helper.assertValueEqual(holder.getElements().size(), 2, "brown donor has restored base and lid displays");
        var base = (ItemDisplayElement) holder.getElements().getFirst();
        var lid = (ItemDisplayElement) holder.getElements().get(1);
        helper.assertValueEqual(
                base.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/donor_brown_shulker_base"),
                "brown donor base uses its restored texture"
        );
        helper.assertValueEqual(
                lid.getItem().get(DataComponents.ITEM_MODEL),
                DyeDepot.modLoc("polymer/donor_brown_shulker_lid"),
                "brown donor lid uses its restored texture"
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
