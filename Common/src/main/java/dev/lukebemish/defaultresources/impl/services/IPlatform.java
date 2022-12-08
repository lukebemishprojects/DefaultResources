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
