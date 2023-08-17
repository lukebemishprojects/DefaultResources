package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public interface GlobalResourceManager extends ResourceManager {
    GlobalResourceManager STAtIC_ASSETS = DefaultResources.createStaticResourceManager(PackType.CLIENT_RESOURCES);
    GlobalResourceManager STATIC_DATA = DefaultResources.createStaticResourceManager(PackType.SERVER_DATA);

    /**
     * This should be run before your config file is written too, to ensure that DefaultResources takes it into account
     * when figuring out whether to extract default resources.
     */
    static void forceInitialization() {
        DefaultResources.initialize();
    }
}
