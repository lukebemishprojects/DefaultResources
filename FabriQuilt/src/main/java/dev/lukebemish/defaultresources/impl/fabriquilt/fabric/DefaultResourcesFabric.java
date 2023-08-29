/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt.fabric;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DefaultResources.initialize();
        addPackResources(PackType.SERVER_DATA);
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
                out.add(pair.getSecond().open(pair.getFirst()));
            }
            return out;
        });
    }
}
