package io.github.lukebemish.defaultresources.impl.forge;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.lukebemish.defaultresources.impl.DefaultResources;
import io.github.lukebemish.defaultresources.impl.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import io.github.lukebemish.defaultresources.impl.services.IPlatform;
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
                    DefaultResources.forMod(FMLPaths.CONFIGDIR.get(), mod.getOwningFile().getFile()::findResource, mod.getModId());
                });
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
