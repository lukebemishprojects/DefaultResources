/*
 * Copyright (C) 2022 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.quilt;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Set;


public class EmptyResourcePack extends AbstractPackResources {

    private final PackMetadataSection metadata;

    public EmptyResourcePack(String id, PackMetadataSection metadata) {
        super(id, false);
        this.metadata = metadata;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... fileName) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(@NotNull PackType packType, @NotNull String string, @NotNull String string2, @NotNull ResourceOutput resourceOutput) {

    }
    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return Set.of();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer.getMetadataSectionName().equals("pack"))
        {
            return (T) metadata;
        }
        return null;
    }

    @Override
    public void close() {

    }
}
