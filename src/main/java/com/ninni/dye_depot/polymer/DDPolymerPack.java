package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;

final class DDPolymerPack {
    private static final String BROWN_SHULKER_TEXTURE = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAAk1BMVEUAAABWNBtiPCBdOR5nQCJbOB1eOh5iPSBzSChyRyhwRidmPyJsQyVrQiR0SCllPyLh6Kjm8K26tnnHw41vRSasqGuzr3HT1ZoAAAD///9fOx9RMRllPiFkPiFpQSNJLBVUMxpYNhtRMRhMLRZ4TCtJKxVkPSFNLhdhPB9bNx1tRCVuRCZ3SytYNRtKLBZOLxdIKxXmwgISAAAAAXRSTlMAQObYZgAAA2BJREFUeNrlU9mWmzAMBSRvmJCkme7tNMsMYXOd//+6SjaBmbTnTCEPfegF2b6SfGX72MktVNcp1bZVTUb9l5RwjYXxW2gFIEqBCGQS1M2MtwU6icYg21prK+vZAgqNznOtteEG3WyBGvWA0hiDLpmLlrdA00ODUs0W6AB1qR+0zkkDIZstoDA3Jv7aaEwXCDxoQm60XngGgDSVJnNrYf4ZFMLqiHAG9YIt0MwHExU0ugUCpdEll9fLLlIHa50jsuUahZot0DYCLTSIUi57TFLVzmXKuVqlLsvcm89ZdX2rir5THVnbY9USj2P2YejbJ/a1MR5zmVfdE/LVlRgNsEHXggXbIKwheBzFBYKVlpmwFB9yIcxySU9eYwVqyiJTPfkNj2OvnqjPUVJcYIiTkg4cAufXxzdf86dNqGgMjzU36Fpp48UMsBRf86AsyRfuibI65pt48yo7PGZyPiBkCnVJJAqiIE7VQpR6yue7H+uZMteARSuYm+gBXBW8Il4i15G46iTmJBlLEk96hLAkU/KKZNZboc1QQluR9WupAwzloFScH98q+RAyXkGxKopgYc/iyleBd4CREAzHZeTcmBxdUlm9Sgb0Yc+veYWmuPInFEpZPXFeAYIdHT2sVwg4caqG8iW3BeWvRgHgM7ByUlxD1qOcEsK9EJMAn8EaxjiNVfIIakpYVQ5BTQmPzKupwKPao1DFFFcu+RPSATy+3CCZi4u/4sjNEgH6RiwV8AP+RiC9oVEg1r9HwBPuE6D6d67gXwsQ7hJg3CMwwC+6SMeLH7HwJk5YJvASbwgsfs6ZygJGTg5V12rkhJq+kddTfnSAABAiHbmAV1wBsGeKS9GE+JTAaPZjQiOaBl4JCiHghaAEwfFJMZR4OYEk5MRJjjAWqImOgv5AmyEHaUDlvD874gL4a74f/FfvaMcQJsA354+e48CKDVD8mLiTl/6nB7KGbH848Xi09MD9Dy+CnSge8qZ44qkdvuOJMvwwHnzETyEy8DPFSesU7fjDJ4f0/OxT/3wm8+n54vbny5l8V3Pk8xxjvvfPh+D3nBN8ScRms91uNrvd+/e7XbIEmy3hHoFtwH8qwOf/CpsBM87/BnMF3v2GDwEztsD4/Gm7/fQ5jj8GzBTY7fgI7xDYBiwR+AWG6pMBRobdXQAAAABJRU5ErkJggg==";

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
        addDonorCarrierModels(builder);
        for (DDDyes dye : DDDyes.values()) {
            String color = dye.getName();
            String safeColor = DDPolymerColors.vanillaColor(dye.get()).getName();
            addItemDefinition(builder, color + "_banner", "dye_depot:item/polymer/" + color + "_banner");
            addPatternedBannerDefinition(builder, "polymer/" + color + "_banner_patterned", safeColor, "ground");
            addPatternedBannerDefinition(builder, "polymer/" + color + "_wall_banner_patterned", safeColor, "wall");
            addExactBannerDefinition(builder, "polymer/" + color + "_banner_plain", safeColor, "ground", color + "_banner_exact");
            addExactBannerDefinition(builder, "polymer/" + color + "_wall_banner_plain", safeColor, "wall", color + "_wall_banner_exact");
            addItemDefinition(builder, "polymer/" + color + "_sheep_wool", "dye_depot:item/polymer/" + color + "_sheep_wool");
            addItemDefinition(builder, "polymer/" + color + "_wall_banner", "dye_depot:block/polymer/" + color + "_wall_banner");
            addItemDefinition(builder, "polymer/" + color + "_carpet", "dye_depot:block/" + color + "_carpet");
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
                addItemDefinition(builder, "polymer/" + color + "_pane_" + mask, "dye_depot:block/polymer/" + color + "_pane_" + mask);
                builder.addStringData(
                        "assets/dye_depot/models/block/polymer/" + color + "_pane_" + mask + ".json",
                        paneBlockModel(color, mask)
                );
            }
            for (int step = 0; step <= 10; step++) {
                addShulkerDefinition(builder, color, step);
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
                    "assets/dye_depot/models/block/polymer/" + color + "_banner_exact.json",
                    exactBannerClothModel(color, false)
            );
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_wall_banner_exact.json",
                    exactBannerClothModel(color, true)
            );
            builder.addStringData(
                    "assets/dye_depot/models/item/polymer/" + color + "_shulker_empty.json",
                    "{\"textures\":{\"particle\":\"dye_depot:block/" + color + "_wool\"},\"elements\":[]}"
            );
        }
    }

    private static void addDonorCarrierModels(ResourcePackBuilder builder) {
        String emptyModel = "dye_depot:block/polymer/donor_empty";
        builder.addStringData(
                "assets/dye_depot/models/block/polymer/donor_empty.json",
                "{\"textures\":{\"particle\":\"minecraft:block/orange_wool\"},\"elements\":[]}"
        );
        builder.addStringData(
                "assets/minecraft/blockstates/orange_carpet.json",
                "{\"variants\":{\"\":{\"model\":\"" + emptyModel + "\"}}}"
        );

        StringBuilder candleVariants = new StringBuilder();
        String[] candleCounts = {"one", "two", "three", "four"};
        addItemDefinition(builder, "polymer/donor_orange_carpet", "minecraft:block/orange_carpet");
        for (int count = 1; count <= 4; count++) {
            for (boolean lit : new boolean[]{false, true}) {
                if (!candleVariants.isEmpty()) candleVariants.append(',');
                candleVariants.append("\"candles=").append(count).append(",lit=").append(lit)
                        .append("\":{\"model\":\"").append(emptyModel).append("\"}");
                String suffix = candleCounts[count - 1] + "_candle" + (count == 1 ? "" : "s") + (lit ? "_lit" : "");
                addItemDefinition(
                        builder,
                        "polymer/donor_orange_candle_" + count + (lit ? "_lit" : ""),
                        "minecraft:block/orange_candle_" + suffix
                );
            }
        }
        builder.addStringData(
                "assets/minecraft/blockstates/orange_candle.json",
                "{\"variants\":{" + candleVariants + "}}"
        );

        builder.addStringData("assets/minecraft/blockstates/brown_stained_glass_pane.json", "{\"multipart\":[]}");
        for (int mask = 0; mask < 16; mask++) {
            addItemDefinition(builder, "polymer/donor_brown_pane_" + mask, "dye_depot:block/polymer/donor_brown_pane_" + mask);
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/donor_brown_pane_" + mask + ".json",
                    paneBlockModel("minecraft", "brown", mask)
            );
        }

        builder.addData("assets/minecraft/textures/entity/shulker/shulker_brown.png", transparentTexture(64));
        builder.addData(
                "assets/dye_depot/textures/entity/shulker/donor_brown.png",
                Base64.getDecoder().decode(BROWN_SHULKER_TEXTURE)
        );
        builder.addStringData(
                "assets/minecraft/items/brown_shulker_box.json",
                "{\"model\":" + shulkerSpecial("minecraft:item/brown_shulker_box", "dye_depot:donor_brown", null) + "}"
        );
        for (int step = 0; step <= 10; step++) {
            builder.addStringData(
                    "assets/dye_depot/items/polymer/donor_brown_shulker_box_" + step + ".json",
                    "{\"model\":" + shulkerSpecial("minecraft:item/brown_shulker_box", "dye_depot:donor_brown", step / 10.0f) + "}"
            );
        }
    }

    private static byte[] transparentTexture(int size) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB), "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate transparent donor texture", exception);
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
                "{\"model\":" + bannerSpecialComponent(safeColor, attachment) + "}"
        );
    }

    private static void addExactBannerDefinition(
            ResourcePackBuilder builder,
            String path,
            String safeColor,
            String attachment,
            String exactModel
    ) {
        builder.addStringData(
                "assets/dye_depot/items/" + path + ".json",
                "{\"model\":{\"type\":\"minecraft:composite\",\"models\":[" +
                        bannerSpecialComponent(safeColor, attachment) +
                        ",{\"type\":\"minecraft:model\",\"model\":\"dye_depot:block/polymer/" + exactModel + "\"}]}}"
        );
    }

    private static String bannerSpecialComponent(String safeColor, String attachment) {
        return "{\"type\":\"minecraft:special\",\"base\":\"minecraft:item/template_banner\"," +
                        "\"model\":{\"type\":\"minecraft:banner\",\"color\":\"" + safeColor +
                        "\",\"attachment\":\"" + attachment + "\"},\"transformation\":{" +
                        "\"left_rotation\":[0.0,0.0,0.0,1.0],\"right_rotation\":[0.0,0.0,0.0,1.0]," +
                        "\"scale\":[0.6666667,-0.6666667,-0.6666667],\"translation\":[0.5,0.0,0.5]}}";
    }

    private static void addShulkerDefinition(ResourcePackBuilder builder, String color, int step) {
        builder.addStringData(
                "assets/dye_depot/items/polymer/" + color + "_shulker_box_" + step + ".json",
                "{\"model\":{\"type\":\"minecraft:special\",\"base\":\"dye_depot:item/polymer/" + color + "_shulker_empty\"," +
                        "\"model\":{\"type\":\"minecraft:shulker_box\",\"texture\":\"dye_depot:shulker_" + color +
                        "\",\"openness\":" + (step / 10.0f) + "},\"transformation\":{" +
                        "\"left_rotation\":[1.0,0.0,0.0,0.0],\"right_rotation\":[0.0,0.0,0.0,1.0]," +
                        "\"scale\":[0.9995,0.9995,0.9995],\"translation\":[0.5,1.4995,0.5]}}}"
        );
    }

    private static String shulkerSpecial(String base, String texture, Float openness) {
        return "{\"type\":\"minecraft:special\",\"base\":\"" + base + "\"," +
                "\"model\":{\"type\":\"minecraft:shulker_box\",\"texture\":\"" + texture + "\"" +
                (openness == null ? "" : ",\"openness\":" + openness) + "},\"transformation\":{" +
                "\"left_rotation\":[1.0,0.0,0.0,0.0],\"right_rotation\":[0.0,0.0,0.0,1.0]," +
                "\"scale\":[0.9995,0.9995,0.9995],\"translation\":[0.5,1.4995,0.5]}}";
    }

    private static String sheepCoatModel(String color) {
        String texture = "dye_depot:block/" + color + "_wool";
        return model(texture,
                box(2, 4, 2, 14, 12, 14, "#wool") + "," +
                box(4, 6, -2, 12, 14, 4, "#wool")
        );
    }

    private static String paneBlockModel(String color, int mask) {
        return paneBlockModel("dye_depot", color, mask);
    }

    private static String paneBlockModel(String namespace, String color, int mask) {
        String pane = namespace + ":block/" + color + "_stained_glass";
        String edge = namespace + ":block/" + color + "_stained_glass_pane_top";
        StringBuilder elements = new StringBuilder();
        StringBuilder centerFaces = new StringBuilder(
                "\"down\":{\"uv\":[7,7,9,9],\"texture\":\"#edge\"}," +
                        "\"up\":{\"uv\":[7,7,9,9],\"texture\":\"#edge\"}"
        );
        if ((mask & 1) == 0) centerFaces.append(",\"north\":{\"texture\":\"#pane\"}");
        if ((mask & 2) == 0) centerFaces.append(",\"east\":{\"texture\":\"#pane\"}");
        if ((mask & 4) == 0) centerFaces.append(",\"south\":{\"texture\":\"#pane\"}");
        if ((mask & 8) == 0) centerFaces.append(",\"west\":{\"texture\":\"#pane\"}");
        elements.append(element(7, 0, 7, 9, 16, 9, centerFaces.toString()));
        if ((mask & 1) != 0) appendElement(elements, element(7, 0, 0, 9, 16, 7, paneArmFaces("north", "west", "east")));
        if ((mask & 2) != 0) appendElement(elements, element(9, 0, 7, 16, 16, 9, paneArmFaces("east", "north", "south")));
        if ((mask & 4) != 0) appendElement(elements, element(7, 0, 9, 9, 16, 16, paneArmFaces("south", "west", "east")));
        if ((mask & 8) != 0) appendElement(elements, element(0, 0, 7, 7, 16, 9, paneArmFaces("west", "north", "south")));
        return "{\"ambientocclusion\":false,\"textures\":{" +
                "\"particle\":{\"sprite\":\"" + pane + "\",\"force_translucent\":true}," +
                "\"pane\":{\"sprite\":\"" + pane + "\",\"force_translucent\":true}," +
                "\"edge\":{\"sprite\":\"" + edge + "\",\"force_translucent\":true}}," +
                "\"elements\":[" + elements + "]}";
    }

    private static String paneArmFaces(String end, String sideA, String sideB) {
        return "\"down\":{\"texture\":\"#edge\"},\"up\":{\"texture\":\"#edge\"}," +
                "\"" + end + "\":{\"texture\":\"#edge\"}," +
                "\"" + sideA + "\":{\"texture\":\"#pane\"}," +
                "\"" + sideB + "\":{\"texture\":\"#pane\"}";
    }

    private static String element(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String faces) {
        return "{\"from\":[" + fromX + "," + fromY + "," + fromZ + "],\"to\":[" + toX + "," + toY + "," + toZ + "],\"faces\":{" + faces + "}}";
    }

    private static void appendElement(StringBuilder elements, String element) {
        elements.append(',').append(element);
    }

    private static String standingBannerModel(String color) {
        String cloth = "dye_depot:block/" + color + "_wool";
        return modelWithPole(cloth,
                box(7.5, 0, 7.5, 8.5, 16, 8.5, "#pole") + "," +
                box(3, 14, 7.25, 13, 15, 8.75, "#pole") + "," +
                box(3, 2, 7.6, 13, 14, 8.4, "#cloth")
        );
    }

    private static String exactBannerClothModel(String color, boolean wall) {
        String cloth = "dye_depot:block/" + color + "_wool";
        String element = wall
                ? box(1.25, -13.25, 1.55, 14.75, 13.9, 2.45, "#cloth")
                : box(1.25, 2.5, 8.55, 14.75, 29.5, 9.45, "#cloth");
        return "{\"ambientocclusion\":false,\"textures\":{\"particle\":\"" + cloth +
                "\",\"cloth\":\"" + cloth + "\"},\"elements\":[" + element + "]}";
    }

    private static String wallBannerModel(String color) {
        String cloth = "dye_depot:block/" + color + "_wool";
        return modelWithPole(cloth,
                box(3, 14, 14.5, 13, 15, 16, "#pole") + "," +
                box(3, 2, 15.2, 13, 14, 16, "#cloth")
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
