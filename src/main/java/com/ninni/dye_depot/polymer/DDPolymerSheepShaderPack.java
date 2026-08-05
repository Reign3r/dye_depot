package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs the experimental native-sheep shader transport. The client still
 * renders an ordinary vanilla sheep and its real wool layers; a tiny,
 * client-only scale residue tells the shader whether a safe vanilla tint is a
 * donor for one of Dye Depot's colors.
 */
final class DDPolymerSheepShaderPack {
    static final int RESIDUE_CLASS_COUNT = 5;
    static final int VANILLA_CLASS = 0;
    static final int CUSTOM_UNSHEARED_CLASS = 1;
    static final int TAN_UNSHEARED_CLASS = 2;
    static final int CUSTOM_SHEARED_CLASS = 3;
    static final int TAN_SHEARED_CLASS = 4;
    static final double LOG_SCALE_STEP = 1.0 / 128.0;
    static final double MIN_SCALE = 0.0625;
    static final double MAX_SCALE = 16.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(DyeDepot.MOD_ID + "/sheep_shader");
    private static final String VERTEX_SHADER = "assets/minecraft/shaders/core/entity.vsh";
    private static final String FRAGMENT_SHADER = "assets/minecraft/shaders/core/entity.fsh";
    private static final int MARKER_X = 63;
    private static final int TYPE_X = 62;
    private static final int MARKER_Y = 31;
    private static final int MARKER_RGB = 0xD9E5A1;
    private static final int ADULT_TYPE_RGB = 0x010201;
    private static final int BABY_TYPE_RGB = 0x010202;
    private static final int UNDERCOAT_TYPE_RGB = 0x010203;
    private static final int ADULT_BASE_TYPE_RGB = 0x010204;
    private static final int BABY_BASE_TYPE_RGB = 0x010205;
    private static final int BASE_WOOL_OVERLAP_TAG = 249;

    private static volatile boolean enabled = true;

    private DDPolymerSheepShaderPack() {
    }

    static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(
                builder -> builder.addPreFinishTask(DDPolymerSheepShaderPack::install)
        );
    }

    static boolean isEnabled() {
        return enabled;
    }

    static int shaderClass(Sheep sheep) {
        // Keep vanilla's native 16-color jeb_ animation during the first
        // shader experiment. Ordinary custom colors use the residue channel.
        if (sheep.getCustomName() != null && "jeb_".equals(sheep.getCustomName().getString())) {
            return VANILLA_CLASS;
        }
        return shaderClass(sheep.getColor(), sheep.isSheared());
    }

    static int shaderClass(DyeColor color) {
        return shaderClass(color, false);
    }

    static int shaderClass(DyeColor color, boolean sheared) {
        int id = color.getId();
        if (id < 16) {
            return VANILLA_CLASS;
        }
        if (!sheared) {
            return id < 31 ? CUSTOM_UNSHEARED_CLASS : TAN_UNSHEARED_CLASS;
        }
        return id < 31 ? CUSTOM_SHEARED_CLASS : TAN_SHEARED_CLASS;
    }

    static DyeColor donorColor(DyeColor color) {
        return donorColor(color, false);
    }

    static DyeColor donorColor(DyeColor color, boolean sheared) {
        int id = color.getId();
        if (id < 16) {
            return color;
        }
        // A white donor suppresses the native adult undercoat, including the
        // exposed snout/lower-leg remnants. Use all 15 non-white donors and a
        // separate tan class in both coat states.
        return DyeColor.byId(id < 31 ? id - 15 : DyeColor.ORANGE.getId());
    }

    static double encodeScale(double realScale, int residueClass) {
        if (!Double.isFinite(realScale) || realScale <= 0.0) {
            return realScale;
        }
        int normalizedClass = Math.floorMod(residueClass, RESIDUE_CLASS_COUNT);
        long nearest = Math.round(log2(realScale) / LOG_SCALE_STEP);
        long encodedStep = Math.round((nearest - normalizedClass) / (double) RESIDUE_CLASS_COUNT)
                * RESIDUE_CLASS_COUNT + normalizedClass;
        double encoded = Math.pow(2.0, encodedStep * LOG_SCALE_STEP);
        while (encoded < MIN_SCALE) {
            encodedStep += RESIDUE_CLASS_COUNT;
            encoded = Math.pow(2.0, encodedStep * LOG_SCALE_STEP);
        }
        while (encoded > MAX_SCALE) {
            encodedStep -= RESIDUE_CLASS_COUNT;
            encoded = Math.pow(2.0, encodedStep * LOG_SCALE_STEP);
        }
        return encoded;
    }

    static int decodeScaleClass(double encodedScale) {
        long step = Math.round(log2(encodedScale) / LOG_SCALE_STEP);
        return Math.floorMod((int) Math.floorMod(step, RESIDUE_CLASS_COUNT), RESIDUE_CLASS_COUNT);
    }

    static byte[] markTexture(byte[] source, TextureType type) throws IOException {
        BufferedImage original = readTexture(source);
        BufferedImage marked = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                marked.setRGB(x, y, original.getRGB(x, y));
            }
        }

        if (type == TextureType.ADULT) {
            tagCube(marked, 0, 0, 6, 6, 6, 250, 251, 252);
            tagCube(marked, 28, 8, 8, 16, 6, 253, 254, 255);
            tagCube(marked, 0, 16, 4, 6, 4, 251, 252, 252);
        } else if (type == TextureType.UNDERCOAT) {
            tagAdultHiddenRegions(marked);
        }

        int typeRgb = switch (type) {
            case ADULT -> ADULT_TYPE_RGB;
            case BABY -> BABY_TYPE_RGB;
            case UNDERCOAT -> UNDERCOAT_TYPE_RGB;
            case ADULT_BASE -> ADULT_BASE_TYPE_RGB;
            case BABY_BASE -> BABY_BASE_TYPE_RGB;
        };
        marked.setRGB(MARKER_X, MARKER_Y, MARKER_RGB);
        marked.setRGB(TYPE_X, MARKER_Y, typeRgb);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(marked, "PNG", output)) {
            throw new IOException("No PNG writer is available");
        }
        return output.toByteArray();
    }

    static byte[] markBaseTexture(byte[] source, byte[] woolMask, TextureType type) throws IOException {
        if (type != TextureType.ADULT_BASE && type != TextureType.BABY_BASE) {
            throw new IllegalArgumentException("A base texture requires a base texture type");
        }
        BufferedImage mask = readTexture(woolMask);
        byte[] markedBytes = markTexture(source, type);
        BufferedImage marked = readTexture(markedBytes);
        if (type == TextureType.ADULT_BASE) {
            tagAdultHiddenRegions(marked);
        } else {
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    int argb = marked.getRGB(x, y);
                    if ((argb >>> 24) != 0 && (mask.getRGB(x, y) >>> 24) != 0) {
                        marked.setRGB(x, y, BASE_WOOL_OVERLAP_TAG << 24 | argb & 0x00FFFFFF);
                    }
                }
            }
        }
        // Restore the transparent sentinels in case a third-party mask uses
        // either normally empty corner pixel.
        int typeRgb = type == TextureType.ADULT_BASE ? ADULT_BASE_TYPE_RGB : BABY_BASE_TYPE_RGB;
        marked.setRGB(MARKER_X, MARKER_Y, MARKER_RGB);
        marked.setRGB(TYPE_X, MARKER_Y, typeRgb);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(marked, "PNG", output)) {
            throw new IOException("No PNG writer is available");
        }
        return output.toByteArray();
    }

    private static void install(ResourcePackBuilder builder) {
        byte[] existingVertex = builder.getData(VERTEX_SHADER);
        byte[] existingFragment = builder.getData(FRAGMENT_SHADER);
        if (existingVertex != null || existingFragment != null) {
            enabled = false;
            LOGGER.error(
                    "Disabling the native sheep shader because another resource-pack contributor owns {} or {}",
                    VERTEX_SHADER,
                    FRAGMENT_SHADER
            );
            return;
        }

        try {
            byte[] adult = requiredSource(builder, "assets/minecraft/textures/entity/sheep/sheep_wool.png");
            byte[] baby = requiredSource(builder, "assets/minecraft/textures/entity/sheep/sheep_wool_baby.png");
            byte[] undercoat = requiredSource(
                    builder,
                    "assets/minecraft/textures/entity/sheep/sheep_wool_undercoat.png"
            );
            byte[] adultBase = requiredSource(builder, "assets/minecraft/textures/entity/sheep/sheep.png");
            byte[] babyBase = requiredSource(builder, "assets/minecraft/textures/entity/sheep/sheep_baby.png");
            byte[] vertexShader = resource("entity.vsh");
            byte[] fragmentShader = resource("entity.fsh");
            byte[] markedAdult = markTexture(adult, TextureType.ADULT);
            byte[] markedBaby = markTexture(baby, TextureType.BABY);
            byte[] markedUndercoat = markTexture(undercoat, TextureType.UNDERCOAT);
            byte[] markedAdultBase = markBaseTexture(adultBase, undercoat, TextureType.ADULT_BASE);
            byte[] markedBabyBase = markBaseTexture(babyBase, baby, TextureType.BABY_BASE);
            builder.addData(VERTEX_SHADER, vertexShader);
            builder.addData(FRAGMENT_SHADER, fragmentShader);
            builder.addData(
                    "assets/minecraft/textures/entity/sheep/sheep_wool.png",
                    markedAdult
            );
            builder.addData(
                    "assets/minecraft/textures/entity/sheep/sheep_wool_baby.png",
                    markedBaby
            );
            builder.addData(
                    "assets/minecraft/textures/entity/sheep/sheep_wool_undercoat.png",
                    markedUndercoat
            );
            builder.addData("assets/minecraft/textures/entity/sheep/sheep.png", markedAdultBase);
            builder.addData("assets/minecraft/textures/entity/sheep/sheep_baby.png", markedBabyBase);
            enabled = true;
            LOGGER.info("Enabled Dye Depot's experimental native sheep shader");
        } catch (Throwable throwable) {
            enabled = false;
            builder.logError("Unable to install Dye Depot's native sheep shader", throwable);
            LOGGER.error("Disabled the native sheep shader after resource generation failed", throwable);
        }
    }

    private static byte[] requiredSource(ResourcePackBuilder builder, String path) throws IOException {
        byte[] data = builder.getDataOrSource(path);
        if (data == null) {
            throw new IOException("Missing client asset " + path);
        }
        return data;
    }

    private static byte[] resource(String name) throws IOException {
        String path = "/dye_depot/sheep_shader/" + name;
        try (InputStream stream = DDPolymerSheepShaderPack.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing bundled shader " + path);
            }
            return stream.readAllBytes();
        }
    }

    private static BufferedImage readTexture(byte[] source) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
        if (image == null || image.getWidth() != 64 || image.getHeight() != 32) {
            throw new IOException("Expected a 64x32 sheep texture");
        }
        return image;
    }

    private static void tagCube(
            BufferedImage image,
            int u,
            int v,
            int width,
            int height,
            int depth,
            int headOrTopTag,
            int eastWestTag,
            int northSouthTag
    ) {
        // The caller supplies the six metric classes needed by the adult fur
        // mesh. For the isotropic head, and for the leg's equal X/Z axes,
        // several values intentionally coincide.
        int topTag = headOrTopTag;
        int sideEastWestTag = eastWestTag;
        int sideNorthSouthTag = northSouthTag;
        if (u == 0 && v == 0) {
            sideEastWestTag = topTag;
            sideNorthSouthTag = topTag;
        } else if (u == 0 && v == 16) {
            topTag = headOrTopTag;
            sideEastWestTag = eastWestTag;
            sideNorthSouthTag = northSouthTag;
        }

        tagRect(image, u + depth, v, width, depth, topTag);
        tagRect(image, u + depth + width, v, width, depth, topTag);
        tagRect(image, u, v + depth, depth, height, sideEastWestTag);
        tagRect(image, u + depth, v + depth, width, height, sideNorthSouthTag);
        tagRect(image, u + depth + width, v + depth, depth, height, sideEastWestTag);
        tagRect(image, u + depth + width + depth, v + depth, width, height, sideNorthSouthTag);
    }

    private static void tagAdultHiddenRegions(BufferedImage image) {
        // Exact base-geometry texels enclosed by SheepFurModel's inflated
        // adult coat. The exposed snout and lower-leg texels stay opaque.
        tagRect(image, 8, 0, 12, 6, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 0, 8, 6, 6, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 16, 8, 12, 6, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 34, 8, 16, 6, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 28, 14, 28, 16, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 8, 16, 4, 4, BASE_WOOL_OVERLAP_TAG);
        tagRect(image, 0, 20, 16, 6, BASE_WOOL_OVERLAP_TAG);
    }

    private static void tagRect(BufferedImage image, int x, int y, int width, int height, int alpha) {
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                int argb = image.getRGB(px, py);
                if ((argb >>> 24) != 0) {
                    image.setRGB(px, py, alpha << 24 | argb & 0x00FFFFFF);
                }
            }
        }
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }

    enum TextureType {
        ADULT,
        BABY,
        UNDERCOAT,
        ADULT_BASE,
        BABY_BASE
    }
}
