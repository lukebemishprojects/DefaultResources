/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public interface GlobalResourceManager extends ResourceManager {
    GlobalResourceManager STAtIC_ASSETS = DefaultResources.createStaticResourceManager(PackType.CLIENT_RESOURCES);
    GlobalResourceManager STATIC_DATA = DefaultResources.createStaticResourceManager(PackType.SERVER_DATA);
}
