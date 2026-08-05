package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDMapDecorationType;
import com.ninni.dye_depot.registry.DDParticles;
import com.ninni.dye_depot.registry.DDPoiTypes;
import com.ninni.dye_depot.registry.DDSoundEvents;
import eu.pb4.polymer.core.api.other.PolymerParticleType;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

public final class DDPolymer {
    private DDPolymer() {
    }

    public static void initialize() {
        DDPolymerPack.register();
        DDPolymerCollarPack.register();
        DDPolymerSheepPack.register();
        PolymerResourcePackUtils.addModAssets(DyeDepot.MOD_ID);
        ResourcePackExtras.forDefault().addBridgedModelsFolder(DyeDepot.modLoc("block"), DyeDepot.modLoc("item"));
        PolymerResourcePackUtils.markAsRequired();

        DDPolymerItems.register();
        DDPolymerBlocks.register();
        DDPolymerEntities.register();
        DDPolymerCreativeTab.register();
        RegistrySyncUtils.setServerEntry(
                BuiltInRegistries.POINT_OF_INTEREST_TYPE,
                DDPoiTypes.HOME.identifier()
        );
        DDMapDecorationType.BANNERS.forEach((color, holder) -> {
            MapDecorationType replacement = vanillaBannerDecoration(DDPolymerColors.vanillaColor(color));
            PolymerSyncedObject.setSyncedObject(
                    BuiltInRegistries.MAP_DECORATION_TYPE,
                    holder.value(),
                    (original, context) -> replacement
            );
        });
        PolymerParticleType.setOverlay(DDParticles.DYE_POOF, DDPolymerParticles.DYE_POOF);
        PolymerSoundEvent.registerOverlay(DDSoundEvents.DYE_BASKET_POOF);
    }

    private static MapDecorationType vanillaBannerDecoration(net.minecraft.world.item.DyeColor color) {
        return switch (color) {
            case WHITE -> MapDecorationTypes.WHITE_BANNER.value();
            case ORANGE -> MapDecorationTypes.ORANGE_BANNER.value();
            case MAGENTA -> MapDecorationTypes.MAGENTA_BANNER.value();
            case LIGHT_BLUE -> MapDecorationTypes.LIGHT_BLUE_BANNER.value();
            case YELLOW -> MapDecorationTypes.YELLOW_BANNER.value();
            case LIME -> MapDecorationTypes.LIME_BANNER.value();
            case PINK -> MapDecorationTypes.PINK_BANNER.value();
            case GRAY -> MapDecorationTypes.GRAY_BANNER.value();
            case LIGHT_GRAY -> MapDecorationTypes.LIGHT_GRAY_BANNER.value();
            case CYAN -> MapDecorationTypes.CYAN_BANNER.value();
            case PURPLE -> MapDecorationTypes.PURPLE_BANNER.value();
            case BLUE -> MapDecorationTypes.BLUE_BANNER.value();
            case BROWN -> MapDecorationTypes.BROWN_BANNER.value();
            case GREEN -> MapDecorationTypes.GREEN_BANNER.value();
            case RED -> MapDecorationTypes.RED_BANNER.value();
            case BLACK -> MapDecorationTypes.BLACK_BANNER.value();
            default -> throw new IllegalStateException("Extended color was not sanitized: " + color);
        };
    }
}
