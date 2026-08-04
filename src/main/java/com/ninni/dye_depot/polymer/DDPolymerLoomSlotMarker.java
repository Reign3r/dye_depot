package com.ninni.dye_depot.polymer;

import net.minecraft.world.item.ItemStack;

/**
 * Out-of-band provenance for the copied stack sent from the Loom banner slot.
 *
 * <p>The marker is a mixin field, not a data component, so it can guide the
 * client representation without ever becoming part of the wire format.</p>
 */
public interface DDPolymerLoomSlotMarker {
    int NO_CONTAINER = -1;

    int dyeDepot$getLoomContainerId();

    void dyeDepot$setLoomContainerId(int containerId);

    static void mark(ItemStack stack, int containerId) {
        if (!stack.isEmpty()) {
            ((DDPolymerLoomSlotMarker) (Object) stack).dyeDepot$setLoomContainerId(containerId);
        }
    }

    static boolean isMarked(ItemStack stack) {
        return ((DDPolymerLoomSlotMarker) (Object) stack).dyeDepot$getLoomContainerId() != NO_CONTAINER;
    }
}
