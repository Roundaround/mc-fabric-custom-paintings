package me.roundaround.custompaintings.resource;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.network.CustomId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceUtil {
  private static final String CUSTOM_PAINTINGS_JSON = "custompaintings.json";

  private ResourceUtil() {
  }

  public static long lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException ignored) {
      return 0;
    }
  }

  public static long fileSize(Path path) {
    try {
      if (Files.isRegularFile(path)) {
        return Files.size(path);
      }
      return directorySize(path, true);
    } catch (IOException ignored) {
      return 0;
    }
  }

  public static long directorySize(Path path) {
    return directorySize(path, false);
  }

  public static long directorySize(Path path, boolean recursive) {
    var size = new Object() {
      long value = 0L;
    };

    try (Stream<Path> walk = Files.walk(path)) {
      for (Path file : (Iterable<Path>) walk::iterator) {
        if (file.equals(path)) {
          continue;
        }
        try {
          if (recursive) {
            size.value += fileSize(file);
          } else if (Files.isRegularFile(file)) {
            size.value += Files.size(file);
          }
        } catch (IOException ignored) {
        }
      }
    } catch (IOException ignored) {
    }

    return size.value;
  }

  public static String getFolderPrefix(ZipFile zip) {
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

  public static String stripTrailingSeparator(String path) {
    if (path.endsWith("/") || path.endsWith("\\")) {
      return path.substring(0, path.length() - 1);
    }
    return path;
  }

  public static ZipEntry getImageZipEntry(ZipFile zip, String... path) {
    return getImageZipEntry(zip, List.of(path));
  }

  public static ZipEntry getImageZipEntry(ZipFile zip, Iterable<String> path) {
    // Trim any trailing separators and remove any blank entries.
    ArrayList<String> sanitized = new ArrayList<>();
    for (String element : path) {
      if (element == null) {
        continue;
      }
      String trimmed = stripTrailingSeparator(element);
      if (!trimmed.isBlank()) {
        sanitized.add(trimmed);
      }
    }

    // Try both forward and backward slash
    ZipEntry zipImage = zip.getEntry(String.join("/", sanitized));
    if (zipImage == null) {
      zipImage = zip.getEntry(String.join("\\", sanitized));
    }
    return zipImage;
  }

  public static Image readImageFromZip(ZipFile zip, String... path) {
    return readImageFromZip(zip, List.of(path));
  }

  public static Image readImageFromZip(ZipFile zip, Iterable<String> path) {
    ZipEntry entry = getImageZipEntry(zip, path);
    if (entry == null) {
      return null;
    }

    try (InputStream stream = zip.getInputStream(entry)) {
      BufferedImage image = ImageIO.read(stream);
      if (image == null) {
        return null;
      }

      return Image.read(image);
    } catch (IOException e) {
      return null;
    }
  }

  public static boolean isPaintingPack(Path path) {
    if (!Files.exists(path)) {
      return false;
    }

    if (Files.isDirectory(path)) {
      return Files.exists(path.resolve(CUSTOM_PAINTINGS_JSON));
    } else if (Files.isRegularFile(path)) {
      try (ZipFile zip = new ZipFile(path.toFile())) {
        return zip.getEntry(CUSTOM_PAINTINGS_JSON) != null;
      } catch (IOException e) {
        return false;
      }
    }
    return false;
  }

  public static HashSet<CustomId> getAllImageIds(Collection<String> packs, Collection<CustomId> paintings) {
    HashSet<CustomId> neededIds = new HashSet<>();
    neededIds.addAll(packs.stream().map(PackIcons::customId).toList());
    neededIds.addAll(paintings);
    return neededIds;
  }

  public static HashResult hashImages(Map<CustomId, Image> images) throws IOException {
    HashMap<CustomId, String> imageHashes = new HashMap<>();
    LinkedHashMap<CustomId, ByteSource> byteSources = getByteSources(images);

    for (var entry : byteSources.entrySet()) {
      imageHashes.put(entry.getKey(), entry.getValue().hash(Hashing.sha256()).toString());
    }

    String combinedImageHash = calculateCombinedHash(byteSources);

    return new HashResult(combinedImageHash, imageHashes);
  }

  public static String calculateCombinedHash(Map<CustomId, Image> images) throws IOException {
    return calculateCombinedHash(getByteSources(images));
  }

  private static LinkedHashMap<CustomId, ByteSource> getByteSources(Map<CustomId, Image> images) {
    TreeSet<CustomId> imageIds = new TreeSet<>(images.keySet());
    LinkedHashMap<CustomId, ByteSource> byteSources = new LinkedHashMap<>();
    for (CustomId id : imageIds) {
      byteSources.putIfAbsent(id, images.get(id).getByteSource());
    }
    return byteSources;
  }

  private static String calculateCombinedHash(LinkedHashMap<CustomId, ByteSource> byteSources) throws IOException {
    return ByteSource.concat(byteSources.values()).hash(Hashing.sha256()).toString();
  }
}
