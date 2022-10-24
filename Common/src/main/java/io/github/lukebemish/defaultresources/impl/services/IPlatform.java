package io.github.lukebemish.defaultresources.impl.services;

import io.github.lukebemish.defaultresources.api.ModMetaFile;
import io.github.lukebemish.defaultresources.api.ResourceProvider;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

public interface IPlatform {
    Path getGlobalFolder();

    void extractResources();
    Collection<ResourceProvider> getJarProviders();

    Path getConfigDir();

    Map<String, ModMetaFile> getMetaFiles();
}
