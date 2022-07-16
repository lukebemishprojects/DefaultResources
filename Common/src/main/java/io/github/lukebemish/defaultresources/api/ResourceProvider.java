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

    /**
     * Use this to get an instance of a ResourceProvider to get resources from.
     * @return A ResourceProvider grouping resources from mod jars and the `globalresources` directory.
     */
    static ResourceProvider instance() {
        if (DefaultResources.RESOURCE_PROVIDER == null) {
            Services.PLATFORM.extractResources();
            DefaultResources.RESOURCE_PROVIDER = DefaultResources.assembleResourceProvider();
        }
        return DefaultResources.RESOURCE_PROVIDER;
    }

    /**
     * Gets a collection of ResourceLocations present matching a certain pattern.
     * @param packType An identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                 datapacks.
     * @param prefix The initial part of the ResourceLocation path to search in.
     * @param predicate A predicate to filter results.
     * @return A collection of located ResourceLocations, excluding the pack type.
     */
    @NotNull
    Collection<ResourceLocation> getResources(String packType, String prefix, Predicate<ResourceLocation> predicate);

    /**
     * Gets a stream of InputStreams matching a single ResourceLocation.
     * @param packType An identifier for the pack type folder - for instance, vanilla uses the `data` pack type for
     *                 datapacks.
     * @param location The ResourceLocation, excluding pack type, of the resources to locate.
     * @return A stream of the located resources. Should be closed once no longer in use.
     */
    @NotNull
    Stream<? extends InputStream> getResourceStreams(String packType, ResourceLocation location);
}
