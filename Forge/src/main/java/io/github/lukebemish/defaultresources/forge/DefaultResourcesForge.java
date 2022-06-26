package io.github.lukebemish.defaultresources.forge;

import io.github.lukebemish.defaultresources.AutoMetadataFolderPackResources;
import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.DelegatingResourcePack;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mod(DefaultResources.MOD_ID)
public class DefaultResourcesForge {

    public DefaultResourcesForge() {
        ResourceProvider.instance();
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        modbus.register(this);
    }

    @SubscribeEvent
    public void addPacks(AddPackFindersEvent event) {
        DefaultResources.LOGGER.info("Attempting pack insertion...");
        event.addRepositorySource((packConsumer, constructor) -> {
            try {
                if (!Files.exists(Services.PLATFORM.getGlobalFolder())) Files.createDirectories(Services.PLATFORM.getGlobalFolder());
                Pack pack = Pack.create(DefaultResources.MOD_ID, true, () -> {
                    List<PackResources> packs = new ArrayList<>();
                    try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
                        for (var file : files.toList()) {
                            if (!Files.isDirectory(file)) continue;
                            AutoMetadataFolderPackResources packResources = new AutoMetadataFolderPackResources(event.getPackType(), file.toFile());
                            packs.add(packResources);
                        }
                    } catch (IOException ignored) {

                    }
                    return new DelegatingResourcePack(DefaultResources.MOD_ID+"_global", "Global Resources",
                            new PackMetadataSection(Component.literal("Global Resources"), event.getPackType().getVersion(SharedConstants.getCurrentVersion())), packs);
                }, constructor, Pack.Position.TOP, PackSource.DEFAULT);
                packConsumer.accept(pack);
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Couldn't inject resources!");
            }
        });
    }
}
