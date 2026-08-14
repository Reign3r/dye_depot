package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.registry.DDBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Shadow
    @Final
    private boolean isSticky;

    @Inject(
            method = "triggerEvent(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;II)Z",
            at = @At("RETURN")
    )
    private void dyeDepot$resyncUnpulledGlazedTerracotta(
            BlockState pistonState,
            Level level,
            BlockPos pistonPos,
            int event,
            int data,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!this.isSticky
                || event != PistonBaseBlock.TRIGGER_CONTRACT
                || !Boolean.TRUE.equals(cir.getReturnValue())
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos sourcePos = pistonPos.relative(pistonState.getValue(PistonBaseBlock.FACING), 2);
        BlockState sourceState = serverLevel.getBlockState(sourcePos);
        if (DDBlocks.GLAZED_TERRACOTTA.values().anyMatch(sourceState::is)) {
            // Piston block events are broadcast after this method, while
            // ChunkHolder flushes this unchanged-state correction next tick.
            // The correction therefore wins over vanilla-client prediction.
            serverLevel.sendBlockUpdated(sourcePos, sourceState, sourceState, Block.UPDATE_CLIENTS);
        }
    }
}
