package me.roundaround.custompaintings.resource.file.accessor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public abstract class FileAccessor implements Closeable {
  protected final Path path;

  protected FileAccessor(Path path) {
    this.path = path;
  }

  public Path getPath() {
    return this.path;
  }

  public String getFileName() {
    return this.path.getFileName().toString();
  }

  public String getPathSeparator() {
    return File.separator;
  }

  public abstract boolean hasFile(String path);

  public abstract BufferedReader getBufferedReader(String path) throws IOException;

  public abstract InputStream getInputStream(String path) throws IOException;

  public abstract boolean isZip();

  @Override
  public void close() throws IOException {
  }
}
