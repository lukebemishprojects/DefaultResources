/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt.quilt;

import dev.lukebemish.defaultresources.impl.fabriquilt.FabriquiltPlatform;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public final class QuiltPlatform implements FabriquiltPlatform {
    public static final QuiltPlatform INSTANCE = new QuiltPlatform();
    private QuiltPlatform() {}

    @Override
    public void forAllMods(BiConsumer<String, Path> consumer) {
        QuiltLoader.getAllMods().forEach(mod -> consumer.accept(mod.metadata().id(), mod.rootPath()));
    }
}
