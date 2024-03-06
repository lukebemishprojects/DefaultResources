/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.defaultresources.api.OutdatedResourcesListener;
import dev.lukebemish.defaultresources.api.ResourceProvider;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class DefaultResources {
    public static final String MOD_ID = "defaultresources";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final int BUFFER_SIZE = 1024;
    public static final Map<String, Optional<String>> OUTDATED_TARGETS = new ConcurrentHashMap<>();
    public static final Map<String, Optional<String>> MOD_TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, List<OutdatedResourcesListener>> OUTDATED_RESOURCES_LISTENERS = new ConcurrentHashMap<>();
    public static final String META_FILE_PATH = DefaultResources.MOD_ID + ".meta.json";
    public static final String CHECK_FILE_PATH = "." + DefaultResources.MOD_ID;

    public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public volatile static ResourceProvider RESOURCE_PROVIDER;

    private static final Map<String, ResourceProvider> QUEUED_PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, BiFunction<String, PackType, Supplier<PackResources>>> QUEUED_RESOURCES = new ConcurrentHashMap<>();

    public static void addListener(String modId, OutdatedResourcesListener listener) {
        OUTDATED_RESOURCES_LISTENERS.computeIfAbsent(modId, s -> new ArrayList<>()).add(listener);
    }

    public synchronized static void delegate(Runnable ifInitialized, Runnable ifUninitialized) {
        if (RESOURCE_PROVIDER == null) {
            ifInitialized.run();
        } else {
            ifUninitialized.run();
        }
    }

    public synchronized static ResourceProvider assembleResourceProvider() {
        List<ResourceProvider> providers = new ArrayList<>();
        QUEUED_PROVIDERS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).forEach(providers::add);
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
        Config.INSTANCE.get().fromResourcePacks().forEach((name, enabled) -> {
            if (enabled) {
                Path path = Services.PLATFORM.getResourcePackDir().resolve(name);
                if (Files.isDirectory(path)) {
                    providers.add(new PathResourceProvider(path));
                } else if (Files.isRegularFile(path)) {
                    providers.add(new ZipResourceProvider(path));
                } else {
                    return;
                }
                DefaultResources.LOGGER.info("Added resource pack \"{}\" to global resource providers", name);
            }
        });
        return new GroupedResourceProvider(providers);
    }

    public static void forMod(Function<String, Path> inJarPathGetter, String modId) {
        Path defaultResourcesMeta = inJarPathGetter.apply(META_FILE_PATH);
        ModMetaFile meta;
        if (Files.exists(defaultResourcesMeta)) {
            try (InputStream is = Files.newInputStream(defaultResourcesMeta)) {
                JsonObject obj = GSON.fromJson(new BufferedReader(new InputStreamReader(is)), JsonObject.class);
                meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {
                });
            } catch (IOException | RuntimeException e) {
                DefaultResources.LOGGER.error("Could not read meta file for mod {}", modId, e);
                return;
            }
        } else {
            try {
                meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow(false, e -> {});
            } catch (RuntimeException e) {
                DefaultResources.LOGGER.error("Could not parse default meta file", e);
                return;
            }
        }
        Path defaultResources = inJarPathGetter.apply(meta.resourcesPath());

        try {
            if (Files.exists(defaultResources)) {
                MOD_TARGETS.put(modId, meta.dataVersion());
                var defaultExtraction = meta.extract() ? Config.ExtractionState.EXTRACT : Config.ExtractionState.UNEXTRACTED;
                Config.ExtractionState extractionState = Config.INSTANCE.get().extract().getOrDefault(modId, defaultExtraction);
                if (extractionState == Config.ExtractionState.OUTDATED) {
                    extractionState = defaultExtraction;
                }
                if (!Config.INSTANCE.get().extract().containsKey(modId)) {
                    Config.INSTANCE.get().extract().put(modId, defaultExtraction);
                }
                if (extractionState == Config.ExtractionState.UNEXTRACTED) {
                    QUEUED_PROVIDERS.put(modId, new PathResourceProvider(defaultResources));
                    QUEUED_RESOURCES.put("__extracted_" + modId, (s, type) -> {
                        if (!Files.exists(defaultResources.resolve(type.getDirectory()))) return null;
                        return () -> new AutoMetadataFolderPackResources(s, type, defaultResources);
                    });
                } else if (extractionState == Config.ExtractionState.EXTRACT) {
                    Config.INSTANCE.get().extract().put(modId, meta.extract() ? Config.ExtractionState.EXTRACT : Config.ExtractionState.EXTRACTED);
                    if (!meta.zip()) {
                        Path outPath = Services.PLATFORM.getGlobalFolder().resolve(modId);
                        String checksum = shouldCopy(defaultResources, outPath, Files.exists(outPath), modId, meta);
                        if (checksum != null) {
                            copyResources(defaultResources, outPath, checksum, meta.dataVersion().orElse(null));
                        }
                    } else {
                        Path zipPath = Services.PLATFORM.getGlobalFolder().resolve(modId + ".zip");
                        boolean zipExists = Files.exists(zipPath);
                        String checksum;
                        try (FileSystem zipFs = FileSystems.newFileSystem(
                            zipPath,
                            Collections.singletonMap("create", "true"))
                        ) {
                            Path outPath = zipFs.getPath("/");
                            checksum = shouldCopy(defaultResources, outPath, zipExists, modId, meta);
                            if (checksum != null && !zipExists) {
                                copyResources(defaultResources, outPath, checksum, meta.dataVersion().orElse(null));
                            }
                        }
                        if (checksum != null && zipExists) {
                            Files.delete(zipPath);
                            try (FileSystem zipFs = FileSystems.newFileSystem(
                                zipPath,
                                Collections.singletonMap("create", "true"))) {
                                Path outPath = zipFs.getPath("/");
                                copyResources(defaultResources, outPath, checksum, meta.dataVersion().orElse(null));
                            }
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            DefaultResources.LOGGER.error("Could not handle default resources for mod {}", modId, e);
        }
    }

    private static void couldNotUpdate(String modId, Path outPath, ModMetaFile meta) {
        String oldDataVersion;
        try {
            oldDataVersion = dataVersion(outPath);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not read old data version for mod {}", modId, e);
            oldDataVersion = null;
        }
        DefaultResources.LOGGER.error("Could not extract default resources for mod {} (data version {} to version {}) because they are already extracted and have been changed on disk", modId, oldDataVersion, meta.dataVersion().orElse(null));
        OUTDATED_TARGETS.put(modId, Optional.ofNullable(oldDataVersion));
        Config.INSTANCE.get().extract().put(modId, Config.ExtractionState.OUTDATED);
    }

    private static @Nullable String shouldCopy(Path defaultResources, Path outPath, boolean alreadyExists, String modId, ModMetaFile meta) {
        try {
            if (alreadyExists) {
                Path checksumPath = outPath.resolve(CHECK_FILE_PATH);
                String oldChecksum;
                String oldVersion;
                if (Files.exists(checksumPath)) {
                    var parts = Files.readString(checksumPath).split(":", 2);
                    oldChecksum = parts[0];
                    if (parts.length == 2) {
                        oldVersion = parts[1];
                    } else {
                        oldVersion = null;
                    }
                } else {
                    couldNotUpdate(modId, outPath, meta);
                    return null;
                }
                String newChecksum = checkPath(defaultResources);
                String newVersion = meta.dataVersion().orElse(null);
                if (newChecksum.equals(oldChecksum) && Objects.equals(newVersion, oldVersion)) {
                    // The resources to extract have not changed, but the extracted resources have been modified
                    return null;
                } else {
                    // The resources to extract differ from the saved checksum
                    String newExtractedChecksum = checkPath(outPath);
                    if (newExtractedChecksum.equals(oldChecksum)) {
                        // The calculated extracted checksum does not differ from the saved checksum
                        return newChecksum;
                    }
                }
            } else {
                return checkPath(defaultResources);
            }
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Error checking compatibility of resources from {} targeted at {}", defaultResources, outPath, e);
        }
        couldNotUpdate(modId, outPath, meta);
        return null;
    }

    private static @Nullable String dataVersion(Path path) throws IOException {
        Path checksumPath = path.resolve(CHECK_FILE_PATH);
        if (Files.exists(checksumPath)) {
            var parts = Files.readString(checksumPath).split(":", 2);
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }

    private static String checkPath(Path path) throws IOException {
        StringBuilder newChecksum = new StringBuilder();
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.comparing(p -> path.relativize(p).toString())).forEach(p -> {
                try {
                    if (!Files.isDirectory(p) && !(path.relativize(p).getNameCount() == 1 && p.endsWith(CHECK_FILE_PATH))) {
                        Checksum check = new Adler32();
                        try (var is = Files.newInputStream(p)) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int length;
                            while ((length = is.read(buffer)) > 0) {
                                check.update(buffer, 0, length);
                            }
                        }
                        newChecksum.append(encode((int) check.getValue()));
                    }
                } catch (IOException e) {
                    DefaultResources.LOGGER.error("Error calculating checksum at {}", p, e);
                }
            });
        }
        return newChecksum.toString();
    }

    private static CharSequence encode(int i) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 4; j++) {
            sb.append((char) (((i >> (j * 4)) & 0xF) + 97));
        }
        return sb;
    }

    private static void copyResources(Path defaultResources, Path outPath, String checksum, @Nullable String dataVersion) {
        try (var walk = Files.walk(defaultResources)) {
            walk.sorted(Comparator.comparing(p -> p.relativize(defaultResources).toString())).forEach(p -> {
                try {
                    if (!Files.isDirectory(p)) {
                        String rel = defaultResources.relativize(p).toString();
                        Path newPath = outPath.resolve(rel);
                        if (!Files.exists(newPath.getParent())) Files.createDirectories(newPath.getParent());
                        Files.copy(p, newPath);
                    }
                } catch (IOException e) {
                    DefaultResources.LOGGER.error("Error checking compatibility of resources from {} targeted at {}, for path {}", defaultResources, outPath, p, e);
                }
            });
            Path checksumPath = outPath.resolve(CHECK_FILE_PATH);
            Files.writeString(checksumPath, checksum + (dataVersion == null ? "" : ":" + dataVersion));
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Error checking compatibility of resources from {} targeted at {}", defaultResources, outPath, e);
        }
    }

    public static void cleanupExtraction() {
        Config.INSTANCE.get().save();
        for (var entry : OUTDATED_TARGETS.entrySet()) {
            String oldVersion = MOD_TARGETS.getOrDefault(entry.getKey(), Optional.empty()).orElse(null);
            String newVersion = entry.getValue().orElse(null);
            String modId = entry.getKey();
            OUTDATED_RESOURCES_LISTENERS.getOrDefault(modId, List.of()).forEach(listener -> listener.resourcesOutdated(oldVersion, newVersion));
        }
    }

    @NotNull
    public static List<Pair<String, Pack.ResourcesSupplier>> getPackResources(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFolderPackResources(s, type, file);
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFilePackResources(s, type, file.toFile());
                    packs.add(new Pair<>(file.getFileName().toString(), packResources));
                }
            }
        } catch (IOException ignored) {

        }
        QUEUED_RESOURCES.forEach((s, biFunction) -> {
            Supplier<PackResources> resources = biFunction.apply(s, type);
            if (resources == null) return;
            packs.add(new Pair<>(s, str -> resources.get()));
        });
        return packs;
    }
}
