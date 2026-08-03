package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.fabricmc.loader.api.FabricLoader;

final class DDPolymerPack {
    private DDPolymerPack() {
    }

    static void register() {
        // Apply the default-enabled Sky/Ash pack after normal mod assets so its
        // duplicate language keys win the JSON merge deterministically.
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(DDPolymerPack::copyOverridePack);
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(DDPolymerPack::addVanillaSafeModels);
    }

    private static void copyOverridePack(ResourcePackBuilder builder) {
        FabricLoader.getInstance().getModContainer(DyeDepot.MOD_ID)
                .flatMap(container -> container.findPath("resourcepacks/dye_override"))
                .ifPresent(builder::copyResourcePackFromPath);
    }

    private static void addVanillaSafeModels(ResourcePackBuilder builder) {
        for (DDDyes dye : DDDyes.values()) {
            String color = dye.getName();
            String safeColor = DDPolymerColors.vanillaColor(dye.get()).getName();
            addItemDefinition(builder, color + "_banner", "dye_depot:item/polymer/" + color + "_banner");
            addPatternedBannerDefinition(builder, "polymer/" + color + "_banner_patterned", safeColor, "ground");
            addPatternedBannerDefinition(builder, "polymer/" + color + "_wall_banner_patterned", safeColor, "wall");
            addItemDefinition(builder, "polymer/" + color + "_sheep_wool", "dye_depot:item/polymer/" + color + "_sheep_wool");
            addItemDefinition(builder, "polymer/" + color + "_wall_banner", "dye_depot:block/polymer/" + color + "_wall_banner");
            addItemDefinition(builder, "polymer/" + color + "_bed_head", "dye_depot:block/" + color + "_bed_head");
            addItemDefinition(builder, "polymer/" + color + "_bed_foot", "dye_depot:block/" + color + "_bed_foot");
            addItemDefinition(builder, "polymer/" + color + "_candle_cake", "dye_depot:block/" + color + "_candle_cake");
            addItemDefinition(builder, "polymer/" + color + "_candle_cake_lit", "dye_depot:block/" + color + "_candle_cake_lit");

            String[] candleCounts = {"one", "two", "three", "four"};
            for (int count = 1; count <= 4; count++) {
                String blockModel = "dye_depot:block/" + color + "_candle_" + candleCounts[count - 1] + "_candle" + (count == 1 ? "" : "s");
                addItemDefinition(builder, "polymer/" + color + "_candle_" + count, blockModel);
                addItemDefinition(builder, "polymer/" + color + "_candle_" + count + "_lit", blockModel + "_lit");
            }
            for (int mask = 0; mask < 16; mask++) {
                builder.addStringData(
                        "assets/dye_depot/items/polymer/" + color + "_pane_" + mask + ".json",
                        paneItemDefinition(color, mask)
                );
            }

            builder.addStringData(
                    "assets/dye_depot/models/item/polymer/" + color + "_banner.json",
                    "{\"parent\":\"dye_depot:block/polymer/" + color + "_banner\"}"
            );
            builder.addStringData(
                    "assets/dye_depot/models/item/polymer/" + color + "_sheep_wool.json",
                    sheepCoatModel(color)
            );
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_banner.json",
                    standingBannerModel(color)
            );
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_wall_banner.json",
                    wallBannerModel(color)
            );
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_shulker_box.json",
                    shulkerModel(color)
            );
        }
    }

    private static void addItemDefinition(ResourcePackBuilder builder, String path, String model) {
        builder.addStringData(
                "assets/dye_depot/items/" + path + ".json",
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + model + "\"}}"
        );
    }

    private static void addPatternedBannerDefinition(
            ResourcePackBuilder builder,
            String path,
            String safeColor,
            String attachment
    ) {
        builder.addStringData(
                "assets/dye_depot/items/" + path + ".json",
                "{\"model\":{\"type\":\"minecraft:special\",\"base\":\"minecraft:item/template_banner\"," +
                        "\"model\":{\"type\":\"minecraft:banner\",\"color\":\"" + safeColor +
                        "\",\"attachment\":\"" + attachment + "\"},\"transformation\":{" +
                        "\"left_rotation\":[0.0,0.0,0.0,1.0],\"right_rotation\":[0.0,0.0,0.0,1.0]," +
                        "\"scale\":[0.6666667,-0.6666667,-0.6666667],\"translation\":[0.5,0.0,0.5]}}}"
        );
    }

    private static String sheepCoatModel(String color) {
        String texture = "dye_depot:block/" + color + "_wool";
        return model(texture,
                box(2, 4, 2, 14, 12, 14, "#wool") + "," +
                box(4, 6, -2, 12, 14, 4, "#wool")
        );
    }

    private static String paneItemDefinition(String color, int mask) {
        String base = "dye_depot:block/" + color + "_stained_glass_pane_";
        StringBuilder models = new StringBuilder(modelComponent(base + "post", 0));
        appendModel(models, base + ((mask & 1) != 0 ? "side" : "noside"), 0);
        appendModel(models, base + ((mask & 2) != 0 ? "side" : "noside_alt"), (mask & 2) != 0 ? 90 : 0);
        appendModel(models, base + ((mask & 4) != 0 ? "side_alt" : "noside_alt"), (mask & 4) != 0 ? 0 : 90);
        appendModel(models, base + ((mask & 8) != 0 ? "side_alt" : "noside"), (mask & 8) != 0 ? 90 : 270);
        return "{\"model\":{\"type\":\"minecraft:composite\",\"models\":[" + models + "]}}";
    }

    private static void appendModel(StringBuilder builder, String model, int y) {
        builder.append(',').append(modelComponent(model, y));
    }

    private static String modelComponent(String model, int y) {
        if (y == 0) {
            return "{\"type\":\"minecraft:model\",\"model\":\"" + model + "\"}";
        }
        double radians = Math.toRadians(y) / 2.0;
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return "{\"type\":\"minecraft:model\",\"model\":\"" + model + "\",\"transformation\":{" +
                "\"left_rotation\":[0.0," + sin + ",0.0," + cos + "]," +
                "\"right_rotation\":[0.0,0.0,0.0,1.0]," +
                "\"scale\":[1.0,1.0,1.0],\"translation\":[0.0,0.0,0.0]}}";
    }

    private static String standingBannerModel(String color) {
        String cloth = "dye_depot:block/" + color + "_wool";
        return modelWithPole(cloth,
                box(7.5, 0, 7.5, 8.5, 16, 8.5, "#pole") + "," +
                box(3, 14, 7.25, 13, 15, 8.75, "#pole") + "," +
                box(3, 2, 7.6, 13, 14, 8.4, "#cloth")
        );
    }

    private static String wallBannerModel(String color) {
        String cloth = "dye_depot:block/" + color + "_wool";
        return modelWithPole(cloth,
                box(3, 14, 14.5, 13, 15, 16, "#pole") + "," +
                box(3, 2, 15.2, 13, 14, 16, "#cloth")
        );
    }

    private static String shulkerModel(String color) {
        String texture = "dye_depot:entity/shulker/shulker_" + color;
        return model(texture,
                box(1, 0, 1, 15, 7, 15, "#wool") + "," +
                box(1, 7, 1, 15, 16, 15, "#wool")
        );
    }

    private static String model(String texture, String elements) {
        return "{\"textures\":{\"particle\":\"" + texture + "\",\"wool\":\"" + texture + "\"},\"elements\":[" + elements + "]}";
    }

    private static String modelWithPole(String cloth, String elements) {
        return "{\"textures\":{\"particle\":\"" + cloth + "\",\"cloth\":\"" + cloth + "\",\"pole\":\"minecraft:block/oak_planks\"},\"elements\":[" + elements + "]}";
    }

    private static String box(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String texture) {
        return "{\"from\":[" + fromX + "," + fromY + "," + fromZ + "],\"to\":[" + toX + "," + toY + "," + toZ + "],\"faces\":{" +
                "\"down\":{\"texture\":\"" + texture + "\"}," +
                "\"up\":{\"texture\":\"" + texture + "\"}," +
                "\"north\":{\"texture\":\"" + texture + "\"}," +
                "\"south\":{\"texture\":\"" + texture + "\"}," +
                "\"west\":{\"texture\":\"" + texture + "\"}," +
                "\"east\":{\"texture\":\"" + texture + "\"}}}";
    }
}
