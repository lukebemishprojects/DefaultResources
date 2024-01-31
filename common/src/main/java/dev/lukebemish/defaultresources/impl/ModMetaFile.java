/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ModMetaFile(String resourcesPath, boolean zip, boolean extract) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("resources_path", "defaultresources").forGetter(ModMetaFile::resourcesPath),
        Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip),
        Codec.BOOL.optionalFieldOf("extract", true).forGetter(ModMetaFile::extract)
    ).apply(instance, ModMetaFile::new));
}
