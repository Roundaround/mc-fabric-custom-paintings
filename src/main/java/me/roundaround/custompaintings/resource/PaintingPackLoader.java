package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.event.InitialDataPackLoadEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.resource.fs.ResourceFileSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public class PaintingPackLoader extends SinglePreparationResourceReloader<HashMap<String, PaintingPack>> implements
    IdentifiableResourceReloadListener {
  private static final String META_FILENAME = "custompaintings.json";

  private Path directory = null;

  public PaintingPackLoader() {
    InitialDataPackLoadEvent.EVENT.register(this::prepForLoading);
    ServerLifecycleEvents.SERVER_STOPPING.register(this::clear);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void prepForLoading(LevelStorage.Session session) {
    this.directory = session.getDirectory().path().resolve(CustomPaintingsMod.MOD_ID);
    this.directory.toFile().mkdirs();
  }

  private void clear(MinecraftServer server) {
    this.directory = null;
  }

  @Override
  public Identifier getFabricId() {
    return new Identifier(CustomPaintingsMod.MOD_ID, "paintings");
  }

  @Override
  protected HashMap<String, PaintingPack> prepare(ResourceManager manager, Profiler profiler) {
    CustomPaintingsMod.LOGGER.info("Loading painting packs");

    if (this.directory == null) {
      CustomPaintingsMod.LOGGER.info("Missing pointer to working directory, skipping");
      return new HashMap<>(0);
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.directory, "*.zip")) {
      HashMap<String, PaintingPack> packs = new HashMap<>();
      directoryStream.forEach((path) -> {
        CustomPaintingsMod.LOGGER.info("Resource found: {}", path);
      });
      return packs;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void apply(HashMap<String, PaintingPack> packs, ResourceManager manager, Profiler profiler) {

  }

  private static PaintingPack readAsPack(Path path) throws IOException {
    BasicFileAttributes fileAttributes;
    try {
      fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException e) {
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

  private static PaintingPack readZipAsPack(Path path) throws IOException {
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

  private static PaintingPack readDirectoryAsPack(Path path) throws IOException {
    if (!Files.isRegularFile(path.resolve(META_FILENAME), LinkOption.NOFOLLOW_LINKS)) {
      CustomPaintingsMod.LOGGER.warn("Found Custom Paintings directory \"{}\" without a {} file, skipping...",
          path.getFileName(), META_FILENAME
      );
      return null;
    }

    JsonObject metaJson = JsonHelper.deserialize(Files.newBufferedReader(path.resolve(META_FILENAME)));

    return null;
  }
}
