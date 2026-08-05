package com.ninni.dye_depot.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.ninni.dye_depot.polymer.DDPolymerLoomSlotMarker;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @WrapOperation(
            method = "synchronizeSlotToRemote(ILnet/minecraft/world/item/ItemStack;Ljava/util/function/Supplier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/RemoteSlot;matches(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean dyeDepot$compareLoomBannerSlotRepresentation(
            RemoteSlot remoteSlot,
            ItemStack current,
            Operation<Boolean> original,
            @Local(argsOnly = true) int slotIndex
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (menu instanceof LoomMenu loom
                && slotIndex >= 0
                && slotIndex < menu.slots.size()
                && menu.getSlot(slotIndex) == loom.getBannerSlot()) {
            // The client predicts moves with the inventory representation,
            // which includes the exact-color synthetic base. Compare against
            // the Loom-specific view so a five-pattern input is corrected to
            // five visible layers instead of being mistaken for a match at six.
            ItemStack comparisonCopy = current.copy();
            DDPolymerLoomSlotMarker.mark(comparisonCopy, menu.containerId);
            return original.call(remoteSlot, comparisonCopy);
        }
        return original.call(remoteSlot, current);
    }

    @WrapOperation(
            method = "sendAllDataToRemote",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ContainerSynchronizer;sendInitialData(Lnet/minecraft/world/inventory/AbstractContainerMenu;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;[I)V"
            )
    )
    private void dyeDepot$markInitialLoomBannerSlot(
            ContainerSynchronizer synchronizer,
            AbstractContainerMenu menu,
            List<ItemStack> slotItems,
            ItemStack carried,
            int[] dataSlots,
            Operation<Void> original
    ) {
        if (menu instanceof LoomMenu loom) {
            int bannerSlot = dyeDepot$bannerSlotIndex(loom);
            if (bannerSlot >= 0 && bannerSlot < slotItems.size()) {
                DDPolymerLoomSlotMarker.mark(slotItems.get(bannerSlot), menu.containerId);
            }
        }
        original.call(synchronizer, menu, slotItems, carried, dataSlots);
    }

    @WrapOperation(
            method = "synchronizeSlotToRemote",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ContainerSynchronizer;sendSlotChange(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V"
            )
    )
    private void dyeDepot$markChangedLoomBannerSlot(
            ContainerSynchronizer synchronizer,
            AbstractContainerMenu menu,
            int slotIndex,
            ItemStack stack,
            Operation<Void> original
    ) {
        if (menu instanceof LoomMenu loom
                && slotIndex >= 0
                && slotIndex < menu.slots.size()
                && menu.getSlot(slotIndex) == loom.getBannerSlot()) {
            DDPolymerLoomSlotMarker.mark(stack, menu.containerId);
        }
        original.call(synchronizer, menu, slotIndex, stack);
    }

    private static int dyeDepot$bannerSlotIndex(LoomMenu loom) {
        for (int index = 0; index < loom.slots.size(); index++) {
            if (loom.getSlot(index) == loom.getBannerSlot()) {
                return index;
            }
        }
        return -1;
    }
}
