/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.api.ResourceProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GroupedResourceProvider implements ResourceProvider {

    private final Collection<? extends ResourceProvider> deferrals;

    public GroupedResourceProvider(Collection<? extends ResourceProvider> deferrals) {
        this.deferrals = deferrals;
    }

    @Override
    public @NotNull Collection<ResourceLocation> getResources(String packType, String prefix, Predicate<ResourceLocation> predicate) {
        return deferrals.stream().flatMap(p -> p.getResources(packType, prefix, predicate).stream()).toList();
    }

    @Override
    public @NotNull Stream<? extends InputStream> getResourceStreams(String packType, ResourceLocation location) {
        return deferrals.stream().flatMap(p -> p.getResourceStreams(packType, location));
    }

    @Override
    public @NotNull Stream<? extends InputStream> getResourceStreams(String packType, Collection<ResourceLocation> locations) {
        return deferrals.stream().flatMap(p -> p.getResourceStreams(packType, locations));
    }
}
