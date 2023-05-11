/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class AutoMetadataFilePackResources extends FilePackResources {

    private final PackType packType;

    public AutoMetadataFilePackResources(String s, PackType packType, File file) {
        super(s, file, false);
        this.packType = packType;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer.getMetadataSectionName().equals("pack")) {
            JsonObject object = new JsonObject();
            object.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(this.packType));
            object.addProperty("description", "Global resources");
            return serializer.fromJson(object);
        }
        return null;
    }
}
