package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.roundalib.PathAccessor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public class PaintingPackLoader extends SinglePreparationResourceReloader<PaintingPackLoader.LoadResult> implements
    IdentifiableResourceReloadListener {
  private static final String META_FILENAME = "custompaintings.json";
  private static final Gson GSON = new GsonBuilder().create();
  private static final int MAX_SIZE = 1 << 24;

  @Override
  public Identifier getFabricId() {
    return new Identifier(CustomPaintingsMod.MOD_ID, "paintings");
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
      HashMap<Identifier, PaintingImage> images = new HashMap<>();
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
    result.packs.forEach((packId, pack) -> packs.put(packId, new PaintingPack(packId, pack.name(), pack.paintings()
        .stream()
        .map((resource) -> new PaintingData(new Identifier(packId, resource.id()), resource.width(), resource.height(),
            resource.name(), resource.artist()
        ))
        .toList())));

    ServerPaintingRegistry.getInstance().update(packs, result.images);
  }

  private static SinglePackResult readAsPack(Path path) {
    BasicFileAttributes fileAttributes;
    try {
      fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
      return null;
    }

    if (fileAttributes.isDirectory()) {
      return readDirectoryAsPack(path);
    }

    if (fileAttributes.isRegularFile()) {
      return readZipAsPack(path);
    }

    return null;
  }

  private static SinglePackResult readZipAsPack(Path path) {
    if (!path.getFileName().toString().endsWith(".zip")) {
      CustomPaintingsMod.LOGGER.warn("Found non-zip Custom Paintings file \"{}\", skipping...", path.getFileName());
      return null;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      CustomPaintingsMod.LOGGER.warn(
          "Found zip Custom Paintings file \"{}\" outside the system's default file system, skipping...",
          path.getFileName()
      );
      return null;
    }

    return null;
  }

  private static SinglePackResult readDirectoryAsPack(Path path) {
    if (!Files.isRegularFile(path.resolve(META_FILENAME), LinkOption.NOFOLLOW_LINKS)) {
      CustomPaintingsMod.LOGGER.warn("Found Custom Paintings directory \"{}\" without a {} file, skipping...",
          path.getFileName(), META_FILENAME
      );
      return null;
    }

    PackResource pack;
    try {
      pack = GSON.fromJson(Files.newBufferedReader(path.resolve(META_FILENAME)), PackResource.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Failed to parse {} from \"{}\", skipping...", META_FILENAME, path.getFileName());
      return null;
    }

    if (pack.paintings().isEmpty()) {
      CustomPaintingsMod.LOGGER.warn("No paintings found in \"{}\", skipping...", path.getFileName());
      return null;
    }

    HashMap<Identifier, PaintingImage> images = new HashMap<>();
    Path imageDir = path.resolve("images");
    if (!Files.exists(imageDir, LinkOption.NOFOLLOW_LINKS)) {
      return new SinglePackResult(pack.id(), pack, images);
    }

    pack.paintings().forEach((painting) -> {
      Identifier id = new Identifier(pack.id(), painting.id());
      Path imagePath = imageDir.resolve(painting.id() + ".png");
      if (!Files.exists(imagePath)) {
        CustomPaintingsMod.LOGGER.warn("Missing custom painting image file for {}", id);
        return;
      }

      try {
        BufferedImage image = ImageIO.read(Files.newInputStream(imagePath, LinkOption.NOFOLLOW_LINKS));
        if (image == null) {
          throw new IOException("BufferedImage is null");
        }

        long size = (long) image.getWidth() * image.getHeight();
        if (size > MAX_SIZE) {
          CustomPaintingsMod.LOGGER.warn("Image file for {} is too large, skipping", id);
          return;
        }

        images.put(id, PaintingImage.read(image));
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn("Failed to read custom painting image file for {}", id);
      }
    });

    return new SinglePackResult(pack.id(), pack, images);
  }

  protected record SinglePackResult(String id, PackResource pack, HashMap<Identifier, PaintingImage> images) {
  }

  protected record LoadResult(HashMap<String, PackResource> packs, HashMap<Identifier, PaintingImage> images) {
    public static LoadResult empty() {
      return new LoadResult(new HashMap<>(0), new HashMap<>(0));
    }
  }
}