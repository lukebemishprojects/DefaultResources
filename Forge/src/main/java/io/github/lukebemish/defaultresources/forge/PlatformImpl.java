package io.github.lukebemish.defaultresources.forge;

import com.google.auto.service.AutoService;
import io.github.lukebemish.defaultresources.Cache;
import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import io.github.lukebemish.defaultresources.services.IPlatform;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getGlobalFolder() {
        return FMLPaths.GAMEDIR.get().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException ignored) {
        }
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f->f.getMods().stream())
                .filter(mod->!(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
                .forEach(mod-> {
                    Path defaultResources = mod.getOwningFile().getFile().getFilePath().resolve(DefaultResources.MOD_ID);
                    if (Files.exists(defaultResources) && !Cache.CACHE.modids.contains(mod.getModId())) {
                        Path outPath = Services.PLATFORM.getGlobalFolder().resolve(mod.getModId());
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
                                    } catch (IOException ignored) {
                                    }
                                });
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    Cache.CACHE.modids.add(mod.getModId());
                });
        Cache.CACHE.save();
    }

    @Override
    public Collection<ResourceProvider> getJarProviders() {
        List<ResourceProvider> providers = new ArrayList<>();
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f->f.getMods().stream())
                .filter(mod->!(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
                .forEach(mod->
                        providers.add(new PathResourceProvider(mod.getOwningFile().getFile().getFilePath())));
        return providers;
    }
}
