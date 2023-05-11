/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.api.ResourceProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathResourceProvider implements ResourceProvider {

    protected final Path source;

    public PathResourceProvider(Path path) {
        this.source = path;
    }

    @Override
    public @NotNull Collection<ResourceLocation> getResources(String packType, @Nullable String prefix, Predicate<ResourceLocation> predicate) {
        try {
            Path root = resolve(packType);
            ArrayList<ResourceLocation> rls = new ArrayList<>();
            try (var files = Files.list(root)) {
                files.map(file -> {
                    Path newRoot = prefix == null ? file : file.resolve(prefix);
                    try (var walk = Files.walk(newRoot)) {
                        return walk
                            .filter(p -> !Files.isDirectory(p))
                            .map(root::relativize)
                            .map(path -> {
                                String namespace = path.getName(0).toString();
                                List<String> rlPath = new ArrayList<>();
                                for (int i = 1; i < path.getNameCount(); i++) {
                                    rlPath.add(path.getName(i).toString());
                                }
                                return new ResourceLocation(namespace, String.join("/", rlPath));
                            })
                            .filter(predicate)
                            .collect(Collectors.toList());
                    } catch (IOException e) {
                        return List.<ResourceLocation>of();
                    }
                }).forEach(rls::addAll);
                return rls;
            }
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public @NotNull Stream<? extends InputStream> getResourceStreams(String packType, ResourceLocation location) {
        try {
            Path path = resolve(packType, location.getNamespace(), location.getPath());
            return Stream.of(Files.newInputStream(path, StandardOpenOption.READ));
        } catch (IOException e) {
            return Stream.of();
        }
    }

    @Override
    public @NotNull Stream<? extends InputStream> getResourceStreams(String packType, Collection<ResourceLocation> locations) {
        return locations.stream().flatMap(rl -> getResourceStreams(packType, rl));
    }

    protected Path resolve(String... paths) throws IOException {
        Path path = this.source.toAbsolutePath();
        for (String name : paths)
            path = path.resolve(name);
        return path;
    }
}
