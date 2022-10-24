package io.github.lukebemish.defaultresources.impl;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class AutoMetadataFolderPackResources extends FolderPackResources {

    private final PackType packType;

    public AutoMetadataFolderPackResources(PackType packType, File file) {
        super(file);
        this.packType = packType;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        if(serializer.getMetadataSectionName().equals("pack")) {
            JsonObject object = new JsonObject();
            object.addProperty("pack_format", this.packType.getVersion(SharedConstants.getCurrentVersion()));
            object.addProperty("description", "Global resources");
            return serializer.fromJson(object);
        }
        return null;
    }
}
