/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.quilt;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.AutoMetadataPathPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.services.Platform;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.quiltmc.loader.api.QuiltLoader;

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
                DefaultResources.forMod(QuiltLoader.getConfigDir(), mod.rootPath().toAbsolutePath()::resolve, modid);
            }
        });
    }

    @Override
    public Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> providers = new ArrayList<>();
        QuiltLoader.getAllMods().forEach(mod -> {
            String modid = mod.metadata().id();
            if (!modid.equals("minecraft")) {
                Path packPath = mod.rootPath().resolve("static");
                providers.add(new Pair<>(modid, s -> new AutoMetadataPathPackResources(s, type, packPath)));
            }
        });
        return providers;
    }

    @Override
    public Path getConfigDir() {
        return QuiltLoader.getConfigDir();
    }

    @Override
    public Map<String, Path> getExistingModdedPaths(String relative) {
        return QuiltLoader.getAllMods().stream()
            .filter(mod -> !mod.metadata().id().equals("minecraft"))
            .map(mod ->
                new Pair<>(mod.metadata().id(), mod.rootPath().toAbsolutePath().resolve(relative)))
            .filter(it -> it.getSecond() != null && Files.exists(it.getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> a));
    }
}
