package io.github.lukebemish.defaultresources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Cache {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().excludeFieldsWithoutExposeAnnotation().create();
    private static final Codec<Cache> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("modids").forGetter(c->new ArrayList<>(c.modids))
    ).apply(instance, Cache::new));

    public static final Cache CACHE = load();

    public final HashSet<String> modids;

    private Cache(List<String> modids) {
        this.modids = new HashSet<>(modids);
    }

    private static Cache load() {
        Path path = Services.PLATFORM.getGlobalFolder().resolve(DefaultResources.CACHE);
        try {
            JsonObject json = GSON.fromJson(new FileReader(path.toFile()), JsonObject.class);
            return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e-> {});
        } catch (IOException e) {
            return new Cache(List.of());
        }
    }

    public void save() {
        Path path = Services.PLATFORM.getCacheFolder().resolve(DefaultResources.CACHE);
        try {
            if (!Files.exists(Services.PLATFORM.getCacheFolder())) Files.createDirectories(Services.PLATFORM.getCacheFolder());
            JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE,this).getOrThrow(false,e->{});
            FileWriter w = new FileWriter(path.toFile());
            GSON.toJson(json, w);
            w.flush();
            w.close();
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Couldn't save a cache of modids of extracted resources. They will be rewritten on the next game restart!", e);
        }
    }
}
