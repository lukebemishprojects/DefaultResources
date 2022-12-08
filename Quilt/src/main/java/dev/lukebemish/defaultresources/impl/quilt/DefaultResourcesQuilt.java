package dev.lukebemish.defaultresources.impl.quilt;

import dev.lukebemish.defaultresources.api.ResourceProvider;
import dev.lukebemish.defaultresources.impl.AutoMetadataFilePackResources;
import dev.lukebemish.defaultresources.impl.AutoMetadataFolderPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.GroupResourcePack;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DefaultResourcesQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        ResourceProvider.forceInitialization();
        addPackResources(PackType.SERVER_DATA);
    }

    public static void addPackResources(PackType type) {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder())) Files.createDirectories(Services.PLATFORM.getGlobalFolder());
            ResourceLoader.get(type).registerResourcePackProfileProvider((infoConsumer) -> {
                Pack pack = Pack.readMetaAndCreate(DefaultResources.MOD_ID, Component.literal("Global Resources"), true, s -> {
                    EmptyResourcePack core = new EmptyResourcePack(DefaultResources.MOD_ID + "_global",
                            new PackMetadataSection(Component.literal("Global Resources"), type.getVersion(SharedConstants.getCurrentVersion())));
                    List<PackResources> packs = new ArrayList<>();
                    try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
                        for (var file : files.toList()) {
                            if (Files.isDirectory(file)) {
                                AutoMetadataFolderPackResources packResources = new AutoMetadataFolderPackResources(s, type, file);
                                packs.add(packResources);
                            } else if (file.getFileName().toString().endsWith(".zip")) {
                                AutoMetadataFilePackResources packResources = new AutoMetadataFilePackResources(s, type, file.toFile());
                                packs.add(packResources);
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    return new GroupResourcePack.Wrapped(type, core, packs, false);
                }, type, Pack.Position.TOP, PackSource.DEFAULT);
                infoConsumer.accept(pack);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
