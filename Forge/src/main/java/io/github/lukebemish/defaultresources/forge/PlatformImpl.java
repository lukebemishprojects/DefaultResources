package io.github.lukebemish.defaultresources.forge;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.ModMetaFile;
import io.github.lukebemish.defaultresources.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import io.github.lukebemish.defaultresources.services.IPlatform;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public Path getGlobalFolder() {
        return FMLPaths.GAMEDIR.get().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f->f.getMods().stream())
                .filter(mod->!(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
                .forEach(mod-> {
                    Path defaultResourcesMeta = mod.getOwningFile().getFile().findResource(DefaultResources.MOD_ID+".meta.json");
                    if (Files.exists(defaultResourcesMeta)) {
                        try (InputStream is = Files.newInputStream(defaultResourcesMeta)) {
                            JsonObject obj = GSON.fromJson(new BufferedReader(new InputStreamReader(is)),JsonObject.class);
                            ModMetaFile meta = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> {});
                            Path defaultResources = mod.getOwningFile().getFile().findResource(meta.resourcesPath());
                            if (Files.exists(defaultResources) && !Files.exists(FMLPaths.CONFIGDIR.get().resolve(meta.configPath()))) {
                                if (!meta.zip()) {
                                    Path outPath = Services.PLATFORM.getGlobalFolder().resolve(mod.getModId());
                                    copyResources(defaultResources, outPath);
                                } else {
                                    try (FileSystem zipFs = FileSystems.newFileSystem(
                                            URI.create("jar:" + Services.PLATFORM.getGlobalFolder().resolve(mod.getModId()+".zip").toAbsolutePath().toUri()),
                                            Collections.singletonMap("create", "true"))) {
                                         Path outPath = zipFs.getPath("/");
                                         copyResources(defaultResources, outPath);
                                    }
                                }
                            }
                        } catch (IOException | RuntimeException e) {
                            DefaultResources.LOGGER.error("Could not read meta file for mod {}",mod.getModId(),e);
                        }
                    }
                });
    }

    private void copyResources(Path defaultResources, Path outPath) {
        if (!Files.exists(outPath)) {
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
    }

    @Override
    public Collection<ResourceProvider> getJarProviders() {
        List<ResourceProvider> providers = new ArrayList<>();
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f->f.getMods().stream())
                .filter(mod->!(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
                .forEach(mod->
                        providers.add(new PathResourceProvider(mod.getOwningFile().getFile().getSecureJar().getPath(String.join("/")))));
        return providers;
    }
}
