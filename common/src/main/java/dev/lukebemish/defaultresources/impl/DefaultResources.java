/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.defaultresources.api.GlobalResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

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
import java.util.function.Supplier;

public class DefaultResources {
    public static final String MOD_ID = "defaultresources";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final String META_FILE_PATH = DefaultResources.MOD_ID + ".meta.json";

    public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static final Map<String, BiFunction<String, PackType, Supplier<PackResources>>> QUEUED_RESOURCES = new HashMap<>();
    private static final Map<String, BiFunction<String, PackType, Supplier<PackResources>>> QUEUED_STATIC_RESOURCES = new HashMap<>();
    public static final String GLOBAL_PREFIX = "global";

    public static final GlobalResourceManager STATIC_ASSETS = createStaticResourceManager(PackType.CLIENT_RESOURCES);
    public static final GlobalResourceManager STATIC_DATA = createStaticResourceManager(PackType.SERVER_DATA);

    public static void forMod(Function<String, Path> inJarPathGetter, String modId) {
        Path defaultResourcesMeta = inJarPathGetter.apply(META_FILE_PATH);
        if (Files.exists(defaultResourcesMeta)) {
            try (InputStream is = Files.newInputStream(defaultResourcesMeta)) {
                JsonObject obj = GSON.fromJson(new BufferedReader(new InputStreamReader(is)), JsonObject.class);
                ModMetaFile meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {
                });
                Path defaultResources = inJarPathGetter.apply(meta.resourcesPath());

                if (Files.exists(defaultResources)) {
                    Config.ExtractionState extractionState = Config.INSTANCE.get().extract().getOrDefault(modId, Config.ExtractionState.UNEXTRACTED);
                    if (extractionState == Config.ExtractionState.UNEXTRACTED) {
                        QUEUED_RESOURCES.put("__unextracted_" + modId, (s, type) -> {
                            if (!Files.exists(defaultResources.resolve(type.getDirectory()))) return null;
                            return () -> new AutoMetadataPathPackResources(s, "", defaultResources, type);
                        });
                        QUEUED_STATIC_RESOURCES.put("__unextracted_" + modId, (s, type) -> {
                            if (!Files.exists(defaultResources.resolve(GLOBAL_PREFIX+type.getDirectory()))) return null;
                            return () -> new AutoMetadataPathPackResources(s, GLOBAL_PREFIX, defaultResources, type);
                        });
                    } else if (extractionState == Config.ExtractionState.EXTRACT) {
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
                    }
                }
            } catch (IOException | RuntimeException e) {
                DefaultResources.LOGGER.error("Could not read meta file for mod {}", modId, e);
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

    public static Pack.ResourcesSupplier wrap(Function<String, PackResources> function) {
        return new Pack.ResourcesSupplier() {
            @Override
            public @NonNull PackResources openPrimary(String string) {
                return function.apply(string);
            }

            @Override
            public @NonNull PackResources openFull(String string, Pack.Info info) {
                return function.apply(string);
            }
        };
    }

    @NonNull
    public static List<Pair<String, Pack.ResourcesSupplier>> getPackResources(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Pack.ResourcesSupplier packResources = wrap(s -> new AutoMetadataPathPackResources(s, "", file, type));
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Pack.ResourcesSupplier packResources = wrap(s -> new AutoMetadataPathPackResources(s, "", file, type));
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                }
            }
        } catch (IOException ignored) {

        }
        QUEUED_RESOURCES.forEach((s, biFunction) -> {
            Supplier<PackResources> resources = biFunction.apply(s, type);
            if (resources == null) return;
            packs.add(new Pair<>(s, wrap(str -> resources.get())));
        });
        return packs;
    }

    @NonNull
    private static List<Pair<String, Pack.ResourcesSupplier>> getStaticPackResources(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Pack.ResourcesSupplier packResources = wrap(s -> new AutoMetadataPathPackResources(s, GLOBAL_PREFIX, file, type));
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Pack.ResourcesSupplier packResources = wrap(s -> new AutoMetadataPathPackResources(s, GLOBAL_PREFIX, file, type));
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                }
            }
        } catch (IOException ignored) {

        }
        packs.addAll(getDetectedPacks(type));
        QUEUED_STATIC_RESOURCES.forEach((s, biFunction) -> {
            Supplier<PackResources> resources = biFunction.apply(s, type);
            if (resources == null) return;
            packs.add(new Pair<>(s, wrap(str -> resources.get())));
        });
        return packs;
    }

    private static List<Pair<String, Pack.ResourcesSupplier>> getDetectedPacks(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> packs = new ArrayList<>();
        Config.INSTANCE.get().fromResourcePacks().forEach((name, enabled) -> {
            if (enabled) {
                Path path = Services.PLATFORM.getResourcePackDir().resolve(name);
                if (Files.isDirectory(path)) {
                    packs.add(Pair.of(name, wrap(n -> new AutoMetadataPathPackResources(n, GLOBAL_PREFIX, path, type))));
                } else if (Files.isRegularFile(path)) {
                    packs.add(Pair.of(name, wrap(n -> new AutoMetadataFilePackResources(n, GLOBAL_PREFIX, path, type))));
                } else {
                    return;
                }
                DefaultResources.LOGGER.info("Added resource pack \"{}\" to global {} resource providers", name, type.getDirectory());
            }
        });
        return packs;
    }

    private volatile static boolean GLOBAL_SETUP = false;

    public synchronized static void initialize() {
        if (!GLOBAL_SETUP) {
            GLOBAL_SETUP = true;
            Services.PLATFORM.extractResources();
            DefaultResources.cleanupExtraction();
        }
    }

    public synchronized static GlobalResourceManager createStaticResourceManager(PackType type) {
        initialize();
        List<Pair<String, Pack.ResourcesSupplier>> sources = new ArrayList<>(getStaticPackResources(type));
        sources.addAll(Services.PLATFORM.getJarProviders(type));
        return new CombinedResourceManager(type, sources);
    }
}
