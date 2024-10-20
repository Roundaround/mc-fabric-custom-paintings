package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.roundalib.util.PathAccessor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PaintingPackLoader extends SinglePreparationResourceReloader<PaintingPackLoader.LoadResult> implements
    IdentifiableResourceReloadListener {
  public static final Identifier FABRIC_ID = new Identifier(CustomPaintingsMod.MOD_ID, "paintings");

  private static final String META_FILENAME = "custompaintings.json";
  private static final String LOG_NO_META = "Found Custom Paintings pack \"{}\" without a {} file, skipping...";
  private static final String LOG_META_PARSE_FAIL = "Failed to parse {} from \"{}\", skipping...";
  private static final String LOG_NO_PAINTINGS = "No paintings found in \"{}\", skipping...";
  private static final String LOG_NO_ICON = "Missing icon.png file for {}";
  private static final String LOG_ICON_READ_FAIL = "Failed to read icon.png file for {}";
  private static final String LOG_MISSING_PAINTING = "Missing custom painting image file for {}";
  private static final String LOG_LARGE_IMAGE = "Image file for {} is too large, skipping";
  private static final String LOG_PAINTING_READ_FAIL = "Failed to read custom painting image file for {}";
  private static final Gson GSON = new GsonBuilder().create();
  private static final int MAX_SIZE = 1 << 24;

  @Override
  public Identifier getFabricId() {
    return FABRIC_ID;
  }

  @Override
  protected LoadResult prepare(ResourceManager manager, Profiler profiler) {
    CustomPaintingsMod.LOGGER.info("Loading painting packs");
    Path packsDir = PathAccessor.getInstance().getPerWorldModDir(CustomPaintingsMod.MOD_ID);

    if (packsDir == null) {
      CustomPaintingsMod.LOGGER.info("Unable to locate packs directory, skipping");
      return LoadResult.empty();
    }

    if (Files.notExists(packsDir)) {
      return LoadResult.empty();
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(packsDir)) {
      HashMap<String, PackResource> packs = new HashMap<>();
      HashMap<Identifier, Image> images = new HashMap<>();
      directoryStream.forEach((path) -> {
        SinglePackResult result = readAsPack(path);
        if (result != null) {
          packs.put(result.id, result.pack);
          images.putAll(result.images);
        }
      });
      return new LoadResult(packs, images);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void apply(LoadResult result, ResourceManager manager, Profiler profiler) {
    HashMap<String, PaintingPack> packs = new HashMap<>(result.packs.size());
    result.packs.forEach((packId, pack) -> packs.put(packId, new PaintingPack(packId, pack.name(), pack.description(),
        pack.paintings()
            .stream()
            .map((resource) -> new PaintingData(new Identifier(packId, resource.id()), resource.width(),
                resource.height(), resource.name(), resource.artist()
            ))
            .toList()
    )));

    ServerPaintingRegistry.getInstance().update(packs, result.images);
  }

  private static SinglePackResult readAsPack(Path path) {
    BasicFileAttributes fileAttributes;
    try {
      fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readDirectoryAsPack(path);
      }

      if (fileAttributes.isRegularFile()) {
        return readZipAsPack(path);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
      return null;
    }

    return null;
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

  private static SinglePackResult readZipAsPack(Path path) {
    String filename = path.getFileName().toString();

    if (!filename.endsWith(".zip")) {
      CustomPaintingsMod.LOGGER.warn("Found non-zip Custom Paintings file \"{}\", skipping...", filename);
      return null;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      CustomPaintingsMod.LOGGER.warn(
          "Found zip Custom Paintings file \"{}\" outside the system's default file system, skipping...", filename);
      return null;
    }

    try (ZipFile zip = new ZipFile(path.toFile())) {
      String folderPrefix = getFolderPrefix(zip);
      if (!folderPrefix.isBlank()) {
        CustomPaintingsMod.LOGGER.info("Folder-in-zip detected in \"{}\", adjusting paths", filename);
      }

      ZipEntry zipMeta = zip.getEntry(folderPrefix + META_FILENAME);
      if (zipMeta == null) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_META, filename, META_FILENAME);
        return null;
      }

      PackResource pack;
      try (InputStream stream = zip.getInputStream(zipMeta)) {
        pack = GSON.fromJson(new InputStreamReader(stream), PackResource.class);
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn(LOG_META_PARSE_FAIL, META_FILENAME, filename);
        return null;
      }

      if (pack.paintings().isEmpty()) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, filename);
        return null;
      }

      HashMap<Identifier, Image> images = new HashMap<>();

      ZipEntry zipIconImage = zip.getEntry(folderPrefix + "icon.png");
      if (zipIconImage == null) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_ICON, pack.id());
      } else {
        try (InputStream stream = zip.getInputStream(zipIconImage)) {
          BufferedImage image = ImageIO.read(stream);
          if (image == null) {
            throw new IOException("BufferedImage is null");
          }

          images.put(PackIcons.identifier(pack.id()), Image.read(image));
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(e);
          CustomPaintingsMod.LOGGER.warn(LOG_ICON_READ_FAIL, pack.id());
        }
      }

      pack.paintings().forEach((painting) -> {
        Identifier id = new Identifier(pack.id(), painting.id());
        ZipEntry zipImage = zip.getEntry(folderPrefix + String.format("images/%s.png", painting.id()));
        if (zipImage == null) {
          CustomPaintingsMod.LOGGER.warn(LOG_MISSING_PAINTING, id);
          return;
        }

        try (InputStream stream = zip.getInputStream(zipImage)) {
          BufferedImage image = ImageIO.read(stream);
          if (image == null) {
            throw new IOException("BufferedImage is null");
          }

          long size = (long) image.getWidth() * image.getHeight();
          if (size > MAX_SIZE) {
            CustomPaintingsMod.LOGGER.warn(LOG_LARGE_IMAGE, id);
            return;
          }

          images.put(id, Image.read(image));
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(e);
          CustomPaintingsMod.LOGGER.warn(LOG_PAINTING_READ_FAIL, id);
        }
      });

      return new SinglePackResult(pack.id(), pack, images);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static SinglePackResult readDirectoryAsPack(Path path) {
    String dirname = path.getFileName().toString();

    if (!Files.isRegularFile(path.resolve(META_FILENAME), LinkOption.NOFOLLOW_LINKS)) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_META, dirname, META_FILENAME);
      return null;
    }

    PackResource pack;
    try {
      pack = GSON.fromJson(Files.newBufferedReader(path.resolve(META_FILENAME)), PackResource.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn(LOG_META_PARSE_FAIL, META_FILENAME, dirname);
      return null;
    }

    if (pack.paintings().isEmpty()) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, dirname);
      return null;
    }

    HashMap<Identifier, Image> images = new HashMap<>();

    Path iconImagePath = path.resolve("icon.png");
    if (!Files.exists(iconImagePath)) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_ICON, pack.id());
    } else {
      try {
        BufferedImage image = ImageIO.read(Files.newInputStream(iconImagePath, LinkOption.NOFOLLOW_LINKS));
        if (image == null) {
          throw new IOException("BufferedImage is null");
        }

        images.put(PackIcons.identifier(pack.id()), Image.read(image));
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn(LOG_ICON_READ_FAIL, pack.id());
      }
    }

    pack.paintings().forEach((painting) -> {
      Identifier id = new Identifier(pack.id(), painting.id());
      Path imagePath = path.resolve("images").resolve(painting.id() + ".png");
      if (!Files.exists(imagePath)) {
        CustomPaintingsMod.LOGGER.warn(LOG_MISSING_PAINTING, id);
        return;
      }

      try {
        BufferedImage image = ImageIO.read(Files.newInputStream(imagePath, LinkOption.NOFOLLOW_LINKS));
        if (image == null) {
          throw new IOException("BufferedImage is null");
        }

        long size = (long) image.getWidth() * image.getHeight();
        if (size > MAX_SIZE) {
          CustomPaintingsMod.LOGGER.warn(LOG_LARGE_IMAGE, id);
          return;
        }

        images.put(id, Image.read(image));
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn(LOG_PAINTING_READ_FAIL, id);
      }
    });

    return new SinglePackResult(pack.id(), pack, images);
  }

  protected record SinglePackResult(String id, PackResource pack, HashMap<Identifier, Image> images) {
  }

  protected record LoadResult(HashMap<String, PackResource> packs, HashMap<Identifier, Image> images) {
    public static LoadResult empty() {
      return new LoadResult(new HashMap<>(0), new HashMap<>(0));
    }
  }
}
