package io.github.lukebemish.defaultresources.impl;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.animal.Cod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record Config(Map<String, Boolean> extract) {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("extract").forGetter(Config::extract)
    ).apply(i, Config::new));

    public static final Supplier<Config> INSTANCE = Suppliers.memoize(Config::readFromConfig);

    private static Config getDefault() {
        return new Config(Map.of());
    }

    private static Config readFromConfig() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve(DefaultResources.MOD_ID + ".json");
        Config config = getDefault();
        if (Files.exists(configPath)) {
            try {
                JsonElement json = DefaultResources.GSON.fromJson(Files.newBufferedReader(configPath), JsonElement.class);
                config = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e->{
                    DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default: {}",DefaultResources.MOD_ID,e);
                });
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Could not read {}.json config; using (and replacing) with default.",DefaultResources.MOD_ID,e);
            } catch (JsonSyntaxException e) {
                DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default.",DefaultResources.MOD_ID,e);
            } catch (RuntimeException ignored) {
                // Already caught and logged.
            }
        }
        var map = new HashMap<>(config.extract());
        Services.PLATFORM.getMetaFiles().forEach((modId, metaFile) -> {
            if (!map.containsKey(modId)) {
                map.put(modId, metaFile.extractsByDefault());
            }
        });
        config = new Config(Map.copyOf(map));

        try {
            writeConfig(configPath, config);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not write {}.json config. There may be odd behavior. ",DefaultResources.MOD_ID,e);
        }

        return config;
    }

    private static void writeConfig(Path path, Config config) throws IOException {
        if (!Files.exists(path.getParent()))
            Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        try (var writer = Files.newBufferedWriter(path)) {
            DefaultResources.GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, config)
                    .getOrThrow(false, e -> {}),
                    writer);
        }
    }
}
