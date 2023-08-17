/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.forge;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.AutoMetadataPathPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.services.Platform;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {

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
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(mod -> !(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
            .forEach(mod ->
                DefaultResources.forMod(FMLPaths.CONFIGDIR.get(), mod.getOwningFile().getFile()::findResource, mod.getModId()));
    }

    @Override
    public Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> providers = new ArrayList<>();
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(mod -> !(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
            .forEach(mod -> {
                Path packPath = mod.getOwningFile().getFile().getSecureJar().getPath(String.join("/static/"));
                providers.add(new Pair<>(mod.getModId(), s -> new AutoMetadataPathPackResources(s, type, packPath)));
            });
        return providers;
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Map<String, Path> getExistingModdedPaths(String relative) {
        return FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(mod -> !(mod.getModId().equals("forge") || mod.getModId().equals("minecraft")))
            .map(mod ->
                new Pair<>(mod.getModId(), mod.getOwningFile().getFile().findResource(relative)))
            .filter(it -> it.getSecond() != null && Files.exists(it.getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> a));
    }
}
