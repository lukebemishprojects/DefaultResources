/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record ModMetaFile(String resourcesPath, boolean zip,
                          boolean extract, Optional<String> dataVersion) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("resources_path", DefaultResources.MOD_ID).forGetter(ModMetaFile::resourcesPath),
        Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip),
        Codec.BOOL.optionalFieldOf("extract", true).forGetter(ModMetaFile::extract),
        Codec.STRING.optionalFieldOf("data_version").forGetter(ModMetaFile::dataVersion)
    ).apply(instance, ModMetaFile::new));
}
