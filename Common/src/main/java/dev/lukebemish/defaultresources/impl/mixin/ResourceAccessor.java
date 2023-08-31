package dev.lukebemish.defaultresources.impl.mixin;

import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.InputStream;

@Mixin(Resource.class)
public interface ResourceAccessor {
    @Accessor
    IoSupplier<InputStream> getStreamSupplier();
    @Accessor
    IoSupplier<ResourceMetadata> getMetadataSupplier();
}
