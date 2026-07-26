package com.ninni.dye_depot.registry;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

public class DDFlammables {

    public static void register() {
        var flammables = FlammableBlockRegistry.getDefaultInstance();

        DDBlocks.CARPETS.values().forEach(block -> flammables.add(block, 60, 20));
        DDBlocks.WOOL.values().forEach(block -> flammables.add(block, 60, 100));
    }

}
