/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.quilt;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.api.ResourceProvider;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class DefaultResourcesQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        ResourceProvider.instance();
        addPackResources(PackType.SERVER_DATA);
    }

    public static void addPackResources(PackType type) {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ResourceLoader.get(type).getRegisterDefaultResourcePackEvent().register(context -> {
            List<Pair<String, Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(type);
            for (var pair : packs) {
                context.addResourcePack(pair.getSecond().open(pair.getFirst()));
            }
        });
    }
}
