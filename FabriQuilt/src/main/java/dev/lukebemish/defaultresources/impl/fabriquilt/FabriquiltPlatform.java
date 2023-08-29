/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt;

import dev.lukebemish.defaultresources.impl.fabriquilt.fabric.FabricPlatform;
import dev.lukebemish.defaultresources.impl.fabriquilt.quilt.QuiltPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public interface FabriquiltPlatform {
    @SuppressWarnings("deprecation")
    static FabriquiltPlatform getInstance() {
        if (FabricLoader.getInstance().isModLoaded("quilt_loader")) {
            return QuiltPlatform.INSTANCE;
        } else {
            return FabricPlatform.INSTANCE;
        }
    }

    void forAllMods(BiConsumer<String, Path> consumer);
}
