package io.github.lukebemish.defaultresources.impl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class ZipResourceProvider extends PathResourceProvider {

    @Nullable
    private FileSystem zipFile;

    public ZipResourceProvider(Path file) {
        super(file);
    }

    private FileSystem getZipFile() throws IOException {
        if (this.zipFile == null)
            this.zipFile = FileSystems.newFileSystem(
                URI.create("jar:" + source.toAbsolutePath().toUri()),
                Map.of());
        return this.zipFile;
    }

    @Override
    protected Path resolve(String... paths) throws IOException {
        Path path = getZipFile().getPath("/");
        for(String name : paths)
            path = path.resolve(name);
        return path;
    }
}
