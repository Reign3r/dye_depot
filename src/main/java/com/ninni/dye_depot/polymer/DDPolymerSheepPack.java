package com.ninni.dye_depot.polymer;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.util.Base64;

/**
 * Generates the reusable, tintable item models used by the articulated
 * Polymer sheep coat. Keeping these models color-neutral avoids allocating a
 * separate model-data entry for every Dye Depot color and every body part.
 */
final class DDPolymerSheepPack {
    private static final byte[] ADULT_WOOL_TEXTURE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgBAMAAABQs2O3AAAAElBMVEUAAAD////4+Pjv7+/f39/U1NS+A43/AAAAAXRSTlMA" +
                    "QObYZgAAATpJREFUeNqtktGRwzAIRK8FuAoANxDAFYAqCPTfyiWOleTDTn5uZ/SjebPLIv3cxMzd/XMuBPwCEHwGgL5EENBn" +
                    "gOFTRFwIgRA9Kw4BYwUiURttxw6iiCjiPmpOBHdN4KoECMBr9thLIxHQCzBGIrR11AMgRhBinEC3AaF1Zz2KqDCaq56WtsUW" +
                    "94hTIDI9bzrZGZuPTlMmBjp+Fh+VSlvZGSrdr63/LlkpgARPB9UXgEiLtxEjAOAE3hwALNb2uAC+RdibA8k2JDHiDtimsN2B" +
                    "LasylACAt6uMSPVZmyyz6+p24TmksImqyh4hkdnXDIYZoaJ8P3tNdK8uUyQg3LcgwsY6FyU52oPo+SVyMU8P3wFkXzuU4SY6" +
                    "eYuuEGUG4gNAVHxUGSvjIcBmY3SFKTIeApLdXV2Pmv+iP9POUERdnCfHAAAAAElFTkSuQmCC"
    );
    private static final byte[] BABY_WOOL_TEXTURE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgBAMAAABQs2O3AAAAElBMVEUAAAD////4+Pjs7Ozf39/U1NQ46axEAAAAAXRSTlMA" +
                    "QObYZgAAAOBJREFUeNrNkjFOgzEMRg3lAMRtd2JzAPCXA1TY3RnI/a+C/5YF5U8GJt4QKdLLi2KFiB4LOy4056EU95WQhYh1" +
                    "oUZbFphpQWUpUiqz5KbTiOBZXqACnQjwNyL31kAUySA0z+XJI1rKqjoIEZtwP6tFePKOerr2y/Khp57CatgpfBL1WaGWcxYW" +
                    "QuHzVohkXwBeN8FUQXsc4Lcraj3KfqH5rTDl0LxN5yCJ9m5yhwZMoNL7pkHMaEBFodcvFRMzxU6hQYAADAZ80Ej/9VH+IMQP" +
                    "NAMKNRhohvI712M1+sd8A4+CLXW/ejOmAAAAAElFTkSuQmCC"
    );
    private static final byte[] WOOL_UNDERCOAT_TEXTURE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgBAMAAABQs2O3AAAAD1BMVEUAAAD49vXs7Oze3t7S0tLbRZzRAAAAAXRSTlMA" +
                    "QObYZgAAATJJREFUeNqt0cGN5DAMRNEvKQF2ewOgSQegphXAruT8Y1oIhjGYgX2bd6gDUagLmcZo1oJnqw3zwbNY2rDGs2a" +
                    "LW/As1qUN51nb/YjGs9FbmPMsXmPfOvfGZ/nn7/7ns/iIDRAywhfbyIDSW7SdSTCESzTOwjDfNwDVla/CvBeghlkLBxC+2a17" +
                    "pcRm3S0AVapmuPTdj0o5Yh/bPgBBZlRuZVYFVk088LM2fTgdxzETJjWmRAGUqetoMzlVfVMRUTKnv9twyNcCGbKASpI3Z6M7" +
                    "HFyUSgFBUCbv4d79WgAypJJBYYrm1mwLmIrBCihJOXWLMHeDKYuSS6YgcAof4/oCpJQhISTI3JHVETKaeSm3TOeKpMStWpE3" +
                    "UAXhXhYKCUC5k8kgKoK+gJ8UeEGqAsLv+A+eJie1pArJOgAAAABJRU5ErkJggg=="
    );
    private static final PartModel[] PART_MODELS = {
            // SheepFurModel.createFurLayer(), transformed around each native pivot.
            new PartModel("adult_head", "sheep_wool", 0, 0, 6, 6, 6,
                    4.4, 5.4, 3.4, 11.6, 12.6, 10.6),
            new PartModel("adult_body", "sheep_wool", 28, 8, 8, 16, 6,
                    2.25, 0.25, -0.75, 13.75, 19.75, 8.75),
            new PartModel("adult_leg", "sheep_wool", 0, 16, 4, 6, 4,
                    5.5, 1.5, 5.5, 10.5, 8.5, 10.5),
            new PartModel("adult_right_leg", "sheep_wool", 0, 16, 4, 6, 4,
                    5.5, 1.5, 5.5, 10.5, 8.5, 10.5),

            // ModelLayers.SHEEP_BABY_WOOL uses BabySheepModel's authored mesh.
            new PartModel("baby_head", "sheep_wool_baby", 0, 0, 5, 5, 5,
                    5.5, 7.5, 4.5, 10.5, 12.5, 9.5),
            new PartModel("baby_body", "sheep_wool_baby", 0, 10, 6, 4, 9,
                    5.0, 6.0, 3.5, 11.0, 10.0, 12.5),
            new PartModel("baby_right_hind_leg", "sheep_wool_baby", 0, 23, 2, 5, 2,
                    7.0, 3.0, 7.0, 9.0, 8.0, 9.0),
            new PartModel("baby_left_hind_leg", "sheep_wool_baby", 24, 12, 2, 5, 2,
                    7.0, 3.0, 7.0, 9.0, 8.0, 9.0),
            new PartModel("baby_right_front_leg", "sheep_wool_baby", 8, 23, 2, 5, 2,
                    7.0, 3.0, 7.0, 9.0, 8.0, 9.0),
            new PartModel("baby_left_front_leg", "sheep_wool_baby", 24, 5, 2, 5, 2,
                    7.0, 3.0, 7.0, 9.0, 8.0, 9.0)
    };
    private static final PartModel[] UNDERCOAT_MODELS = {
            // SheepModel.createBodyLayer(), inflated by 0.01 pixel to avoid
            // depth fighting with the native white proxy's base model.
            new PartModel("adult_undercoat_head", "sheep_wool_undercoat", 0, 0, 6, 6, 8,
                    4.99, 5.99, 1.99, 11.01, 12.01, 10.01),
            new PartModel("adult_undercoat_body", "sheep_wool_undercoat", 28, 8, 8, 16, 6,
                    3.99, 1.99, 0.99, 12.01, 18.01, 7.01),
            new PartModel("adult_undercoat_leg", "sheep_wool_undercoat", 0, 16, 4, 12, 4,
                    5.99, -4.01, 5.99, 10.01, 8.01, 10.01),
            // QuadrupedModel.createBodyMesh(12, false, true, ...) mirrors only
            // the right base legs. SheepFurModel reuses an unmirrored cube for
            // every outer-wool leg, so only this undercoat element is mirrored.
            new PartModel("adult_undercoat_right_leg", "sheep_wool_undercoat", 0, 16, 4, 12, 4,
                    5.99, -4.01, 5.99, 10.01, 8.01, 10.01, true)
    };

    private DDPolymerSheepPack() {
    }

    static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(
                DDPolymerSheepPack::addModels
        );
    }

    private static void addModels(ResourcePackBuilder builder) {
        builder.addData(
                "assets/dye_depot/textures/block/polymer/sheep_wool.png",
                ADULT_WOOL_TEXTURE
        );
        builder.addData(
                "assets/dye_depot/textures/block/polymer/sheep_wool_baby.png",
                BABY_WOOL_TEXTURE
        );
        builder.addData(
                "assets/dye_depot/textures/block/polymer/sheep_wool_undercoat.png",
                WOOL_UNDERCOAT_TEXTURE
        );
        for (int index = 0; index < PART_MODELS.length; index++) {
            PartModel part = PART_MODELS[index];
            String path = "polymer/sheep/" + part.name();
            builder.addStringData(
                    "assets/dye_depot/items/" + path + ".json",
                    itemDefinition("dye_depot:item/" + path)
            );
            builder.addStringData(
                    "assets/dye_depot/models/item/" + path + ".json",
                    index < UNDERCOAT_MODELS.length
                            ? partModel(UNDERCOAT_MODELS[index], part)
                            : partModel(part)
            );
        }
        for (PartModel part : UNDERCOAT_MODELS) {
            String path = "polymer/sheep/" + part.name();
            builder.addStringData(
                    "assets/dye_depot/items/" + path + ".json",
                    itemDefinition("dye_depot:item/" + path)
            );
            builder.addStringData(
                    "assets/dye_depot/models/item/" + path + ".json",
                    partModel(part)
            );
        }
    }

    private static String itemDefinition(String model) {
        return "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + model +
                "\",\"tints\":[{\"type\":\"minecraft:dye\",\"default\":-1}]}}";
    }

    private static String partModel(PartModel... parts) {
        StringBuilder textures = new StringBuilder();
        StringBuilder elements = new StringBuilder();
        for (PartModel part : parts) {
            if (!textures.isEmpty()) textures.append(',');
            textures.append('"').append(part.texture()).append("\":\"dye_depot:block/polymer/")
                    .append(part.texture()).append('"');
            if (!elements.isEmpty()) elements.append(',');
            elements.append(partElement(part));
        }
        return "{\"ambientocclusion\":false,\"textures\":{" +
                "\"particle\":\"dye_depot:block/polymer/" + parts[0].texture() + "\"," + textures +
                "},\"elements\":[" + elements + "]}";
    }

    private static String partElement(PartModel part) {
        int u0 = part.textureU();
        int u1 = u0 + part.depth();
        int u2 = u1 + part.width();
        int u22 = u2 + part.width();
        int u3 = u2 + part.depth();
        int u4 = u3 + part.width();
        int v0 = part.textureV();
        int v1 = v0 + part.depth();
        int v2 = v1 + part.height();

        // Model coordinates are rotated 180 degrees around Z before the item
        // renderer applies its own Y turn. Swap the corresponding face names
        // while preserving ModelPart.Cube's native unfolded UV rectangles.
        String faces = part.mirrored()
                ? String.join(",",
                        face("up", u2, v0, u1, v1, part.texture()),
                        face("down", u22, v1, u2, v0, part.texture()),
                        face("east", u3, v1, u2, v2, part.texture()),
                        face("north", u2, v1, u1, v2, part.texture()),
                        face("west", u1, v1, u0, v2, part.texture()),
                        face("south", u4, v1, u3, v2, part.texture())
                )
                : String.join(",",
                        face("up", u1, v0, u2, v1, part.texture()),
                        face("down", u2, v1, u22, v0, part.texture()),
                        face("east", u0, v1, u1, v2, part.texture()),
                        face("north", u1, v1, u2, v2, part.texture()),
                        face("west", u2, v1, u3, v2, part.texture()),
                        face("south", u3, v1, u4, v2, part.texture())
                );
        return "{" +
                "\"from\":[" + part.fromX() + ',' + part.fromY() + ',' + part.fromZ() + "]," +
                "\"to\":[" + part.toX() + ',' + part.toY() + ',' + part.toZ() + "]," +
                "\"faces\":{" + faces + "}}";
    }

    private static String face(String direction, int u1, int v1, int u2, int v2, String texture) {
        return "\"" + direction + "\":{\"uv\":[" + uvX(u1) + ',' + uvY(v1) + ',' +
                uvX(u2) + ',' + uvY(v2) + "],\"texture\":\"#" + texture + "\",\"tintindex\":0}";
    }

    private static double uvX(int pixel) {
        return pixel / 4.0;
    }

    private static double uvY(int pixel) {
        return pixel / 2.0;
    }

    private record PartModel(
            String name,
            String texture,
            int textureU,
            int textureV,
            int width,
            int height,
            int depth,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            boolean mirrored
    ) {
        private PartModel(
                String name,
                String texture,
                int textureU,
                int textureV,
                int width,
                int height,
                int depth,
                double fromX,
                double fromY,
                double fromZ,
                double toX,
                double toY,
                double toZ
        ) {
            this(name, texture, textureU, textureV, width, height, depth,
                    fromX, fromY, fromZ, toX, toY, toZ, false);
        }
    }
}
