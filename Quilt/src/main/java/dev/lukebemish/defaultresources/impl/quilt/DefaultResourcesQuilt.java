package dev.lukebemish.defaultresources.impl.quilt;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.api.ResourceProvider;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.resource.loader.api.GroupResourcePack;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
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
                List<Pair<String,Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(type);
                Pack.Info info = new Pack.Info(Component.literal("Global Resources"), type.getVersion(SharedConstants.getCurrentVersion()), FeatureFlagSet.of());
                if (type == PackType.CLIENT_RESOURCES) {
                    Pack pack = Pack.create(DefaultResources.MOD_ID, Component.literal("Global Resources"), true, s -> {
                        EmptyResourcePack core = new EmptyResourcePack(DefaultResources.MOD_ID + "_global",
                                new PackMetadataSection(Component.literal("Global Resources"), type.getVersion(SharedConstants.getCurrentVersion())));
                        return new GroupResourcePack.Wrapped(type, core, packs.stream().map(it->it.getSecond().open(s)).toList(), false);
                    }, info, type, Pack.Position.TOP, true, PackSource.DEFAULT);
                    infoConsumer.accept(pack);
                } else {
                    for (var pair : packs) {
                        Pack pack = Pack.create(DefaultResources.MOD_ID+":"+pair.getFirst(),
                                Component.literal("Global Resources"),
                                true,
                                pair.getSecond(),
                                info,
                                PackType.SERVER_DATA,
                                Pack.Position.TOP,
                                true,
                                PackSource.DEFAULT);
                        infoConsumer.accept(pack);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
