/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AutoMetadataPathPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String name;
    protected final Path path;
    private final PackType packType;

    public AutoMetadataPathPackResources(String s, String prefix, Path path, PackType packType) {
        super(s, false);
        this.name = prefix+packType.getDirectory();
        this.path = path;
        this.packType = packType;
    }

    protected String getPackFolderName() {
        return name;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        Path path = this.path.resolve(getPackFolderName()).resolve(location.getNamespace());
        if (!Files.isDirectory(path)) {
            return null;
        }
        return PathPackResources.getResource(location, path);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        FileUtil.decomposePath(path).get().ifLeft((list) -> {
            Path namespacePath = this.path.resolve(getPackFolderName()).resolve(namespace);
            if (!Files.isDirectory(namespacePath)) {
                return;
            }
            PathPackResources.listPath(namespace, namespacePath, list, resourceOutput);
        }).ifRight((partialResult) -> LOGGER.error("Invalid path {}: {}", path, partialResult.message()));
    }

    @Override
    public @NonNull Set<String> getNamespaces(PackType type) {
        Set<String> set = new HashSet<>();
        Path path = this.path.resolve(getPackFolderName());

        if (!Files.isDirectory(path)) {
            return Set.of();
        }

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
            for (Path namespacePath : paths) {
                if (Files.isDirectory(namespacePath)) {
                    String namespace = namespacePath.getFileName().toString();
                    if (namespace.equals(namespace.toLowerCase(Locale.ROOT))) {
                        set.add(namespace);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list path {}", path, e);
        }
        return set;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer.getMetadataSectionName().equals("pack")) {
            JsonObject object = new JsonObject();
            object.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(this.packType));
            object.addProperty("description", "Global resources");
            return serializer.fromJson(object);
        }
        return null;
    }

    @Override
    public void close() {

    }
}
