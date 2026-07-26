package com.ninni.dye_depot.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class ResourceTestSupport {

    static final List<String> CUSTOM_COLORS = List.of(
            "maroon",
            "rose",
            "coral",
            "indigo",
            "navy",
            "slate",
            "olive",
            "amber",
            "beige",
            "teal",
            "mint",
            "aqua",
            "verdant",
            "forest",
            "ginger",
            "tan"
    );

    static final List<String> VANILLA_COLORS = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    );

    static final List<String> ALL_COLORS;

    static {
        var colors = new ArrayList<String>(VANILLA_COLORS);
        colors.addAll(CUSTOM_COLORS);
        ALL_COLORS = List.copyOf(colors);
    }

    private static final String MARKER = "dye_depot.mixins.json";
    private static final URL PROJECT_MARKER = locateMarker();

    private ResourceTestSupport() {}

    static JsonObject json(String path) {
        byte[] bytes = bytes(path);
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            assertTrue(parsed.isJsonObject(), () -> path + " must contain a JSON object");
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            return fail("Could not parse " + path, exception);
        }
    }

    static byte[] bytes(String path) {
        String normalized = normalize(path);
        try {
            if ("file".equals(PROJECT_MARKER.getProtocol())) {
                Path root = Path.of(PROJECT_MARKER.toURI()).getParent();
                Path file = root.resolve(normalized.replace('/', java.io.File.separatorChar));
                assertTrue(Files.isRegularFile(file), () -> "Missing classpath resource " + normalized);
                return Files.readAllBytes(file);
            }

            if ("jar".equals(PROJECT_MARKER.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) PROJECT_MARKER.openConnection();
                connection.setUseCaches(false);
                try (JarFile jar = connection.getJarFile()) {
                    JarEntry entry = jar.getJarEntry(normalized);
                    assertNotNull(entry, () -> "Missing classpath resource " + normalized);
                    try (var input = jar.getInputStream(entry)) {
                        return input.readAllBytes();
                    }
                }
            }

            return fail("Unsupported project resource URL protocol: " + PROJECT_MARKER);
        } catch (IOException | URISyntaxException exception) {
            return fail("Could not read classpath resource " + normalized, exception);
        }
    }

    static boolean exists(String path) {
        String normalized = normalize(path);
        try {
            if ("file".equals(PROJECT_MARKER.getProtocol())) {
                Path root = Path.of(PROJECT_MARKER.toURI()).getParent();
                return Files.isRegularFile(root.resolve(normalized.replace('/', java.io.File.separatorChar)));
            }

            if ("jar".equals(PROJECT_MARKER.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) PROJECT_MARKER.openConnection();
                connection.setUseCaches(false);
                try (JarFile jar = connection.getJarFile()) {
                    return jar.getJarEntry(normalized) != null;
                }
            }

            throw new IllegalStateException("Unsupported project resource URL protocol: " + PROJECT_MARKER);
        } catch (IOException | URISyntaxException exception) {
            throw new AssertionError("Could not inspect classpath resource " + normalized, exception);
        }
    }

    static Set<String> filesUnder(String prefix) {
        String normalizedPrefix = normalize(prefix).replaceAll("/+$", "");
        try {
            if ("file".equals(PROJECT_MARKER.getProtocol())) {
                Path root = Path.of(PROJECT_MARKER.toURI()).getParent();
                Path directory = root.resolve(normalizedPrefix.replace('/', java.io.File.separatorChar));
                assertTrue(Files.isDirectory(directory), () -> "Missing classpath resource directory " + normalizedPrefix);
                try (var files = Files.walk(directory)) {
                    var result = new LinkedHashSet<String>();
                    files.filter(Files::isRegularFile)
                            .map(root::relativize)
                            .map(Path::toString)
                            .map(path -> path.replace(java.io.File.separatorChar, '/'))
                            .forEach(result::add);
                    return Collections.unmodifiableSet(result);
                }
            }

            if ("jar".equals(PROJECT_MARKER.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) PROJECT_MARKER.openConnection();
                connection.setUseCaches(false);
                try (JarFile jar = connection.getJarFile()) {
                    var result = new LinkedHashSet<String>();
                    String directoryPrefix = normalizedPrefix + "/";
                    jar.stream()
                            .filter(entry -> !entry.isDirectory())
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith(directoryPrefix))
                            .forEach(result::add);
                    assertTrue(!result.isEmpty(), () -> "Missing classpath resource directory " + normalizedPrefix);
                    return Collections.unmodifiableSet(result);
                }
            }

            return fail("Unsupported project resource URL protocol: " + PROJECT_MARKER);
        } catch (IOException | URISyntaxException exception) {
            return fail("Could not enumerate classpath resources under " + normalizedPrefix, exception);
        }
    }

    static Set<String> jsonFilesUnder(String prefix) {
        var result = new LinkedHashSet<String>();
        filesUnder(prefix).stream().filter(path -> path.endsWith(".json")).forEach(result::add);
        return Collections.unmodifiableSet(result);
    }

    static JsonObject requiredObject(JsonObject parent, String member, String path) {
        assertTrue(parent.has(member), () -> path + " is missing object member '" + member + "'");
        assertTrue(parent.get(member).isJsonObject(), () -> path + " member '" + member + "' must be an object");
        return parent.getAsJsonObject(member);
    }

    static JsonArray requiredArray(JsonObject parent, String member, String path) {
        assertTrue(parent.has(member), () -> path + " is missing array member '" + member + "'");
        assertTrue(parent.get(member).isJsonArray(), () -> path + " member '" + member + "' must be an array");
        return parent.getAsJsonArray(member);
    }

    static String requiredString(JsonObject parent, String member, String path) {
        assertTrue(parent.has(member), () -> path + " is missing string member '" + member + "'");
        assertTrue(parent.get(member).isJsonPrimitive(), () -> path + " member '" + member + "' must be a string");
        assertTrue(parent.getAsJsonPrimitive(member).isString(), () -> path + " member '" + member + "' must be a string");
        return parent.get(member).getAsString();
    }

    static Set<String> tagValues(String path) {
        JsonArray values = requiredArray(json(path), "values", path);
        var result = new LinkedHashSet<String>();
        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                result.add(value.getAsString());
            } else {
                JsonObject entry = value.getAsJsonObject();
                result.add(requiredString(entry, "id", path));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    static Set<String> allStrings(JsonElement element) {
        var result = new LinkedHashSet<String>();
        collectStrings(element, result);
        return Collections.unmodifiableSet(result);
    }

    static void assertAllJsonParses(String prefix) {
        Set<String> files = jsonFilesUnder(prefix);
        assertTrue(!files.isEmpty(), () -> "Expected JSON resources under " + prefix);
        for (String path : files) {
            json(path);
        }
    }

    static String resourceId(String namespace, String path) {
        int namespaceStart = path.indexOf("data/") + "data/".length();
        int namespaceEnd = path.indexOf('/', namespaceStart);
        int typeEnd = path.indexOf('/', namespaceEnd + 1);
        String relative = path.substring(typeEnd + 1, path.length() - ".json".length());
        return namespace + ":" + relative;
    }

    private static void collectStrings(JsonElement element, Set<String> result) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                result.add(element.getAsString());
            }
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectStrings(child, result));
            return;
        }
        element.getAsJsonObject().entrySet().forEach(entry -> collectStrings(entry.getValue(), result));
    }

    private static URL locateMarker() {
        URL marker = ResourceTestSupport.class.getClassLoader().getResource(MARKER);
        assertNotNull(marker, "Could not locate the processed Dye Depot resource root");
        return marker;
    }

    private static String normalize(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
