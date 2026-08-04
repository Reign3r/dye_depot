package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DDPolymerSignCollarGameTests {
    private static final BlockPos STANDING_SIGN_POS = new BlockPos(2, 2, 2);
    private static final BlockPos HANGING_SIGN_POS = new BlockPos(5, 2, 2);
    private static final BlockPos CAT_POS = new BlockPos(1, 2, 1);
    private static final BlockPos WOLF_POS = new BlockPos(3, 2, 1);
    private static final BlockPos UNTAMED_CAT_POS = new BlockPos(5, 2, 1);
    private static final BlockPos UNTAMED_WOLF_POS = new BlockPos(7, 2, 1);

    @GameTest
    public void customDyesAndGlowInkUseRealInteractionOnBothSignKindsAndFaces(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        SignBlockEntity standing = placeSign(helper, STANDING_SIGN_POS, Blocks.OAK_SIGN);
        SignBlockEntity hanging = placeSign(helper, HANGING_SIGN_POS, Blocks.OAK_HANGING_SIGN);

        exerciseSign(helper, player, standing, "standing sign");
        exerciseSign(helper, player, hanging, "hanging sign");

        helper.succeed();
    }

    @GameTest
    public void allCustomCollarDyesHonorOwnershipConsumptionNegativesAndPersistence(GameTestHelper helper) {
        Player owner = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Player nonOwner = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Cat cat = helper.spawn(EntityTypes.CAT, CAT_POS);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, WOLF_POS);
        Cat untamedCat = helper.spawn(EntityTypes.CAT, UNTAMED_CAT_POS);
        Wolf untamedWolf = helper.spawn(EntityTypes.WOLF, UNTAMED_WOLF_POS);
        cat.tame(owner);
        wolf.tame(owner);

        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();
            assertUnauthorizedInteractionLeavesCollarAlone(
                    helper,
                    nonOwner,
                    cat,
                    color,
                    cat.getCollarColor(),
                    "non-owner cat"
            );
            assertUnauthorizedInteractionLeavesCollarAlone(
                    helper,
                    nonOwner,
                    wolf,
                    color,
                    wolf.getCollarColor(),
                    "non-owner wolf"
            );
            assertUnauthorizedInteractionLeavesCollarAlone(
                    helper,
                    owner,
                    untamedCat,
                    color,
                    untamedCat.getCollarColor(),
                    "untamed cat"
            );
            assertUnauthorizedInteractionLeavesCollarAlone(
                    helper,
                    owner,
                    untamedWolf,
                    color,
                    untamedWolf.getCollarColor(),
                    "untamed wolf"
            );

            applyOwnedCollarDye(helper, owner, cat, color, "cat");
            applyOwnedCollarDye(helper, owner, wolf, color, "wolf");

            Cat restoredCat = roundTripEntity(helper, cat, EntityTypes.CAT);
            Wolf restoredWolf = roundTripEntity(helper, wolf, EntityTypes.WOLF);
            helper.assertTrue(restoredCat.isTame(), color.getName() + " cat remains tame after serialization");
            helper.assertTrue(restoredWolf.isTame(), color.getName() + " wolf remains tame after serialization");
            helper.assertValueEqual(
                    restoredCat.getCollarColor(),
                    color,
                    color.getName() + " cat collar survives serialization exactly"
            );
            helper.assertValueEqual(
                    restoredWolf.getCollarColor(),
                    color,
                    color.getName() + " wolf collar survives serialization exactly"
            );

            assertSameColorIsNotConsumed(helper, owner, cat, color, "cat");
            assertSameColorIsNotConsumed(helper, owner, wolf, color, "wolf");
        }

        helper.succeed();
    }

    private static SignBlockEntity placeSign(GameTestHelper helper, BlockPos relativePos, Block block) {
        helper.setBlock(relativePos, block.defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
        helper.assertTrue(blockEntity instanceof SignBlockEntity, block.getName().getString() + " creates sign data");
        SignBlockEntity sign = (SignBlockEntity) blockEntity;
        sign.setText(new SignText().setMessage(0, Component.literal("front text")), true);
        sign.setText(new SignText().setMessage(0, Component.literal("back text")), false);
        return sign;
    }

    private static void exerciseSign(
            GameTestHelper helper,
            Player player,
            SignBlockEntity sign,
            String description
    ) {
        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();
            sign.setText(sign.getFrontText().setColor(DyeColor.BLACK).setHasGlowingText(false), true);
            sign.setText(sign.getBackText().setColor(DyeColor.BLACK).setHasGlowingText(false), false);

            interactWithSign(helper, player, sign, true, customDye(color), description + " front dye");
            assertSignFace(helper, sign, true, color, false, description + " front");
            assertSignFace(helper, sign, false, DyeColor.BLACK, false, description + " untouched back");
            assertOutboundSignRgb(helper, sign, true, color, false, description + " front non-glowing");

            interactWithSign(
                    helper,
                    player,
                    sign,
                    true,
                    new ItemStack(Items.GLOW_INK_SAC, 2),
                    description + " front glow"
            );
            assertSignFace(helper, sign, true, color, true, description + " glowing front");
            assertOutboundSignRgb(helper, sign, true, color, true, description + " front glowing");

            interactWithSign(helper, player, sign, false, customDye(color), description + " back dye");
            assertSignFace(helper, sign, false, color, false, description + " back");
            assertSignFace(helper, sign, true, color, true, description + " preserved front");
            assertOutboundSignRgb(helper, sign, false, color, false, description + " back non-glowing");

            interactWithSign(
                    helper,
                    player,
                    sign,
                    false,
                    new ItemStack(Items.GLOW_INK_SAC, 2),
                    description + " back glow"
            );
            assertSignFace(helper, sign, false, color, true, description + " glowing back");
            assertOutboundSignRgb(helper, sign, false, color, true, description + " back glowing");

            SignBlockEntity restored = roundTripSign(helper, sign);
            assertSignFace(helper, restored, true, color, true, description + " restored front");
            assertSignFace(helper, restored, false, color, true, description + " restored back");
        }
    }

    private static ItemStack customDye(DyeColor color) {
        return new ItemStack(DDItems.DYES.getOrThrow(color), 2);
    }

    private static void interactWithSign(
            GameTestHelper helper,
            Player player,
            SignBlockEntity sign,
            boolean front,
            ItemStack stack,
            String description
    ) {
        BlockPos pos = sign.getBlockPos();
        BlockState state = sign.getBlockState();
        positionPlayerAtFace(helper, player, sign, front, description);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = state.useItemOn(
                stack,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), player.getDirection().getOpposite(), pos, false)
        );
        helper.assertTrue(result.consumesAction(), description + " succeeds through SignBlock.useItemOn");
        helper.assertValueEqual(
                player.getItemInHand(InteractionHand.MAIN_HAND).getCount(),
                1,
                description + " consumes exactly one item in survival"
        );
    }

    private static void positionPlayerAtFace(
            GameTestHelper helper,
            Player player,
            SignBlockEntity sign,
            boolean front,
            String description
    ) {
        Vec3 center = Vec3.atCenterOf(sign.getBlockPos());
        Vec3[] candidates = {
                center.add(0.0, 0.0, 2.0),
                center.add(0.0, 0.0, -2.0),
                center.add(2.0, 0.0, 0.0),
                center.add(-2.0, 0.0, 0.0)
        };
        for (Vec3 candidate : candidates) {
            player.setPos(candidate.x, candidate.y, candidate.z);
            if (sign.isFacingFrontText(player) == front) {
                return;
            }
        }
        helper.fail(description + " could not resolve the requested sign face");
    }

    private static void assertSignFace(
            GameTestHelper helper,
            SignBlockEntity sign,
            boolean front,
            DyeColor color,
            boolean glowing,
            String description
    ) {
        SignText text = sign.getText(front);
        helper.assertValueEqual(text.getColor(), color, description + " retains exact authoritative color");
        helper.assertValueEqual(text.hasGlowingText(), glowing, description + " glow state");
    }

    private static void assertOutboundSignRgb(
            GameTestHelper helper,
            SignBlockEntity sign,
            boolean front,
            DyeColor color,
            boolean glowing,
            String description
    ) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag outbound = DDPolymerBlockEntityNbt.sanitize(
                sign.getType(),
                sign.getUpdateTag(registries),
                registries
        );
        String sectionName = front ? "front_text" : "back_text";
        helper.assertTrue(outbound.get(sectionName) instanceof CompoundTag, description + " has serialized text");
        CompoundTag section = (CompoundTag) outbound.get(sectionName);
        SignText decoded = SignText.DIRECT_CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), section)
                .getOrThrow();
        int expectedRgb = glowing ? color.getTextColor() : ARGB.scaleRGB(color.getTextColor(), 0.4F);
        helper.assertValueEqual(
                decoded.getMessage(0, false).getStyle().getColor().getValue(),
                expectedRgb & 0xFFFFFF,
                description + " sends exact custom glyph RGB"
        );
        helper.assertValueEqual(decoded.hasGlowingText(), glowing, description + " sends native glow state");
        helper.assertTrue(decoded.getColor().getId() < 16, description + " uses a vanilla-codec-safe outline carrier");
        helper.assertValueEqual(
                sign.getText(front).getColor(),
                color,
                description + " outbound conversion does not mutate server color"
        );
    }

    private static SignBlockEntity roundTripSign(GameTestHelper helper, SignBlockEntity sign) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = sign.saveWithFullMetadata(registries);
        BlockEntity restored = BlockEntity.loadStatic(sign.getBlockPos(), sign.getBlockState(), saved, registries);
        helper.assertTrue(restored instanceof SignBlockEntity, "serialized sign loads with its concrete type");
        return (SignBlockEntity) restored;
    }

    private static void applyOwnedCollarDye(
            GameTestHelper helper,
            Player owner,
            Cat cat,
            DyeColor color,
            String description
    ) {
        ItemStack dye = customDye(color);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.assertTrue(
                cat.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction(),
                color.getName() + " colors an owned " + description
        );
        helper.assertValueEqual(cat.getCollarColor(), color, color.getName() + " " + description + " collar color");
        helper.assertValueEqual(dye.getCount(), 1, color.getName() + " " + description + " consumes one dye");
    }

    private static void applyOwnedCollarDye(
            GameTestHelper helper,
            Player owner,
            Wolf wolf,
            DyeColor color,
            String description
    ) {
        ItemStack dye = customDye(color);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.assertTrue(
                wolf.mobInteract(owner, InteractionHand.MAIN_HAND).consumesAction(),
                color.getName() + " colors an owned " + description
        );
        helper.assertValueEqual(wolf.getCollarColor(), color, color.getName() + " " + description + " collar color");
        helper.assertValueEqual(dye.getCount(), 1, color.getName() + " " + description + " consumes one dye");
    }

    private static void assertUnauthorizedInteractionLeavesCollarAlone(
            GameTestHelper helper,
            Player player,
            Cat cat,
            DyeColor attemptedColor,
            DyeColor expectedColor,
            String description
    ) {
        ItemStack dye = customDye(attemptedColor);
        player.setItemInHand(InteractionHand.MAIN_HAND, dye);
        cat.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(cat.getCollarColor(), expectedColor, description + " cannot change collar color");
        helper.assertValueEqual(dye.getCount(), 2, description + " does not consume dye");
    }

    private static void assertUnauthorizedInteractionLeavesCollarAlone(
            GameTestHelper helper,
            Player player,
            Wolf wolf,
            DyeColor attemptedColor,
            DyeColor expectedColor,
            String description
    ) {
        ItemStack dye = customDye(attemptedColor);
        player.setItemInHand(InteractionHand.MAIN_HAND, dye);
        wolf.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(wolf.getCollarColor(), expectedColor, description + " cannot change collar color");
        helper.assertValueEqual(dye.getCount(), 2, description + " does not consume dye");
    }

    private static void assertSameColorIsNotConsumed(
            GameTestHelper helper,
            Player owner,
            Cat cat,
            DyeColor color,
            String description
    ) {
        ItemStack dye = customDye(color);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        cat.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(cat.getCollarColor(), color, description + " same-color interaction changes nothing");
        helper.assertValueEqual(dye.getCount(), 2, description + " same-color interaction consumes nothing");
    }

    private static void assertSameColorIsNotConsumed(
            GameTestHelper helper,
            Player owner,
            Wolf wolf,
            DyeColor color,
            String description
    ) {
        ItemStack dye = customDye(color);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        wolf.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(wolf.getCollarColor(), color, description + " same-color interaction changes nothing");
        helper.assertValueEqual(dye.getCount(), 2, description + " same-color interaction consumes nothing");
    }

    private static <T extends net.minecraft.world.entity.Entity> T roundTripEntity(
            GameTestHelper helper,
            T source,
            EntityType<T> type
    ) {
        var registries = helper.getLevel().registryAccess();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        source.saveWithoutId(output);
        T restored = type.create(helper.getLevel(), EntitySpawnReason.LOAD);
        helper.assertTrue(restored != null, "serialized entity type can be recreated");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, output.buildResult()));
        return restored;
    }
}
