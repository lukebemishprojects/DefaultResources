package io.github.lukebemish.defaultresources.forge;

import com.mojang.datafixers.util.Pair;
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
import java.util.function.Supplier;

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
                List<Pair<String,Supplier<PackResources>>> packs = getPackResources(event);
                if (event.getPackType()== PackType.CLIENT_RESOURCES) {
                    Pack pack = Pack.create(DefaultResources.MOD_ID, true, () -> new DelegatingPackResources(DefaultResources.MOD_ID + "_global", "Global Resources",
                            new PackMetadataSection(Component.literal("Global Resources"), event.getPackType().getVersion(SharedConstants.getCurrentVersion())), packs.stream().map(p->p.getSecond().get()).toList()), constructor, Pack.Position.TOP, PackSource.DEFAULT);
                    packConsumer.accept(pack);
                } else {
                    packs.forEach(packResources -> {
                        Pack pack = Pack.create(DefaultResources.MOD_ID+":"+packResources.getFirst(), true, packResources.getSecond(), constructor, Pack.Position.TOP, PackSource.DEFAULT);
                        packConsumer.accept(pack);
                    });
                }
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Couldn't inject resources!");
            }
        });
    }

    @NotNull
    private List<Pair<String,Supplier<PackResources>>> getPackResources(AddPackFindersEvent event) {
        List<Pair<String,Supplier<PackResources>>> packs = new ArrayList<>();
        try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
            for (var file : files.toList()) {
                if (Files.isDirectory(file)) {
                    Supplier<PackResources> packResources = () -> new AutoMetadataFolderPackResources(event.getPackType(), file.toFile());
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                } else if (file.getFileName().toString().endsWith(".zip")) {
                    Supplier<PackResources> packResources = () -> new AutoMetadataFilePackResources(event.getPackType(), file.toFile());
                    packs.add(new Pair<>(file.getFileName().toString(),packResources));
                }
            }
        } catch (IOException ignored) {

        }
        return packs;
    }
}
