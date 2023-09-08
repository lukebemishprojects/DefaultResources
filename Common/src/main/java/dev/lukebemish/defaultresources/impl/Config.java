/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public record Config(HashMap<String, ExtractionState> extract) {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, StringRepresentable.fromEnum(ExtractionState::values)).xmap(HashMap::new, Function.identity()).fieldOf("extract").forGetter(Config::extract)
    ).apply(i, Config::new));

    public static final Supplier<Config> INSTANCE = Suppliers.memoize(Config::readFromConfig);

    private static Config getDefault() {
        return new Config(new HashMap<>());
    }

    private static Config readFromConfig() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve(DefaultResources.MOD_ID + ".json");
        Config config = getDefault();
        if (Files.exists(configPath)) {
            try {
                JsonElement json = DefaultResources.GSON.fromJson(Files.newBufferedReader(configPath), JsonElement.class);
                config = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e ->
                    DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default: {}", DefaultResources.MOD_ID, e));
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Could not read {}.json config; using (and replacing) with default.", DefaultResources.MOD_ID, e);
            } catch (JsonSyntaxException e) {
                DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default.", DefaultResources.MOD_ID, e);
            } catch (RuntimeException ignored) {
                // Already caught and logged.
            }
        }
        var map = new HashMap<>(config.extract());
        Services.PLATFORM.getExistingModdedPaths(DefaultResources.META_FILE_PATH).forEach((modId, metaPath) -> {
            if (!map.containsKey(modId)) {
                map.put(modId, ExtractionState.UNEXTRACTED);
            }
        });
        config = new Config(map);

        try {
            writeConfig(configPath, config);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not write {}.json config. There may be odd behavior. ", DefaultResources.MOD_ID, e);
        }

        return config;
    }

    private static void writeConfig(Path path, Config config) throws IOException {
        if (!Files.exists(path.getParent()))
            Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("// Set extraction to \"extract\" to extract currently unextracted resources.\n");
            DefaultResources.GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, config)
                    .getOrThrow(false, e -> {
                    }),
                writer);
        }
    }

    public void save() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve(DefaultResources.MOD_ID + ".json");
        try {
            writeConfig(configPath, this);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not save {} config!", DefaultResources.MOD_ID, e);
        }
    }

    enum ExtractionState implements StringRepresentable {
        UNEXTRACTED,
        EXTRACT,
        EXTRACTED;

        @Override
        public @NonNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
