package me.roundaround.custompaintings.resource.file.accessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipAccessor extends FileAccessor {
  private ZipFile zip;
  private String folderPrefix;

  public ZipAccessor(Path path) throws IOException {
    super(path);
    this.zip = new ZipFile(this.path.toFile());
    this.folderPrefix = getFolderPrefix(this.zip);
  }

  @Override
  public boolean hasFile(String path) {
    return this.zip.getEntry(this.folderPrefix + path) != null;
  }

  @Override
  public BufferedReader getBufferedReader(String path) throws IOException {
    return new BufferedReader(
        new InputStreamReader(this.zip.getInputStream(this.zip.getEntry(this.folderPrefix + path))));
  }

  @Override
  public InputStream getInputStream(String path) throws IOException {
    return this.zip.getInputStream(this.zip.getEntry(this.folderPrefix + path));
  }

  @Override
  public boolean isZip() {
    return true;
  }

  @Override
  public String getPathSeparator() {
    return "/";
  }

  @Override
  public void close() throws IOException {
    this.zip.close();
  }

  private static String getFolderPrefix(ZipFile zip) {
    Enumeration<? extends ZipEntry> entries = zip.entries();
    if (!entries.hasMoreElements()) {
      return "";
    }

    ZipEntry firstEntry = entries.nextElement();
    if (!firstEntry.isDirectory()) {
      return "";
    }

    String folderPrefix = firstEntry.getName();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (!entry.getName().startsWith(folderPrefix)) {
        return "";
      }
    }

    return folderPrefix;
  }
}
