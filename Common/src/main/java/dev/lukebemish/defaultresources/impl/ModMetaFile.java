/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.defaultresources.api.ResourceProvider;

import java.util.Optional;

public record ModMetaFile(Optional<String> markerPath, String resourcesPath, boolean zip, boolean createsMarker,
                          boolean extractsByDefault) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("marker_path").forGetter(ModMetaFile::markerPath),
        Codec.STRING.optionalFieldOf("resources_path", DefaultResources.MOD_ID).forGetter(ModMetaFile::resourcesPath),
        Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip),
        Codec.BOOL.optionalFieldOf("creates_marker", false).forGetter(ModMetaFile::createsMarker),
        Codec.BOOL.optionalFieldOf("extracts_by_default", false).forGetter(ModMetaFile::extractsByDefault)
    ).apply(instance, ModMetaFile::new));
}
