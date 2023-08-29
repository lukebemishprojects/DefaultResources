/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt.fabric;

import dev.lukebemish.defaultresources.impl.fabriquilt.FabriquiltPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public final class FabricPlatform implements FabriquiltPlatform {
    public static final FabricPlatform INSTANCE = new FabricPlatform();
    private FabricPlatform() {}

    @SuppressWarnings("deprecation")
    @Override
    public void forAllMods(BiConsumer<String, Path> consumer) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> consumer.accept(mod.getMetadata().getId(), mod.getRootPath()));
    }
}
