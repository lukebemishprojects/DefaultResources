/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The main API entrypoint for DefaultResources.
 */
public interface ResourceProvider {
    /**
     * This should be run before your config file is written too, to ensure that DefaultResources takes it into account
     * when figuring out whether to extract default resources.
     */
    static void forceInitialization() {
        ResourceProvider.instance();
    }

    /**
     * Use this to get an instance of a ResourceProvider to get resources from.
     *
     * @return A ResourceProvider grouping resources from mod jars and the `globalresources` directory.
     */
    @SuppressWarnings("UnusedReturnValue")
    static ResourceProvider instance() {
        if (DefaultResources.RESOURCE_PROVIDER == null) {
            Services.PLATFORM.extractResources();
            DefaultResources.cleanupExtraction();
            DefaultResources.RESOURCE_PROVIDER = DefaultResources.assembleResourceProvider();
        }
        return DefaultResources.RESOURCE_PROVIDER;
    }

    /**
     * Gets the highest priority resource matching a given location
     *
     * @param packType an identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                 datapacks
     * @param location the resource location, excluding pack type, of the resources to locate
     * @return An {@link IoSupplier} of the located resource. Should be closed once no longer in use
     */
    @NotNull
    default IoSupplier<InputStream> getResource(String packType, ResourceLocation location) {
        var optional = getResourceStreams(packType, location).findFirst();
        if (optional.isEmpty()) {
            return () -> {
                throw new IOException("No resource found at " + location);
            };
        }
        return optional::get;
    }

    /**
     * Gets a collection of ResourceLocations present matching a certain pattern.
     *
     * @param packType  An identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                  datapacks.
     * @param prefix    The initial part of the ResourceLocation path to search in.
     * @param predicate A predicate to filter results.
     * @return A collection of located ResourceLocations, excluding the pack type.
     */
    @NotNull
    Collection<ResourceLocation> getResources(String packType, String prefix, Predicate<ResourceLocation> predicate);

    /**
     * Gets a stream of InputStreams matching a single location.
     *
     * @param packType an identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                 datapacks
     * @param location the resource location, excluding pack type, of the resources to locate
     * @return a stream of the located resources. Should be closed once no longer in use
     */
    @NotNull
    Stream<? extends InputStream> getResourceStreams(String packType, ResourceLocation location);

    /**
     * Gets a stream of InputStreams matching any of a collections of locations.
     *
     * @param packType an identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                 datapacks
     * @param locations the resource locations, excluding pack type, of the resources to locate
     * @return a stream of the located resources. Should be closed once no longer in use
     */
    @NotNull
    Stream<? extends InputStream> getResourceStreams(String packType, Collection<ResourceLocation> locations);
}
