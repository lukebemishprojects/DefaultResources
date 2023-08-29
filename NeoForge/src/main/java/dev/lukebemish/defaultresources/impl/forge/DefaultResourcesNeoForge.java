/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.forge;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.DelegatingPackResources;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Mod(DefaultResources.MOD_ID)
public class DefaultResourcesNeoForge {

    public DefaultResourcesNeoForge() {
        DefaultResources.initialize();
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        modbus.register(this);
    }

    @SubscribeEvent
    public void addPacks(AddPackFindersEvent event) {
        event.addRepositorySource((packConsumer) -> {
            try {
                if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                    Files.createDirectories(Services.PLATFORM.getGlobalFolder());
                List<Pair<String, Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(event.getPackType());
                if (event.getPackType() == PackType.CLIENT_RESOURCES) {
                    Pack.Info info = new Pack.Info(Component.literal("Global Resources"),
                        SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA),
                        SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
                        FeatureFlagSet.of(),
                        true);
                    Pack pack = Pack.create(DefaultResources.MOD_ID, Component.literal("Global Resources"), true, s -> new DelegatingPackResources(
                            DefaultResources.MOD_ID + "_global",
                            false,
                            new PackMetadataSection(
                                Component.literal("Global Resources"),
                                SharedConstants.getCurrentVersion().getPackVersion(event.getPackType())),
                            packs.stream().map(p -> p.getSecond().open(s)).toList()),
                        info,
                        PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.DEFAULT);
                    packConsumer.accept(pack);
                } else {
                    packs.forEach(packResources -> {
                        Pack.Info info = new Pack.Info(Component.literal("Global Resources"),
                            SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA),
                            SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
                            FeatureFlagSet.of(),
                            true);

                        Pack pack = Pack.create(DefaultResources.MOD_ID + ":" + packResources.getFirst(),
                            Component.literal("Global Resources"),
                            true,
                            packResources.getSecond(),
                            info,
                            PackType.SERVER_DATA,
                            Pack.Position.TOP,
                            true,
                            PackSource.DEFAULT);
                        packConsumer.accept(pack);
                    });
                }
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Couldn't inject resources!");
            }
        });
    }
}
