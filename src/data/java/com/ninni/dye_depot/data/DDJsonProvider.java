package com.ninni.dye_depot.data;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Small base for identifier-only data that cannot use registry-backed vanilla
 * builders (primarily optional-mod resources).
 */
public abstract class DDJsonProvider implements DataProvider {

    protected final PackOutput output;

    protected DDJsonProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache) {
        Map<Path, JsonElement> files = new LinkedHashMap<>();
        generate((path, json) -> {
            if (files.put(path, json) != null) {
                throw new IllegalStateException("Duplicate generated file: " + path);
            }
        });
        return CompletableFuture.allOf(files.entrySet().stream()
                .map(entry -> DataProvider.saveStable(cache, entry.getValue(), entry.getKey()))
                .toArray(CompletableFuture[]::new));
    }

    protected abstract void generate(Output output);

    protected Path asset(String namespace, String folder, String id) {
        return output.getOutputFolder()
                .resolve("assets")
                .resolve(namespace)
                .resolve(folder)
                .resolve(id + ".json");
    }

    protected Path data(String namespace, String folder, String id) {
        return output.getOutputFolder()
                .resolve("data")
                .resolve(namespace)
                .resolve(folder)
                .resolve(id + ".json");
    }

    @FunctionalInterface
    protected interface Output {
        void accept(Path path, JsonElement json);
    }
}
