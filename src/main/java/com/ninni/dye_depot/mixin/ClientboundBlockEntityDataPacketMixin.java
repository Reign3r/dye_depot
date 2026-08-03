package com.ninni.dye_depot.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ninni.dye_depot.polymer.DDPolymerBlockEntityNbt;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientboundBlockEntityDataPacket.class)
public class ClientboundBlockEntityDataPacketMixin {
    @ModifyReturnValue(method = "getTag", at = @At("RETURN"))
    private CompoundTag dyeDepot$sanitizeExtendedColors(CompoundTag original) {
        if (original == null || PacketContext.get() == null) {
            return original;
        }
        var packet = (ClientboundBlockEntityDataPacket) (Object) this;
        return DDPolymerBlockEntityNbt.sanitize(packet.getType(), original);
    }
}
