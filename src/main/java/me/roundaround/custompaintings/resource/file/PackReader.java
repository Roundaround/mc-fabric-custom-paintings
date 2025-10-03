package me.roundaround.custompaintings.resource.file;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.resource.file.accessor.DirectoryAccessor;
import me.roundaround.custompaintings.resource.file.accessor.FileAccessor;
import me.roundaround.custompaintings.resource.file.accessor.ZipAccessor;
import me.roundaround.custompaintings.resource.file.json.CustomPaintingsJson;
import me.roundaround.custompaintings.resource.file.json.LegacyCustomPaintingsJson;
import me.roundaround.custompaintings.resource.file.json.PackMcmeta;
import me.roundaround.custompaintings.util.CustomId;

public final class PackReader {
  private static final String CUSTOMPAINTINGS_JSON = "custompaintings.json";
  private static final String CUSTOM_PAINTINGS_JSON = "custom_paintings.json";
  private static final String PACK_MCMETA = "pack.mcmeta";
  private static final String PACK_PNG = "pack.png";
  private static final String ICON_PNG = "icon.png";
  private static final String LOG_NO_META = "Skipping potential pack \"%s\" with no %s file";
  private static final String LOG_META_PARSE_FAIL = "Skipping potential pack \"%s\" after failing to parse %s";
  private static final String LOG_MCMETA_PARSE_FAIL = "Failed to parse %s file for %s";
  private static final String LOG_NO_PAINTINGS = "Skipping potential pack \"%s\" because it contained no paintings";
  private static final String LOG_NO_ICON = "Missing %s file for pack \"%s\"";
  private static final String LOG_ICON_READ_FAIL = "Failed to read %s file for %s";

  public static Metadata readMetadata(Path path) {
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      try (FileAccessor accessor = fileAttributes.isDirectory()
          ? new DirectoryAccessor(path)
          : new ZipAccessor(path)) {
        return readMetadata(accessor);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
    }

    return null;
  }

  public static HashMap<CustomId, Image> readPaintingImages(Metadata metadata) {
    Path path = metadata.path();
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      try (FileAccessor accessor = fileAttributes.isDirectory()
          ? new DirectoryAccessor(path)
          : new ZipAccessor(path)) {
        return readPaintingImages(accessor, metadata);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
    }

    return new HashMap<>();
  }

  private static Metadata readMetadata(FileAccessor accessor) {
    String customPaintingsJsonPath = CUSTOMPAINTINGS_JSON;
    if (!accessor.hasFile(customPaintingsJsonPath)) {
      customPaintingsJsonPath = CUSTOM_PAINTINGS_JSON;
    }
    if (!accessor.hasFile(customPaintingsJsonPath)) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_NO_META, accessor.getFileName(), CUSTOMPAINTINGS_JSON));
      return null;
    }

    boolean isLegacy = accessor.hasFile(PACK_MCMETA);
    Pack pack = isLegacy
        ? readLegacyPack(accessor, customPaintingsJsonPath)
        : readPack(accessor, customPaintingsJsonPath);

    return new Metadata(
        accessor.getPath(),
        generateFileUid(accessor),
        pack,
        readIcon(accessor),
        isLegacy);
  }

  private static Pack readPack(FileAccessor accessor, String customPaintingsJsonPath) {
    CustomPaintingsJson json = null;
    try (BufferedReader reader = accessor.getBufferedReader(customPaintingsJsonPath)) {
      json = CustomPaintingsMod.GSON.fromJson(reader, CustomPaintingsJson.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER
          .warn(String.format(LOG_META_PARSE_FAIL, accessor.getFileName(), customPaintingsJsonPath), e);
      return null;
    }

    if (json.paintings().isEmpty()) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, accessor.getFileName());
      return null;
    }

    return new Pack(
        json.format(),
        json.id(),
        json.name(),
        json.description(),
        json.sourceLegacyPack(),
        json.paintings(),
        json.migrations());
  }

  private static Pack readLegacyPack(FileAccessor accessor, String customPaintingsJsonPath) {
    LegacyCustomPaintingsJson json = null;
    try (BufferedReader reader = accessor.getBufferedReader(customPaintingsJsonPath)) {
      json = CustomPaintingsMod.GSON.fromJson(reader, LegacyCustomPaintingsJson.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER
          .warn(String.format(LOG_META_PARSE_FAIL, accessor.getFileName(), customPaintingsJsonPath), e);
      return null;
    }

    if (json.paintings().isEmpty()) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, accessor.getFileName());
      return null;
    }

    PackMcmeta mcmeta = null;
    try (BufferedReader reader = accessor.getBufferedReader(PACK_MCMETA)) {
      mcmeta = CustomPaintingsMod.GSON.fromJson(reader, PackMcmeta.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_MCMETA_PARSE_FAIL, accessor.getFileName(), PACK_MCMETA), e);
    }

    return new Pack(
        json.id(),
        json.name(),
        mcmeta == null ? "" : mcmeta.pack().description(),
        json.paintings(),
        json.migrations());
  }

  private static FileUid generateFileUid(FileAccessor accessor) {
    String dirname = accessor.getFileName();
    long lastModified = ResourceUtil.lastModified(accessor.getPath());
    long fileSize = ResourceUtil.fileSize(accessor.getPath());
    return new FileUid(accessor.isZip(), dirname, lastModified, fileSize);
  }

  private static Image readIcon(FileAccessor accessor) {
    String iconPath = PACK_PNG;
    if (!accessor.hasFile(iconPath)) {
      iconPath = ICON_PNG;
    }
    if (accessor.hasFile(iconPath)) {
      try (InputStream inputStream = accessor.getInputStream(iconPath)) {
        return Image.read(inputStream);
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.warn(String.format(LOG_ICON_READ_FAIL, iconPath, accessor.getFileName()), e);
      }
    }

    CustomPaintingsMod.LOGGER.warn(String.format(LOG_NO_ICON, PACK_PNG, accessor.getFileName()));
    return null;
  }

  private static HashMap<CustomId, Image> readPaintingImages(FileAccessor accessor, Metadata metadata) {
    HashMap<CustomId, Image> images = new HashMap<>();

    String packId = metadata.pack().id();
    List<Painting> paintings = metadata.pack().paintings();

    for (Painting painting : paintings) {
      Image image = readImage(accessor, getPaintingPath(accessor, packId, painting.id(), metadata.isLegacy()));
      if (image != null) {
        images.put(new CustomId(packId, painting.id()), image);
      }
    }

    return images;
  }

  private static String getPaintingPath(FileAccessor accessor, String packId, String paintingId, boolean isLegacy) {
    ArrayList<String> segments = isLegacy
        ? new ArrayList<>(List.of("assets", packId, "textures", "painting", paintingId + ".png"))
        : new ArrayList<>(List.of("images", paintingId + ".png"));
    return String.join(accessor.getPathSeparator(), segments);
  }

  private static Image readImage(FileAccessor accessor, String path) {
    if (!accessor.hasFile(path)) {
      // TODO: Move to const
      CustomPaintingsMod.LOGGER.warn("Missing painting image file for \"{}\"", path);
      return null;
    }

    try (InputStream inputStream = accessor.getInputStream(path)) {
      return Image.read(inputStream);
    } catch (Exception e) {
      // TODO: Move to const
      CustomPaintingsMod.LOGGER.warn("Failed to read painting image file for \"{}\"", path, e);
      return null;
    }
  }

  private PackReader() {
  }
}
