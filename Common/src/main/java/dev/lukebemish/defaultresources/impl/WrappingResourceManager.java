/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.api.GlobalResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class WrappingResourceManager implements GlobalResourceManager {

    private final GlobalResourceManager wrapped;
    private final String prefix;

    private static void checkValidPrefix(String prefix) {
        for (char c : prefix.toCharArray()) {
            if (c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                continue;
            }
            throw new IllegalArgumentException("Invalid prefix: \"" + prefix+"\". Prefixes must contain only lowercase letters, numbers, underscores, and dashes.");
        }
    }

    public WrappingResourceManager(GlobalResourceManager wrapped, String prefix) {
        checkValidPrefix(prefix);
        this.wrapped = wrapped;
        this.prefix = prefix+"/";
    }

    @Override
    public @NotNull Set<String> getNamespaces() {
        return wrapped.getNamespaces();
    }

    @Override
    public @NotNull List<Resource> getResourceStack(ResourceLocation location) {
        return wrapped.getResourceStack(location.withPrefix(prefix));
    }

    @Override
    public @NotNull Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
        return wrapped.listResources(prefix + path, filter);
    }

    @Override
    public @NotNull Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
        return wrapped.listResourceStacks(prefix + path, filter);
    }

    @Override
    public @NotNull Stream<PackResources> listPacks() {
        return wrapped.listPacks().map(pack -> new PackResources() {
            @Nullable
            @Override
            public IoSupplier<InputStream> getRootResource(String... elements) {
                return pack.getRootResource(elements);
            }

            @Nullable
            @Override
            public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
                return pack.getResource(packType, location.withPrefix(prefix));
            }

            @Override
            public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
                pack.listResources(packType, namespace, prefix+path, resourceOutput);
            }

            @Override
            public @NotNull Set<String> getNamespaces(PackType type) {
                return pack.getNamespaces(type);
            }

            @Nullable
            @Override
            public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
                return pack.getMetadataSection(deserializer);
            }

            @Override
            public @NotNull String packId() {
                return pack.packId();
            }

            @Override
            public boolean isBuiltin() {
                return pack.isBuiltin();
            }

            @Override
            public void close() {
                pack.close();
            }
        });
    }

    @Override
    public @NotNull Optional<Resource> getResource(ResourceLocation location) {
        return wrapped.getResource(location.withPrefix(prefix));
    }
}
