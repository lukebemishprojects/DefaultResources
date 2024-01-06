/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ModMetaFile(String resourcesPath, boolean zip) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("resources_path").forGetter(ModMetaFile::resourcesPath),
        Codec.BOOL.fieldOf("zip").forGetter(ModMetaFile::zip)
    ).apply(instance, ModMetaFile::new));
}
