package io.github.lukebemish.defaultresources.quilt;

import io.github.lukebemish.defaultresources.AutoMetadataFilePackResources;
import io.github.lukebemish.defaultresources.AutoMetadataFolderPackResources;
import io.github.lukebemish.defaultresources.DefaultResources;
import io.github.lukebemish.defaultresources.Services;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
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
        ResourceProvider.instance();
        addPackResources(PackType.SERVER_DATA);
    }

    public static void addPackResources(PackType type) {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder())) Files.createDirectories(Services.PLATFORM.getGlobalFolder());
            ResourceLoader.get(type).registerResourcePackProfileProvider((infoConsumer, infoFactory) -> {
                Pack pack = Pack.create(DefaultResources.MOD_ID, true, () -> {
                    EmptyResourcePack core = new EmptyResourcePack(DefaultResources.MOD_ID + "_global", "Global Resources",
                            new PackMetadataSection(Component.literal("Global Resources"), type.getVersion(SharedConstants.getCurrentVersion())));
                    List<PackResources> packs = new ArrayList<>();
                    try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
                        for (var file : files.toList()) {
                            if (Files.isDirectory(file)) {
                                AutoMetadataFolderPackResources packResources = new AutoMetadataFolderPackResources(type, file.toFile());
                                packs.add(packResources);
                            } else if (file.getFileName().endsWith(".zip")) {
                                AutoMetadataFilePackResources packResources = new AutoMetadataFilePackResources(type, file.toFile());
                                packs.add(packResources);
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    return new GroupResourcePack.Wrapped(type, core, packs, false);
                }, infoFactory, Pack.Position.TOP, PackSource.DEFAULT);
                infoConsumer.accept(pack);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
