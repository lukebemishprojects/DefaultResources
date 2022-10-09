package io.github.lukebemish.defaultresources.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.defaultresources.DefaultResources;
import org.jetbrains.annotations.ApiStatus;

/**
 * Your mod's `defaultresources.meta.json` will get read according to this format.
 * @param configPath The name of the config file (in the `config` folder) to be used to determine whether to unpack
 *                   default resources. Prior to saving this config, you should run
 *                   {@link ResourceProvider#forceInitialization()}.
 * @param resourcesPath The path to the resources to extract within your mod jar. Defaults to `defaultresources`.
 * @param zip Whether the default resources should be extracted into a zip file. Defaults to true.
 */
public record ModMetaFile(String configPath, String resourcesPath, boolean zip) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("config_path").forGetter(ModMetaFile::configPath),
            Codec.STRING.optionalFieldOf("resources_path", DefaultResources.MOD_ID).forGetter(ModMetaFile::configPath),
            Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip)
    ).apply(instance, ModMetaFile::new));

    @ApiStatus.Internal
    public ModMetaFile {}
}
