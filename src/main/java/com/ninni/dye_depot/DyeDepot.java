package com.ninni.dye_depot;

import com.google.common.reflect.Reflection;
import com.ninni.dye_depot.registry.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.DispenserBlock;

public class DyeDepot implements ModInitializer {
    public static final String MOD_ID = "dye_depot";

    public static Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry, String path) {
        return ResourceKey.create(registry, modLoc(path));
    }

    @Override
    public void onInitialize() {
        DDMigration.addAliases();
        DDResourceConditions.registerFallbacks();
        DDFlammables.register();

        DDVanillaIntegration.commonInit();
        Reflection.initialize(
                DDItems.class,
                DDBlocks.class,
                DDParticles.class,
                DDSoundEvents.class,
                DDCreativeModeTabs.class,
                DDMapDecorationType.class,
                DDPoiTypes.class
        );

        DDBlocks.SHULKER_BOXES.forEach((dye, shulkerBox) ->
            DispenserBlock.registerBehavior(shulkerBox.value(), new ShulkerBoxDispenseBehavior())
        );
    }
}
