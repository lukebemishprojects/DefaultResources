package dev.lukebemish.defaultresources.impl.forge;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.api.ResourceProvider;
import dev.lukebemish.defaultresources.impl.AutoMetadataFilePackResources;
import dev.lukebemish.defaultresources.impl.AutoMetadataFolderPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.DelegatingPackResources;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mod(DefaultResources.MOD_ID)
public class DefaultResourcesForge {

    public DefaultResourcesForge() {
        ResourceProvider.forceInitialization();
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        modbus.register(this);
    }

    @SubscribeEvent
    public void addPacks(AddPackFindersEvent event) {
        DefaultResources.LOGGER.info("Attempting pack insertion...");
        event.addRepositorySource((packConsumer) -> {
            try {
                if (!Files.exists(Services.PLATFORM.getGlobalFolder())) Files.createDirectories(Services.PLATFORM.getGlobalFolder());
                List<Pair<String,Pack.ResourcesSupplier>> packs = getPackResources(event);
                if (event.getPackType()== PackType.CLIENT_RESOURCES) {
                    Pack pack = Pack.readMetaAndCreate(DefaultResources.MOD_ID, Component.literal("Global Resources"), true, s -> new DelegatingPackResources(
                            DefaultResources.MOD_ID + "_global",
                            false,
                            new PackMetadataSection(
                                    Component.literal("Global Resources"),
                                    event.getPackType().getVersion(SharedConstants.getCurrentVersion())),
                            packs.stream().map(p->p.getSecond().open(s)).toList()), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.DEFAULT);
                    packConsumer.accept(pack);
                } else {
                    packs.forEach(packResources -> {
                        Pack pack = Pack.readMetaAndCreate(DefaultResources.MOD_ID+":"+packResources.getFirst(), Component.literal("Global Resources"), true, packResources.getSecond(), PackType.SERVER_DATA ,Pack.Position.TOP, PackSource.DEFAULT);
                        packConsumer.accept(pack);
                    });
                }
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Couldn't inject resources!");
            }
        });
    }

    @NotNull
    private List<Pair<String,Pack.ResourcesSupplier>> getPackResources(AddPackFindersEvent event) {
        List<Pair<String,Pack.ResourcesSupplier>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFolderPackResources(s, event.getPackType(), file);
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Pack.ResourcesSupplier packResources = s -> new AutoMetadataFilePackResources(s, event.getPackType(), file.toFile());
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                }
            }
        } catch (IOException ignored) {

        }
        return packs;
    }
}
