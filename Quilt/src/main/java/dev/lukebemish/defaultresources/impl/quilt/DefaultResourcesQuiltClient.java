package dev.lukebemish.defaultresources.impl.quilt;

import net.minecraft.server.packs.PackType;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class DefaultResourcesQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        DefaultResourcesQuilt.addPackResources(PackType.CLIENT_RESOURCES);
    }
}
