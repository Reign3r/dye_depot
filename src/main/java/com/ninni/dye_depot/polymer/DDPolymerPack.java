package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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
            addItemDefinition(builder, "polymer/" + color + "_shulker_base", "dye_depot:block/polymer/" + color + "_shulker_base");
            addItemDefinition(builder, "polymer/" + color + "_shulker_lid", "dye_depot:block/polymer/" + color + "_shulker_lid");
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_shulker_base.json",
                    shulkerPartModel(color, false)
            );
            builder.addStringData(
                    "assets/dye_depot/models/block/polymer/" + color + "_shulker_lid.json",
                    shulkerPartModel(color, true)
            );
            copyShulkerTextureToBlockAtlas(builder, color);

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
        for (String model : new String[]{"post", "side", "side_alt", "noside", "noside_alt"}) {
            builder.addStringData(
                    "assets/minecraft/models/block/brown_stained_glass_pane_" + model + ".json",
                    "{\"textures\":{\"particle\":\"minecraft:block/brown_stained_glass\"},\"elements\":[]}"
            );
        }
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
        addItemDefinition(builder, "polymer/donor_brown_shulker_base", "dye_depot:block/polymer/donor_brown_shulker_base");
        addItemDefinition(builder, "polymer/donor_brown_shulker_lid", "dye_depot:block/polymer/donor_brown_shulker_lid");
        builder.addStringData(
                "assets/dye_depot/models/block/polymer/donor_brown_shulker_base.json",
                shulkerPartModel("donor_brown", false)
        );
        builder.addStringData(
                "assets/dye_depot/models/block/polymer/donor_brown_shulker_lid.json",
                shulkerPartModel("donor_brown", true)
        );
        builder.addData(
                "assets/dye_depot/textures/block/polymer/shulker_donor_brown.png",
                Base64.getDecoder().decode(BROWN_SHULKER_TEXTURE)
        );
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
        if ((mask & 1) == 0) centerFaces.append(",\"north\":{\"uv\":[9,0,7,16],\"texture\":\"#pane\"}");
        if ((mask & 2) == 0) centerFaces.append(",\"east\":{\"uv\":[7,0,9,16],\"texture\":\"#pane\"}");
        if ((mask & 4) == 0) centerFaces.append(",\"south\":{\"uv\":[7,0,9,16],\"texture\":\"#pane\"}");
        if ((mask & 8) == 0) centerFaces.append(",\"west\":{\"uv\":[9,0,7,16],\"texture\":\"#pane\"}");
        elements.append(element(7, 0, 7, 9, 16, 9, centerFaces.toString()));
        if ((mask & 1) != 0) appendElement(elements, element(7, 0, 0, 9, 16, 7, northPaneArmFaces()));
        if ((mask & 2) != 0) appendElement(elements, element(9, 0, 7, 16, 16, 9, eastPaneArmFaces()));
        if ((mask & 4) != 0) appendElement(elements, element(7, 0, 9, 9, 16, 16, southPaneArmFaces()));
        if ((mask & 8) != 0) appendElement(elements, element(0, 0, 7, 7, 16, 9, westPaneArmFaces()));
        return "{\"ambientocclusion\":false,\"textures\":{" +
                "\"particle\":{\"sprite\":\"" + pane + "\",\"force_translucent\":true}," +
                "\"pane\":{\"sprite\":\"" + pane + "\",\"force_translucent\":true}," +
                "\"edge\":{\"sprite\":\"" + edge + "\",\"force_translucent\":true}}," +
                "\"elements\":[" + elements + "]}";
    }

    private static String northPaneArmFaces() {
        return "\"down\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"},\"up\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"}," +
                "\"west\":{\"uv\":[16,0,9,16],\"texture\":\"#pane\"},\"east\":{\"uv\":[9,0,16,16],\"texture\":\"#pane\"}";
    }

    private static String eastPaneArmFaces() {
        return "\"down\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"},\"up\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"}," +
                "\"north\":{\"uv\":[16,0,9,16],\"texture\":\"#pane\"},\"south\":{\"uv\":[9,0,16,16],\"texture\":\"#pane\"}";
    }

    private static String southPaneArmFaces() {
        return "\"down\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"},\"up\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"}," +
                "\"west\":{\"uv\":[7,0,0,16],\"texture\":\"#pane\"},\"east\":{\"uv\":[0,0,7,16],\"texture\":\"#pane\"}";
    }

    private static String westPaneArmFaces() {
        return "\"down\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"},\"up\":{\"uv\":[7,0,9,7],\"texture\":\"#edge\"}," +
                "\"north\":{\"uv\":[7,0,0,16],\"texture\":\"#pane\"},\"south\":{\"uv\":[0,0,7,16],\"texture\":\"#pane\"}";
    }

    private static void copyShulkerTextureToBlockAtlas(ResourcePackBuilder builder, String color) {
        String source = "assets/dye_depot/textures/entity/shulker/shulker_" + color + ".png";
        var path = FabricLoader.getInstance().getModContainer(DyeDepot.MOD_ID)
                .flatMap(container -> container.findPath(source))
                .orElseThrow(() -> new IllegalStateException("Missing shulker texture " + source));
        try {
            builder.addData("assets/dye_depot/textures/block/polymer/shulker_" + color + ".png", Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not copy shulker texture " + source, exception);
        }
    }

    private static String shulkerPartModel(String color, boolean lid) {
        String texture = "dye_depot:block/polymer/shulker_" + color;
        String faces = lid
                ? entityCubeFaces(0, 0, 16, 12, 16)
                : entityCubeFaces(0, 28, 16, 8, 16);
        return "{\"ambientocclusion\":false,\"textures\":{\"particle\":\"" + texture + "\",\"shell\":\"" + texture + "\"},\"elements\":[" +
                (lid ? element(0, 4, 0, 16, 16, 16, faces) : element(0, 0, 0, 16, 8, 16, faces)) + "]}";
    }

    private static String entityCubeFaces(int u, int v, int width, int height, int depth) {
        return faceUv("west", u, v + depth, u + depth, v + depth + height) + "," +
                faceUv("north", u + depth, v + depth, u + depth + width, v + depth + height) + "," +
                faceUv("east", u + depth + width, v + depth, u + depth * 2 + width, v + depth + height) + "," +
                faceUv("south", u + depth * 2 + width, v + depth, u + depth * 2 + width * 2, v + depth + height) + "," +
                faceUv("up", u + depth, v, u + depth + width, v + depth) + "," +
                faceUv("down", u + depth + width, v, u + depth + width * 2, v + depth);
    }

    private static String faceUv(String face, int u1, int v1, int u2, int v2) {
        return "\"" + face + "\":{\"uv\":[" + uv(u1) + "," + uv(v1) + "," + uv(u2) + "," + uv(v2) + "],\"texture\":\"#shell\"}";
    }

    private static double uv(int pixel) {
        return pixel / 4.0;
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
