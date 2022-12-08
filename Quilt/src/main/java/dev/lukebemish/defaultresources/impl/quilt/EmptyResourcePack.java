package dev.lukebemish.defaultresources.impl.quilt;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.ResourcePackFileNotFoundException;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class EmptyResourcePack extends AbstractPackResources {

    private final PackMetadataSection metadata;
    private final String name;

    public EmptyResourcePack(String id, String name, PackMetadataSection metadata) {
        super(new File(id));
        this.metadata = metadata;
        this.name = name;
    }

    @Nullable
    @Override
    public InputStream getRootResource(String fileName) throws IOException {
        throw new ResourcePackFileNotFoundException(this.file, fileName);
    }

    @Override
    protected boolean hasResource(String resourcePath) {
        return false;
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation location) throws IOException {
        throw new ResourcePackFileNotFoundException(this.file, getFullPath(type, location));
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType packType, String string, String string2, Predicate<ResourceLocation> predicate) {
        return List.of();
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation location) {
        return false;
    }

    @Override
    protected InputStream getResource(String resourcePath) throws IOException {
        throw new ResourcePackFileNotFoundException(this.file, resourcePath);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of();
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer.getMetadataSectionName().equals("pack"))
        {
            return (T) metadata;
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {

    }

    private static String getFullPath(PackType type, ResourceLocation location)
    {
        // stolen from ResourcePack
        return String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), location.getNamespace(), location.getPath());
    }
}
