package com.ninni.dye_depot.test;

import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;

public final class CauldronInteractionGameTests {

    private static final BlockPos CAULDRON_POS = new BlockPos(1, 1, 1);

    @GameTest
    public void customBannersAndShulkersUseVanillaWaterCauldronBehavior(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        DyeColor color = DDDyes.MAROON.get();

        var patterns = helper.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers originalPatterns = new BannerPatternLayers.Builder()
                .add(patterns.getOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.WHITE)
                .add(patterns.getOrThrow(BannerPatterns.CROSS), DyeColor.BLACK)
                .build();
        ItemStack banner = new ItemStack(DDItems.BANNERS.getOrThrow(color));
        banner.set(DataComponents.BANNER_PATTERNS, originalPatterns);

        helper.assertValueEqual(wash(helper, player, banner), InteractionResult.SUCCESS, "custom banner wash result");
        ItemStack washedBanner = player.getMainHandItem();
        helper.assertValueEqual(
                washedBanner.getItem(),
                DDItems.BANNERS.getOrThrow(color),
                "washing retains the custom banner item"
        );
        helper.assertValueEqual(
                washedBanner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                originalPatterns.removeLast(),
                "washing removes exactly the last custom-banner pattern"
        );
        assertOneCauldronLevelConsumed(helper, "custom banner washing");

        ItemContainerContents contents = ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3)));
        ItemStack shulker = new ItemStack(DDItems.SHULKER_BOXES.getOrThrow(color));
        shulker.set(DataComponents.CONTAINER, contents);

        helper.assertValueEqual(wash(helper, player, shulker), InteractionResult.SUCCESS, "custom shulker wash result");
        ItemStack washedShulker = player.getMainHandItem();
        helper.assertTrue(washedShulker.is(Items.SHULKER_BOX), "washing converts to the uncolored vanilla shulker");
        helper.assertValueEqual(washedShulker.getCount(), 1, "washing produces one uncolored shulker");
        helper.assertValueEqual(
                washedShulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY),
                contents,
                "washing preserves custom shulker contents"
        );
        assertOneCauldronLevelConsumed(helper, "custom shulker washing");
        helper.succeed();
    }

    private static InteractionResult wash(
            GameTestHelper helper,
            net.minecraft.world.entity.player.Player player,
            ItemStack stack
    ) {
        helper.setBlock(
                CAULDRON_POS,
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return CauldronInteractions.WATER.get(stack).interact(
                helper.getBlockState(CAULDRON_POS),
                helper.getLevel(),
                helper.absolutePos(CAULDRON_POS),
                player,
                InteractionHand.MAIN_HAND,
                stack
        );
    }

    private static void assertOneCauldronLevelConsumed(GameTestHelper helper, String operation) {
        helper.assertValueEqual(
                helper.getBlockState(CAULDRON_POS).getValue(LayeredCauldronBlock.LEVEL),
                2,
                operation + " consumes one water-cauldron level"
        );
    }
}
