package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.polymer.DDPolymerBlockEntityNbt;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Sanitizes block-entity NBT embedded in initial chunk packets. */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityInfo")
public abstract class ClientboundLevelChunkBlockEntityInfoMixin {
    @Shadow
    @Final
    private BlockEntityType<?> type;

    @ModifyArg(
            method = "write(Lnet/minecraft/network/RegistryFriendlyByteBuf;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;writeNbt(Lnet/minecraft/nbt/Tag;)Lnet/minecraft/network/FriendlyByteBuf;"
            )
    )
    private Tag dyeDepot$sanitizeExtendedColors(Tag original) {
        PacketContext context = PacketContext.get();
        if (!(original instanceof CompoundTag tag) || context == null) {
            return original;
        }
        return DDPolymerBlockEntityNbt.sanitize(
                this.type,
                tag,
                context.get(PacketContext.REGISTRY_ACCESS)
        );
    }
}
