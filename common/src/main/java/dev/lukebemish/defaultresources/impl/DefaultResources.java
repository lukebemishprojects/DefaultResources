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
import dev.lukebemish.defaultresources.api.OutdatedResourcesListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private static final Set<String> OUTDATED_TARGETS = new HashSet<>();
    private volatile static boolean GLOBAL_SETUP = false;
    private static final Map<String, List<OutdatedResourcesListener>> OUTDATED_RESOURCES_LISTENERS = new ConcurrentHashMap<>();
    public static final String META_FILE_PATH = DefaultResources.MOD_ID + ".meta.json";
    public static final String CHECK_FILE_PATH = DefaultResources.MOD_ID + ".checksum";

    public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static final Map<String, BiFunction<String, PackType, Supplier<PackResources>>> QUEUED_RESOURCES = new ConcurrentHashMap<>();
    private static final Map<String, BiFunction<String, PackType, Supplier<PackResources>>> QUEUED_STATIC_RESOURCES = new ConcurrentHashMap<>();
    public static final String GLOBAL_PREFIX = "global";

    public static final GlobalResourceManager STATIC_ASSETS = createStaticResourceManager(PackType.CLIENT_RESOURCES);
    public static final GlobalResourceManager STATIC_DATA = createStaticResourceManager(PackType.SERVER_DATA);

    public static void addListener(String modId, OutdatedResourcesListener listener) {
        OUTDATED_RESOURCES_LISTENERS.computeIfAbsent(modId, s -> new ArrayList<>()).add(listener);
    }

    public static void forMod(Function<String, Path> inJarPathGetter, String modId) {
        Path defaultResourcesMeta = inJarPathGetter.apply(META_FILE_PATH);
        ModMetaFile meta;
        if (Files.exists(defaultResourcesMeta)) {
            try (InputStream is = Files.newInputStream(defaultResourcesMeta)) {
                JsonObject obj = GSON.fromJson(new BufferedReader(new InputStreamReader(is)), JsonObject.class);
                meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {});
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
                var defaultExtraction = meta.extract() ? Config.ExtractionState.EXTRACT : Config.ExtractionState.UNEXTRACTED;
                Config.ExtractionState extractionState = Config.INSTANCE.get().extract().getOrDefault(modId, defaultExtraction);
                if (!Config.INSTANCE.get().extract().containsKey(modId)) {
                    Config.INSTANCE.get().extract().put(modId, defaultExtraction);
                }
                if (extractionState == Config.ExtractionState.UNEXTRACTED) {
                    QUEUED_RESOURCES.put("__unextracted_" + modId, (s, type) -> {
                        if (!Files.exists(defaultResources.resolve(type.getDirectory()))) return null;
                        return () -> new AutoMetadataPathPackResources(s, "", defaultResources, type);
                    });
                    QUEUED_STATIC_RESOURCES.put("__unextracted_" + modId, (s, type) -> {
                        if (!Files.exists(defaultResources.resolve(GLOBAL_PREFIX + type.getDirectory()))) return null;
                        return () -> new AutoMetadataPathPackResources(s, GLOBAL_PREFIX, defaultResources, type);
                    });
                } else if (extractionState == Config.ExtractionState.EXTRACT) {
                    Config.INSTANCE.get().extract().put(modId, meta.extract() ? Config.ExtractionState.EXTRACT : Config.ExtractionState.EXTRACTED);
                    if (!meta.zip()) {
                        Path outPath = Services.PLATFORM.getGlobalFolder().resolve(modId);
                        String checksum = shouldCopy(defaultResources, outPath, Files.exists(outPath));
                        if (checksum != null) {
                            copyResources(defaultResources, outPath, checksum);
                        } else {
                            DefaultResources.LOGGER.error("Could not extract default resources for mod {} because they are already extracted and have been changed on disk", modId);
                            addOutdatedTarget(modId);
                            Config.INSTANCE.get().extract().put(modId, Config.ExtractionState.OUTDATED);
                        }
                    } else {
                        Path zipPath = Services.PLATFORM.getGlobalFolder().resolve(modId + ".zip");
                        boolean zipExists = Files.exists(zipPath);
                        String checksum;
                        try (FileSystem zipFs = FileSystems.newFileSystem(
                            URI.create("jar:" + zipPath.toAbsolutePath().toUri()),
                            Collections.singletonMap("create", "true"))) {
                            Path outPath = zipFs.getPath("/");
                            checksum = shouldCopy(defaultResources, outPath, zipExists);
                            if (checksum != null && !zipExists) {
                                copyResources(defaultResources, outPath, checksum);
                            } else if (checksum == null) {
                                DefaultResources.LOGGER.error("Could not extract default resources for mod {} because they are already extracted and have been changed on disk", modId);
                                addOutdatedTarget(modId);
                                Config.INSTANCE.get().extract().put(modId, Config.ExtractionState.OUTDATED);
                            }
                        }
                        if (checksum != null && zipExists) {
                            Files.delete(zipPath);
                            try (FileSystem zipFs = FileSystems.newFileSystem(
                                URI.create("jar:" + zipPath.toAbsolutePath().toUri()),
                                Collections.singletonMap("create", "true"))) {
                                Path outPath = zipFs.getPath("/");
                                copyResources(defaultResources, outPath, checksum);
                            }
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            DefaultResources.LOGGER.error("Could not handle default resources for mod {}", modId, e);
        }
    }
    private static synchronized void addOutdatedTarget(String modId) {
        OUTDATED_TARGETS.add(modId);
    }

    public static synchronized boolean isTargetOutdated(String modId) {
        return OUTDATED_TARGETS.contains(modId);
    }

    private static @Nullable String shouldCopy(Path defaultResources, Path outPath, boolean alreadyExists) {
        try {
            if (alreadyExists) {
                Path checksumPath = outPath.resolve(CHECK_FILE_PATH);
                String oldChecksum;
                if (Files.exists(checksumPath)) {
                    oldChecksum = Files.readString(checksumPath);
                } else {
                    return null;
                }
                String newExtractedChecksum = checkPath(outPath);
                if (newExtractedChecksum.equals(oldChecksum)) {
                    String newChecksum = checkPath(defaultResources);
                    if (newChecksum.equals(oldChecksum)) {
                        return null;
                    } else {
                        return newChecksum;
                    }
                }
            } else {
                return checkPath(defaultResources);
            }
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Error checking compatibility of resources from {} targeted at {}", defaultResources, outPath, e);
        }
        return null;
    }

    @NotNull
    private static String checkPath(Path defaultResources) throws IOException {
        StringBuilder newChecksum = new StringBuilder();
        try (var walk = Files.walk(defaultResources)) {
            walk.sorted(Comparator.comparing(p -> defaultResources.relativize(p).toString())).forEach(p -> {
                try {
                    if (!Files.isDirectory(p) && !p.endsWith(CHECK_FILE_PATH)) {
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

    private static void copyResources(Path defaultResources, Path outPath, String checksum) {
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
            Files.writeString(checksumPath, checksum);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Error checking compatibility of resources from {} targeted at {}", defaultResources, outPath, e);
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

    public synchronized static void initialize() {
        if (!GLOBAL_SETUP) {
            GLOBAL_SETUP = true;
            Services.PLATFORM.extractResources();
            DefaultResources.cleanupExtraction();
        }
        for (String modId : OUTDATED_TARGETS) {
            OUTDATED_RESOURCES_LISTENERS.getOrDefault(modId, List.of()).forEach(OutdatedResourcesListener::resourcesOutdated);
        }
    }

    public synchronized static GlobalResourceManager createStaticResourceManager(PackType type) {
        initialize();
        List<Pair<String, Pack.ResourcesSupplier>> sources = new ArrayList<>(getStaticPackResources(type));
        sources.addAll(Services.PLATFORM.getJarProviders(type));
        return new CombinedResourceManager(type, sources);
    }
}
