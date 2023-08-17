/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.services;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public interface Platform {
    Path getGlobalFolder();

    void extractResources();

    Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type);

    Path getConfigDir();

    Map<String, Path> getExistingModdedPaths(String relative);
}
