/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.WrappingResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A resource manager that can be used to load resources from global (static) resources.
 */
public interface GlobalResourceManager extends ResourceManager {
    /**
     * Creates a new global resource manager.
     * @param type the type of pack to load from
     * @param prefix the prefix to use for the resource manager
     * @return a new resource manager, which loads resources in {@code "global[data/assets]/[prefix]}.
     */
    @NotNull
    @Contract("_, _ -> new")
    static GlobalResourceManager create(PackType type, String prefix) {
        if (type == PackType.CLIENT_RESOURCES && !Services.PLATFORM.isClient()) {
            throw new IllegalStateException("Cannot create client resource manager on dedicated server");
        }
        return new WrappingResourceManager(switch (type) {
            case CLIENT_RESOURCES -> DefaultResources.STAtIC_ASSETS;
            case SERVER_DATA -> DefaultResources.STATIC_DATA;
        }, prefix);
    }
}
