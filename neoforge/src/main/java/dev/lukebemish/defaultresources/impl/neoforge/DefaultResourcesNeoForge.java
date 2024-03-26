package dev.lukebemish.defaultresources.impl.neoforge;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.resource.EmptyPackResources;

@Mod(DefaultResources.MOD_ID)
public class DefaultResourcesNeoForge {

	public DefaultResourcesNeoForge(IEventBus modbus) {
		DefaultResources.initialize();
		modbus.register(this);
	}

	@SubscribeEvent
	public void addPacks(AddPackFindersEvent event) {
		event.addRepositorySource((packConsumer) -> {
			try {
				if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
					Files.createDirectories(Services.PLATFORM.getGlobalFolder());
				List<Pair<String, Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(event.getPackType());
				var desc = Component.literal("Global Resources");
				Pack root = Pack.create(
					DefaultResources.MOD_ID,
					desc,
					true,
					new EmptyPackResources.EmptyResourcesSupplier(
						new PackMetadataSection(
							desc,
							SharedConstants.getCurrentVersion().getPackVersion(event.getPackType())
						),
						false
					),
					new Pack.Info(
						desc,
						PackCompatibility.COMPATIBLE,
						FeatureFlagSet.of(),
						List.of(),
						true
					),
					Pack.Position.TOP,
					true,
					PackSource.DEFAULT
				).withChildren(createPackStream(packs).toList());
				packConsumer.accept(root);
			} catch (IOException e) {
				DefaultResources.LOGGER.error("Couldn't inject resources!");
			}
		});
	}

	private static Stream<Pack> createPackStream(List<Pair<String, Pack.ResourcesSupplier>> packs) {
		return packs.stream().map(pair -> {
			var desc = Component.literal("Global Resources - "+pair.getFirst());
			Pack.Info info = new Pack.Info(
				desc,
				PackCompatibility.COMPATIBLE,
				FeatureFlagSet.of(),
				List.of(),
				true
			);
			return Pack.create(
				DefaultResources.MOD_ID + ":" + pair.getFirst(),
				desc,
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
