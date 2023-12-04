/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.AutoMetadataPathPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.services.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
    @Override
    public Path getGlobalFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
        DefaultResourcesFabriQuilt.forAllMods((modID, path) -> {
            if (!modID.equals("minecraft")) {
                DefaultResources.forMod(path::resolve, modID);
            }
        });
    }

    @Override
    public Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> providers = new ArrayList<>();
        DefaultResourcesFabriQuilt.forAllMods((modID, path) -> {
            if (!modID.equals("minecraft")) {
                providers.add(new Pair<>(modID, new Pack.ResourcesSupplier() {
                    @Override
                    public @NonNull PackResources openPrimary(@NonNull String s) {
                        return new AutoMetadataPathPackResources(s, "global", path, type);
                    }

                    @Override
                    public @NonNull PackResources openFull(@NonNull String s, Pack.@NonNull Info info) {
                        return new AutoMetadataPathPackResources(s, "global", path, type);
                    }
                }));
            }
        });
        return providers;
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getResourcePackDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }

    @Override
    public Map<String, Path> getExistingModdedPaths(String relative) {
        Map<String, Path> out = new HashMap<>();
        DefaultResourcesFabriQuilt.forAllMods((modID, path) -> {
            if (!modID.equals("minecraft")) {
                Path p = path.resolve(relative);
                if (Files.exists(p)) {
                    out.put(modID, p);
                }
            }
        });
        return out;
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}
