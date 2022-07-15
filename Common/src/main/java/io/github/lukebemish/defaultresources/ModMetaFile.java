package io.github.lukebemish.defaultresources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ModMetaFile(String configPath, String resourcesPath, boolean zip) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("config_path").forGetter(ModMetaFile::configPath),
            Codec.STRING.optionalFieldOf("resources_path",DefaultResources.MOD_ID).forGetter(ModMetaFile::configPath),
            Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip)
    ).apply(instance, ModMetaFile::new));
}
