package dev.lukebemish.defaultresources.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.defaultresources.api.ModMetaFile;
import dev.lukebemish.defaultresources.api.ResourceProvider;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DefaultResources {
    public static final String MOD_ID = "defaultresources";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final String META_FILE_PATH = DefaultResources.MOD_ID+".meta.json";

    public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static ResourceProvider RESOURCE_PROVIDER;

    private static final List<ResourceProvider> QUEUED_PROVIDERS = new ArrayList<>();
    private static final Map<String, BiFunction<String, PackType, PackResources>> QUEUED_RESOURCES = new HashMap<>();

    public static ResourceProvider assembleResourceProvider() {
        List<ResourceProvider> providers = new ArrayList<>(QUEUED_PROVIDERS);
        try (var paths = Files.list(Services.PLATFORM.getGlobalFolder())) {
            paths.forEach(path -> {
                if (Files.isDirectory(path)) {
                    providers.add(new PathResourceProvider(path));
                } else if (path.getFileName().toString().endsWith(".zip")) {
                    providers.add(new ZipResourceProvider(path));
                }
            });
        } catch (IOException ignored) {
        }
        providers.addAll(Services.PLATFORM.getJarProviders());
        return new GroupedResourceProvider(providers);
    }

    public static void forMod(Path configDir, Function<String, Path> inJarPathGetter, String modId) {
        Path defaultResourcesMeta = inJarPathGetter.apply(META_FILE_PATH);
        if (Files.exists(defaultResourcesMeta)) {
            try (InputStream is = Files.newInputStream(defaultResourcesMeta)) {
                JsonObject obj = GSON.fromJson(new BufferedReader(new InputStreamReader(is)),JsonObject.class);
                ModMetaFile meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {});
                Path defaultResources = inJarPathGetter.apply(meta.resourcesPath());

                if (Files.exists(defaultResources)) {
                    Config.ExtractionState extractionState = Config.INSTANCE.get().extract().getOrDefault(modId, Config.ExtractionState.UNEXTRACTED);
                    if (extractionState == Config.ExtractionState.UNEXTRACTED) {
                        QUEUED_PROVIDERS.add(new PathResourceProvider(defaultResources));
                        QUEUED_RESOURCES.put("__extracted_"+modId, (s, type) -> new AutoMetadataFolderPackResources(s, type, defaultResources));
                    } else if ((!Files.exists(configDir.resolve(meta.configPath())) && extractionState.extractIfMissing) || extractionState.extractRegardless) {
                        Config.INSTANCE.get().extract().put(modId, Config.ExtractionState.EXTRACTED);
                        if (!meta.zip()) {
                            Path outPath = Services.PLATFORM.getGlobalFolder().resolve(modId);
                            if (!Files.exists(outPath))
                                copyResources(defaultResources, outPath);
                        } else {
                            try (FileSystem zipFs = FileSystems.newFileSystem(
                                    URI.create("jar:" + Services.PLATFORM.getGlobalFolder().resolve(modId + ".zip").toAbsolutePath().toUri()),
                                    Collections.singletonMap("create", "true"))) {
                                Path outPath = zipFs.getPath("/");
                                copyResources(defaultResources, outPath);
                            }
                        }
                        if (meta.createsMarker() && !Files.exists(configDir.resolve(meta.configPath()))) {
                            try {
                                Path markerPath = configDir.resolve(meta.configPath());
                                String comment;
                                if (meta.configPath().endsWith(".json5") || meta.configPath().endsWith(".json"))
                                    comment = "// ";
                                else if (meta.configPath().endsWith(".toml"))
                                    comment = "# ";
                                else
                                    comment = "";
                                Files.writeString(markerPath, comment + "This is a marker file created by "+modId+". If the mod is marked as already extracted, default resources will not be re-extracted while this file exists.\n");
                            } catch (IOException e) {
                                LOGGER.error("Issues writing marker file at {} for mod {}: ",meta.configPath(), modId, e);
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                DefaultResources.LOGGER.error("Could not read meta file for mod {}",modId,e);
            }
        }
    }

    private static void copyResources(Path defaultResources, Path outPath) {
        try (var walk = Files.walk(defaultResources)) {
            walk.forEach(p -> {
                try {
                    if (!Files.isDirectory(p)) {
                        String rel = defaultResources.relativize(p).toString();
                        Path newPath = outPath.resolve(rel);
                        if (!Files.exists(newPath.getParent())) Files.createDirectories(newPath.getParent());
                        Files.copy(p, newPath);
                    }
                } catch (IOException e) {
                    DefaultResources.LOGGER.error(e);
                }
            });
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
    }

    public static void cleanupExtraction() {
        Config.INSTANCE.get().save();
    }

    @NotNull
    public static List<Pair<String, Pack.ResourcesSupplier>> getPackResources(PackType type) {
        List<Pair<String,Pack.ResourcesSupplier>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFolderPackResources(s, type, file);
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFilePackResources(s, type, file.toFile());
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                }
            }
        } catch (IOException ignored) {

        }
        QUEUED_RESOURCES.forEach((s, biFunction) -> packs.add(new Pair<>(s, str ->biFunction.apply(str, type))));
        return packs;
    }
}
