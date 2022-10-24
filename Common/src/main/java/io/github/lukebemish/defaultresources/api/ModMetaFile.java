package io.github.lukebemish.defaultresources.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lukebemish.defaultresources.impl.DefaultResources;
import org.jetbrains.annotations.ApiStatus;

/**
 * Your mod's `defaultresources.meta.json` will get read according to this format.
 * @param configPath The name of the config file (in the `config` folder) to be used to determine whether to unpack
 *                   default resources. Prior to saving this config, you should run
 *                   {@link ResourceProvider#forceInitialization()}.
 * @param resourcesPath The path to the resources to extract within your mod jar. Defaults to `defaultresources`.
 * @param zip Whether the default resources should be extracted into a zip file. Defaults to true.
 * @param createsMarker Whether defaultresources should create the marker file if it's missing. Defaults to false.
 * @param extractsByDefault Whether the resources are extracted from the mod by default, or whether extraction must be enabled.
 */
public record ModMetaFile(String configPath, String resourcesPath, boolean zip, boolean createsMarker, boolean extractsByDefault) {
    public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("config_path").forGetter(ModMetaFile::configPath),
            Codec.STRING.optionalFieldOf("resources_path", DefaultResources.MOD_ID).forGetter(ModMetaFile::configPath),
            Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip),
            Codec.BOOL.optionalFieldOf("creates_marker", false).forGetter(ModMetaFile::createsMarker),
            Codec.BOOL.optionalFieldOf("extracts_by_default", false).forGetter(ModMetaFile::extractsByDefault)
    ).apply(instance, ModMetaFile::new));

    @ApiStatus.Internal
    public ModMetaFile {}
}
