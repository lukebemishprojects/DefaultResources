/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabriQuilt implements ModInitializer {
    @Override
    public void onInitialize() {
        DefaultResources.initialize();
        addPackResources(PackType.SERVER_DATA);
    }

    public static void forAllMods(BiConsumer<String, Path> consumer) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> consumer.accept(mod.getMetadata().getId(), mod.getRootPath()));
    }

    public static void forAllModsParallel(BiConsumer<String, Path> consumer) {
        FabricLoader.getInstance().getAllMods().parallelStream().forEach(mod -> consumer.accept(mod.getMetadata().getId(), mod.getRootPath()));
    }

    public static void addPackResources(PackType type) {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ResourceLoader.get(type).addPacks(() -> {
            List<PackResources> out = new ArrayList<>();
            List<Pair<String, Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(type);
            for (var pair : packs) {
                Pack.Info info = new Pack.Info(
                    Component.literal("Global Resources - "+pair.getFirst()),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of()
                );
                out.add(pair.getSecond().openFull(pair.getFirst(), info));
            }
            return out;
        });
    }
}
