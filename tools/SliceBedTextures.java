import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Converts pre-26.2 bed entity textures to the seven block textures used by 26.2.
 *
 * <p>The crop and transform layout is the one used by Mojang Slicer 1.2.1 for
 * the 26.2 resource-pack migration:
 * https://github.com/Mojang/slicer/blob/v1.2.1/26.2/src/main/java/com/mojang/slicer/Main.java
 *
 * <p>Usage:
 * <pre>
 * java tools/SliceBedTextures.java INPUT_DIRECTORY OUTPUT_DIRECTORY
 * java tools/SliceBedTextures.java --verify-vanilla OLD_MINECRAFT_JAR NEW_MINECRAFT_JAR
 * </pre>
 */
public final class SliceBedTextures {
    private static final List<String> VANILLA_COLORS = List.of(
        "white", "orange", "magenta", "light_blue",
        "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue",
        "brown", "green", "red", "black"
    );

    private static final List<String> OUTPUT_SUFFIXES = List.of(
        "head_up",
        "foot_up",
        "foot_south",
        "foot_west",
        "foot_east",
        "head_east",
        "head_west"
    );

    private SliceBedTextures() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("--verify-vanilla")) {
            verifyVanilla(Path.of(args[1]), Path.of(args[2]));
            return;
        }

        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Usage: java tools/SliceBedTextures.java INPUT_DIRECTORY OUTPUT_DIRECTORY"
            );
        }

        convertDirectory(Path.of(args[0]), Path.of(args[1]));
    }

    private static void convertDirectory(Path inputDirectory, Path outputDirectory) throws IOException {
        if (!Files.isDirectory(inputDirectory)) {
            throw new IllegalArgumentException("Input is not a directory: " + inputDirectory);
        }

        List<Path> inputs;
        try (var stream = Files.list(inputDirectory)) {
            inputs = stream
                .filter(path -> path.getFileName().toString().endsWith(".png"))
                .sorted()
                .toList();
        }

        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("No PNG textures found in " + inputDirectory);
        }

        Files.createDirectories(outputDirectory);
        Set<String> writtenNames = new TreeSet<>();

        for (Path input : inputs) {
            BufferedImage source = readImage(input);
            requireLegacyDimensions(input.toString(), source);
            String fileName = input.getFileName().toString();
            String color = fileName.substring(0, fileName.length() - ".png".length());

            for (var output : slice(source).entrySet()) {
                String outputName = color + "_bed_" + output.getKey() + ".png";
                Path outputPath = outputDirectory.resolve(outputName);
                if (!ImageIO.write(output.getValue(), "png", outputPath.toFile())) {
                    throw new IOException("No PNG image writer is available");
                }
                assertPixelEquivalent(outputName, output.getValue(), readImage(outputPath));
                writtenNames.add(outputName);
            }
        }

        System.out.printf(
            "Converted %d legacy bed textures into %d block textures in %s%n",
            inputs.size(),
            writtenNames.size(),
            outputDirectory
        );
    }

    private static Map<String, BufferedImage> slice(BufferedImage source) {
        Map<String, BufferedImage> outputs = new LinkedHashMap<>();

        outputs.put("head_up", outputWith(piece(source, 6, 6, 16, 16), 0, 0));
        outputs.put("foot_up", outputWith(piece(source, 6, 28, 16, 16), 0, 0));

        BufferedImage footSouth = transparentOutput();
        paste(footSouth, flipTopBottom(piece(source, 22, 22, 16, 6)), 0, 7);
        paste(footSouth, piece(source, 53, 3, 3, 3), 0, 13);
        paste(footSouth, piece(source, 56, 3, 3, 3), 3, 13);
        paste(footSouth, piece(source, 59, 15, 3, 3), 10, 13);
        paste(footSouth, piece(source, 50, 15, 3, 3), 13, 13);
        outputs.put("foot_south", footSouth);

        BufferedImage footWest = transparentOutput();
        paste(footWest, rotateCounterClockwise(piece(source, 0, 28, 6, 16)), 0, 7);
        paste(footWest, flipTopBottom(piece(source, 56, 0, 3, 3)), 7, 13);
        paste(footWest, piece(source, 59, 3, 3, 3), 10, 13);
        paste(footWest, piece(source, 50, 3, 3, 3), 13, 13);
        outputs.put("foot_west", footWest);

        BufferedImage footEast = transparentOutput();
        paste(footEast, rotateClockwise(piece(source, 22, 28, 6, 16)), 0, 7);
        paste(footEast, piece(source, 53, 15, 3, 3), 0, 13);
        paste(footEast, piece(source, 56, 15, 3, 3), 3, 13);
        paste(
            footEast,
            flipTopBottom(rotateCounterClockwise(piece(source, 56, 12, 3, 3))),
            6,
            13
        );
        outputs.put("foot_east", footEast);

        BufferedImage headEast = transparentOutput();
        paste(headEast, rotateClockwise(piece(source, 22, 6, 6, 16)), 0, 7);
        paste(
            headEast,
            flipTopBottom(rotate180(piece(source, 56, 18, 3, 3))),
            7,
            13
        );
        paste(headEast, piece(source, 59, 21, 3, 3), 10, 13);
        paste(headEast, piece(source, 50, 21, 3, 3), 13, 13);
        outputs.put("head_east", headEast);

        BufferedImage headWest = transparentOutput();
        paste(headWest, rotateCounterClockwise(piece(source, 0, 6, 6, 16)), 0, 7);
        paste(headWest, piece(source, 53, 9, 3, 3), 0, 13);
        paste(headWest, piece(source, 56, 9, 3, 3), 3, 13);
        paste(
            headWest,
            flipTopBottom(rotateClockwise(piece(source, 56, 6, 3, 3))),
            6,
            13
        );
        outputs.put("head_west", headWest);

        return outputs;
    }

    private static BufferedImage outputWith(
        BufferedImage piece,
        int destinationX,
        int destinationY
    ) {
        BufferedImage output = transparentOutput();
        paste(output, piece, destinationX, destinationY);
        return output;
    }

    private static BufferedImage piece(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int sourceY = 0; sourceY < height; sourceY++) {
            for (int sourceX = 0; sourceX < width; sourceX++) {
                result.setRGB(sourceX, sourceY, source.getRGB(x + sourceX, y + sourceY));
            }
        }
        return result;
    }

    private static BufferedImage transparentOutput() {
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    private static void paste(BufferedImage destination, BufferedImage source, int x, int y) {
        if (x < 0 || y < 0
            || x + source.getWidth() > destination.getWidth()
            || y + source.getHeight() > destination.getHeight()) {
            throw new IllegalArgumentException("Image piece does not fit in its destination");
        }

        for (int sourceY = 0; sourceY < source.getHeight(); sourceY++) {
            for (int sourceX = 0; sourceX < source.getWidth(); sourceX++) {
                destination.setRGB(x + sourceX, y + sourceY, source.getRGB(sourceX, sourceY));
            }
        }
    }

    private static BufferedImage flipTopBottom(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                result.setRGB(x, source.getHeight() - 1 - y, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage rotateClockwise(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getHeight(),
            source.getWidth(),
            BufferedImage.TYPE_INT_ARGB
        );
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                result.setRGB(source.getHeight() - 1 - y, x, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage rotateCounterClockwise(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getHeight(),
            source.getWidth(),
            BufferedImage.TYPE_INT_ARGB
        );
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                result.setRGB(y, source.getWidth() - 1 - x, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage rotate180(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                result.setRGB(
                    source.getWidth() - 1 - x,
                    source.getHeight() - 1 - y,
                    source.getRGB(x, y)
                );
            }
        }
        return result;
    }

    private static void verifyVanilla(Path oldJar, Path newJar) throws IOException {
        int verified = 0;
        try (ZipFile oldZip = new ZipFile(oldJar.toFile());
             ZipFile newZip = new ZipFile(newJar.toFile())) {
            for (String color : VANILLA_COLORS) {
                String oldEntry = "assets/minecraft/textures/entity/bed/" + color + ".png";
                BufferedImage source = readImage(oldZip, oldEntry);
                requireLegacyDimensions(oldEntry, source);
                Map<String, BufferedImage> generated = slice(source);

                for (String suffix : OUTPUT_SUFFIXES) {
                    String newEntry =
                        "assets/minecraft/textures/block/" + color + "_bed_" + suffix + ".png";
                    BufferedImage expected = readImage(newZip, newEntry);
                    assertPixelEquivalent(newEntry, expected, generated.get(suffix));
                    verified++;
                }
            }
        }

        System.out.printf(
            "Verified all %d vanilla 26.2 bed textures pixel-for-pixel against converted legacy textures%n",
            verified
        );
    }

    private static BufferedImage readImage(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return readImage(input, path.toString());
        }
    }

    private static BufferedImage readImage(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Missing archive entry: " + entryName);
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return readImage(input, entryName);
        }
    }

    private static BufferedImage readImage(InputStream input, String sourceName) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Could not decode PNG image: " + sourceName);
        }
        return image;
    }

    private static void requireLegacyDimensions(String sourceName, BufferedImage image) {
        if (image.getWidth() != 64 || image.getHeight() != 64) {
            throw new IllegalArgumentException(
                sourceName + " is " + image.getWidth() + "x" + image.getHeight()
                    + "; expected a 64x64 legacy bed texture"
            );
        }
    }

    private static void assertPixelEquivalent(
        String name,
        BufferedImage expected,
        BufferedImage actual
    ) {
        if (expected.getWidth() != actual.getWidth()
            || expected.getHeight() != actual.getHeight()) {
            throw new AssertionError(
                name + " has unexpected dimensions: "
                    + expected.getWidth() + "x" + expected.getHeight()
                    + " instead of " + actual.getWidth() + "x" + actual.getHeight()
            );
        }

        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                if (expected.getRGB(x, y) != actual.getRGB(x, y)) {
                    throw new AssertionError(
                        name + " differs at pixel " + x + "," + y
                            + ": expected 0x" + Integer.toHexString(expected.getRGB(x, y))
                            + ", got 0x" + Integer.toHexString(actual.getRGB(x, y))
                    );
                }
            }
        }
    }
}
