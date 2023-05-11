/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.services;

import dev.lukebemish.defaultresources.api.ResourceProvider;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public interface IPlatform {
    Path getGlobalFolder();

    void extractResources();

    Collection<ResourceProvider> getJarProviders();

    Path getConfigDir();

    Map<String, Path> getExistingModdedPaths(String relative);
}
