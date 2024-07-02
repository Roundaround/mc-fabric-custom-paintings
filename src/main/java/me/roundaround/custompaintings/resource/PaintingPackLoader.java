package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.server.event.InitialDataPackLoadEvent;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public class PaintingPackLoader extends SinglePreparationResourceReloader<PaintingPackLoader.LoadResult> implements
    IdentifiableResourceReloadListener {
  private static final String META_FILENAME = "custompaintings.json";
  private static final Gson GSON = new GsonBuilder().create();

  private Path directory = null;

  public PaintingPackLoader() {
    InitialDataPackLoadEvent.EVENT.register(this::prepForLoading);
    ServerLifecycleEvents.SERVER_STOPPING.register((server) -> this.clear());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void prepForLoading(LevelStorage.Session session) {
    this.directory = session.getDirectory().path().resolve(CustomPaintingsMod.MOD_ID);
    this.directory.toFile().mkdirs();
  }

  private void clear() {
    this.directory = null;
  }

  @Override
  public Identifier getFabricId() {
    return new Identifier(CustomPaintingsMod.MOD_ID, "paintings");
  }

  @Override
  protected LoadResult prepare(ResourceManager manager, Profiler profiler) {
    CustomPaintingsMod.LOGGER.info("Loading painting packs");

    if (this.directory == null) {
      CustomPaintingsMod.LOGGER.info("Missing pointer to working directory, skipping");
      return LoadResult.empty();
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.directory)) {
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

    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();
    if (registry == null) {

    }
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
        images.put(id, PaintingImage.read(Files.newInputStream(imagePath, LinkOption.NOFOLLOW_LINKS)));
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
