package io.github.lukebemish.defaultresources.services;

import io.github.lukebemish.defaultresources.api.ResourceProvider;

import java.nio.file.Path;
import java.util.Collection;

public interface IPlatform {
    Path getGlobalFolder();

    void extractResources();

    Collection<ResourceProvider> getJarProviders();

    Path getCacheFolder();
}
