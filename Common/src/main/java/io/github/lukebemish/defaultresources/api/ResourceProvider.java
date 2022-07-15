package io.github.lukebemish.defaultresources.api;

import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.Services;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ResourceProvider {
    /**
     * This should be run before your config file is written too, to ensure that DefaultResources takes it into account
     * when figuring out whether to extract default resources.
     */
    static void forceInitialization() {
        ResourceProvider.instance();
    }

    static ResourceProvider instance() {
        if (DefaultResources.RESOURCE_PROVIDER == null) {
            Services.PLATFORM.extractResources();
            DefaultResources.RESOURCE_PROVIDER = DefaultResources.assembleResourceProvider();
        }
        return DefaultResources.RESOURCE_PROVIDER;
    }

    @NotNull
    Collection<ResourceLocation> getResources(String packType, String prefix, Predicate<ResourceLocation> predicate);

    @NotNull
    Stream<? extends InputStream> getResourceStreams(String packType, ResourceLocation location);
}
