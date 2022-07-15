package io.github.lukebemish.defaultresources.quilt;

import com.google.auto.service.AutoService;
import io.github.lukebemish.defaultresources.Cache;
import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import io.github.lukebemish.defaultresources.services.IPlatform;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getGlobalFolder() {
        return QuiltLoader.getGameDir().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
        QuiltLoader.getAllMods().forEach(mod -> {
            String modid = mod.metadata().id();
            if (!modid.equals("minecraft")) {
                Path defaultResources = mod.rootPath().toAbsolutePath().resolve(DefaultResources.MOD_ID);
                if (Files.exists(defaultResources) && !Cache.CACHE.modids.contains(modid)) {
                    Path outPath = Services.PLATFORM.getGlobalFolder().resolve(modid);
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
                Cache.CACHE.modids.add(modid);
            }
        });
        Cache.CACHE.save();
    }

    @Override
    public Collection<ResourceProvider> getJarProviders() {
        List<ResourceProvider> providers = new ArrayList<>();
        QuiltLoader.getAllMods().forEach(mod -> {
            String modid = mod.metadata().id();
            if (!modid.equals("minecraft")) {
                providers.add(new PathResourceProvider(mod.rootPath()));
            }
        });
        return providers;
    }

    @Override
    public Path getCacheFolder() {
        return QuiltLoader.getGameDir().resolve("mod_data/defaultresources");
    }
}
