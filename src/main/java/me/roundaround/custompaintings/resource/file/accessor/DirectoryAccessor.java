package me.roundaround.custompaintings.resource.file.accessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class DirectoryAccessor extends FileAccessor {
  public DirectoryAccessor(Path path) {
    super(path);
  }

  @Override
  public boolean hasFile(String path) {
    return Files.isRegularFile(this.path.resolve(path), LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public BufferedReader getBufferedReader(String path) throws IOException {
    return Files.newBufferedReader(this.path.resolve(path));
  }

  @Override
  public InputStream getInputStream(String path) throws IOException {
    return Files.newInputStream(this.path.resolve(path), LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public boolean isZip() {
    return false;
  }
}