package com.ninni.dye_depot.mixin;

import com.ninni.dye_depot.polymer.DDPolymerLoomSlotMarker;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DDPolymerLoomSlotMarker {
    @Unique
    private int dyeDepot$loomContainerId = DDPolymerLoomSlotMarker.NO_CONTAINER;

    @Override
    public int dyeDepot$getLoomContainerId() {
        return this.dyeDepot$loomContainerId;
    }

    @Override
    public void dyeDepot$setLoomContainerId(int containerId) {
        this.dyeDepot$loomContainerId = containerId;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void dyeDepot$copyLoomSlotMarker(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack copy = cir.getReturnValue();
        if (this.dyeDepot$loomContainerId != DDPolymerLoomSlotMarker.NO_CONTAINER && !copy.isEmpty()) {
            DDPolymerLoomSlotMarker.mark(copy, this.dyeDepot$loomContainerId);
        }
    }
}
