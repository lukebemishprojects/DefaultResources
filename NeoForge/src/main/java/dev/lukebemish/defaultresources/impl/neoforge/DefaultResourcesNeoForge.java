/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.neoforge;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

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
                createPackStream(event.getPackType(), packs).forEach(packConsumer);
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Couldn't inject resources!");
            }
        });
    }

    private static Stream<Pack> createPackStream(PackType packType, List<Pair<String, Pack.ResourcesSupplier>> packs) {
        return packs.stream().map(pair -> {
            Pack.Info info = new Pack.Info(
                Component.literal("Global Resources - "+pair.getFirst()),
                SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA),
                SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES),
                PackCompatibility.COMPATIBLE,
                FeatureFlagSet.of(),
                List.of(),
                true
            );
            return Pack.create(
                DefaultResources.MOD_ID + ":" + pair.getFirst(),
                Component.literal("Global Resources"),
                true,
                pair.getSecond(),
                info,
                Pack.Position.TOP,
                true,
                PackSource.DEFAULT
            );
        });
    }
}
