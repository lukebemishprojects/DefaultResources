package io.github.lukebemish.defaultresources;

import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.GroupedResourceProvider;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DefaultResources {
    public static final String MOD_ID = "defaultresources";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final String CACHE = MOD_ID + "_cache.json";

    public static ResourceProvider RESOURCE_PROVIDER;

    public static ResourceProvider assembleResourceProvider() {
        List<ResourceProvider> providers = new ArrayList<>();
        try (var paths = Files.list(Services.PLATFORM.getGlobalFolder())) {
            paths.forEach(path -> {
                if (Files.isDirectory(path)) {
                    providers.add(new PathResourceProvider(path));
                }
            });
        } catch (IOException ignored) {
        }
        providers.addAll(Services.PLATFORM.getJarProviders());
        return new GroupedResourceProvider(providers);
    }
}
