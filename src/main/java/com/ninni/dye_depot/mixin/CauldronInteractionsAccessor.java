package com.ninni.dye_depot.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CauldronInteractions.class)
public interface CauldronInteractionsAccessor {

    @Invoker("shulkerBoxInteraction")
    static InteractionResult DD$shulkerBoxInteraction(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        throw new AssertionError();
    }

    @Invoker("bannerInteraction")
    static InteractionResult DD$bannerInteraction(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        throw new AssertionError();
    }
}
