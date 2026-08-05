package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;

public final class DDPolymerLoomCapacityGameTests {
    @GameTest
    @SuppressWarnings("removal")
    public void realLoomAddsSixthLayerAndRejectsSeventh(GameTestHelper helper) {
        var patterns = helper.getLevel().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers customAuthored = new BannerPatternLayers.Builder()
                .add(patterns.getOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.WHITE)
                .add(patterns.getOrThrow(BannerPatterns.CROSS), DDDyes.MAROON.get())
                .add(patterns.getOrThrow(BannerPatterns.STRIPE_TOP), DyeColor.BLACK)
                .add(patterns.getOrThrow(BannerPatterns.CIRCLE_MIDDLE), DDDyes.ROSE.get())
                .add(patterns.getOrThrow(BannerPatterns.BORDER), DyeColor.BLUE)
                .build();
        assertFiveToSixThenRejectSeventh(
                helper,
                helper.makeMockServerPlayerInLevel(),
                DDItems.BANNERS.getOrThrow(DDDyes.TAN.get()),
                DDDyes.TAN.get(),
                customAuthored,
                DDItems.DYES.getOrThrow(DDDyes.AQUA.get()),
                DDDyes.AQUA.get(),
                "custom"
        );

        BannerPatternLayers vanillaAuthored = new BannerPatternLayers.Builder()
                .add(patterns.getOrThrow(BannerPatterns.STRIPE_BOTTOM), DyeColor.WHITE)
                .add(patterns.getOrThrow(BannerPatterns.CROSS), DyeColor.RED)
                .add(patterns.getOrThrow(BannerPatterns.STRIPE_TOP), DyeColor.BLACK)
                .add(patterns.getOrThrow(BannerPatterns.CIRCLE_MIDDLE), DyeColor.YELLOW)
                .add(patterns.getOrThrow(BannerPatterns.BORDER), DyeColor.BLUE)
                .build();
        assertFiveToSixThenRejectSeventh(
                helper,
                helper.makeMockServerPlayerInLevel(),
                Items.BANNER.pick(DyeColor.RED),
                DyeColor.RED,
                vanillaAuthored,
                Items.DYE.pick(DyeColor.LIME),
                DyeColor.LIME,
                "vanilla control"
        );
        helper.succeed();
    }

    private static void assertFiveToSixThenRejectSeventh(
            GameTestHelper helper,
            ServerPlayer player,
            Item bannerItem,
            DyeColor baseColor,
            BannerPatternLayers authoredPatterns,
            Item dyeItem,
            DyeColor dyeColor,
            String caseName
    ) {
        ItemStack banners = new ItemStack(bannerItem, 2);
        banners.set(DataComponents.BANNER_PATTERNS, authoredPatterns);
        ItemStack dyes = new ItemStack(dyeItem, 2);
        var loom = new LoomMenu(0, player.getInventory());
        loom.getBannerSlot().set(banners);
        loom.getDyeSlot().set(dyes);

        helper.assertTrue(
                !loom.getSelectablePatterns().isEmpty(),
                caseName + " exposes real Loom patterns at five layers"
        );
        var selectedPattern = loom.getSelectablePatterns().getFirst();
        helper.assertTrue(selectedPattern.unwrapKey().isPresent(), caseName + " selects a registered banner pattern");
        helper.assertTrue(loom.clickMenuButton(player, 0), caseName + " selects a sixth pattern");

        ItemStack sixLayerResult = loom.getResultSlot().getItem().copy();
        helper.assertTrue(!sixLayerResult.isEmpty(), caseName + " produces a sixth-layer result");
        helper.assertValueEqual(sixLayerResult.getCount(), 1, caseName + " result stack count");
        helper.assertValueEqual(
                sixLayerResult.getItem(),
                bannerItem,
                caseName + " result retains the exact banner item"
        );
        helper.assertTrue(sixLayerResult.getItem() instanceof BannerItem, caseName + " result remains a banner item");
        helper.assertValueEqual(
                ((BannerItem) sixLayerResult.getItem()).getColor(),
                baseColor,
                caseName + " result retains the exact banner base color"
        );

        BannerPatternLayers expectedPatterns = new BannerPatternLayers.Builder()
                .addAll(authoredPatterns)
                .add(selectedPattern, dyeColor)
                .build();
        BannerPatternLayers resultPatterns = sixLayerResult.getOrDefault(
                DataComponents.BANNER_PATTERNS,
                BannerPatternLayers.EMPTY
        );
        helper.assertValueEqual(
                resultPatterns,
                expectedPatterns,
                caseName + " preserves exact six-layer ordering and colors"
        );
        helper.assertValueEqual(
                resultPatterns.layers().size(),
                6,
                caseName + " result has exactly six authored layers"
        );
        helper.assertValueEqual(
                resultPatterns.layers().getLast().pattern(),
                selectedPattern,
                caseName + " appends the selected pattern last"
        );
        helper.assertValueEqual(
                resultPatterns.layers().getLast().color(),
                dyeColor,
                caseName + " appends the exact selected dye color"
        );

        loom.getResultSlot().onTake(player, sixLayerResult.copy());
        helper.assertValueEqual(
                loom.getBannerSlot().getItem().getCount(),
                1,
                caseName + " take consumes exactly one banner"
        );
        helper.assertValueEqual(
                loom.getBannerSlot().getItem().getItem(),
                bannerItem,
                caseName + " leaves the second banner"
        );
        helper.assertValueEqual(
                loom.getBannerSlot().getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY),
                authoredPatterns,
                caseName + " leaves the second five-layer banner unchanged"
        );
        helper.assertValueEqual(loom.getDyeSlot().getItem().getCount(), 1, caseName + " take consumes exactly one dye");
        helper.assertValueEqual(loom.getDyeSlot().getItem().getItem(), dyeItem, caseName + " leaves the second dye");

        loom.getBannerSlot().set(sixLayerResult.copy());
        helper.assertTrue(
                !loom.getSelectablePatterns().isEmpty(),
                caseName + " still has Loom buttons to reject at six layers"
        );
        helper.assertValueEqual(loom.getSelectedBannerPatternIndex(), -1, caseName + " clears selection at six layers");
        helper.assertTrue(loom.getResultSlot().getItem().isEmpty(), caseName + " clears output at six layers");
        helper.assertFalse(loom.clickMenuButton(player, 0), caseName + " rejects selecting a seventh pattern");
        helper.assertValueEqual(
                loom.getSelectedBannerPatternIndex(),
                -1,
                caseName + " rejected seventh remains unselected"
        );
        helper.assertTrue(loom.getResultSlot().getItem().isEmpty(), caseName + " rejected seventh produces no output");
        helper.assertTrue(
                ItemStack.matches(loom.getBannerSlot().getItem(), sixLayerResult),
                caseName + " rejected seventh preserves the banner"
        );
        helper.assertValueEqual(
                loom.getDyeSlot().getItem().getCount(),
                1,
                caseName + " rejected seventh consumes no dye"
        );
        helper.assertValueEqual(
                loom.getDyeSlot().getItem().getItem(),
                dyeItem,
                caseName + " rejected seventh preserves the dye item"
        );
        loom.removed(player);
    }
}
