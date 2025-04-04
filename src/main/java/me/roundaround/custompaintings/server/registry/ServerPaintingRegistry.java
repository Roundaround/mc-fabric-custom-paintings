package me.roundaround.custompaintings.server.registry;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.*;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;
import me.roundaround.custompaintings.roundalib.util.PathAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServerPaintingRegistry extends CustomPaintingRegistry {
  private static final String META_FILENAME = "custompaintings.json";
  private static final String PACK_PNG = "pack.png";
  private static final String ICON_PNG = "icon.png";
  private static final String LOG_FAIL_ALL = "Skipping loading packs due to an error";
  private static final String LOG_FAIL_GENERIC = "Skipping potential pack \"%s\" to to an error while loading";
  private static final String LOG_FAIL_IMAGES = "Skipping loading images for pack \"%s\" to to an error while loading";
  private static final String LOG_NO_META = "Skipping potential pack \"{}\" with no {} file";
  private static final String LOG_META_PARSE_FAIL = "Skipping potential pack \"%s\" after failing to parse %s";
  private static final String LOG_ID_VALIDATION_FAIL = "Skipping potential pack \"%s\" due to a validation error in %s";
  private static final String LOG_NO_PAINTINGS = "Skipping potential pack \"{}\" because it contained no paintings";
  private static final String LOG_NO_ICON = "Missing icon.png file for pack \"{}\"";
  private static final String LOG_LEGACY_ICON =
      "Deprecated/legacy pack.png file found for pack \"{}\". Rename to " + "icon.png";
  private static final String LOG_ICON_READ_FAIL = "Failed to read icon.png file for %s";
  private static final String LOG_MISSING_PAINTING = "Missing custom painting image file for {}";
  private static final String LOG_LARGE_IMAGE = "Image file for {} is too large, skipping";
  private static final String LOG_PAINTING_READ_FAIL = "Failed to read image file for %s";
  private static final int MAX_SIZE = 1 << 24;

  private static ServerPaintingRegistry instance = null;

  private final HashMap<CustomId, Boolean> finishedMigrations = new HashMap<>();

  private MinecraftServer server;
  private boolean safeMode = false;
  private int loadErrorOrSkipCount = 0;

  private ServerPaintingRegistry() {
    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      if (server == this.server) {
        this.clear();
      }
    });
  }

  public static void init(MinecraftServer server) {
    ServerPaintingRegistry registry = getInstance();
    if (server == null || !server.isRunning()) {
      return;
    }

    registry.setServer(server);
  }

  public static ServerPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ServerPaintingRegistry();
    }
    return instance;
  }

  @Override
  protected DynamicRegistryManager getRegistryManager() {
    return this.server == null ? null : this.server.getRegistryManager();
  }

  @Override
  public void clear() {
    super.clear();
    this.finishedMigrations.clear();
    this.server = null;
  }

  public void setServer(MinecraftServer server) {
    if (this.server != null && this.server != server) {
      this.clear();
    }
    this.server = server;
    this.sendSummaryToAll();
  }

  public void firstLoadPaintingPacks(boolean safeMode) {
    this.safeMode = safeMode;
    LoadResult loadResult = this.safeMode ? LoadResult.empty(0) : this.loadPaintingPacks();
    this.loadErrorOrSkipCount = loadResult.erroredOrSkipped();
    this.setPacks(loadResult.packs());
    this.setImages(loadResult.images());
  }

  public void reloadPaintingPacks(Consumer<MinecraftServer> onSucceed) {
    if (this.server == null || !this.server.isRunning()) {
      return;
    }

    this.safeMode = false;
    CompletableFuture.supplyAsync(this::loadPaintingPacks, Util.getIoWorkerExecutor()).thenAcceptAsync((loadResult) -> {
      this.loadErrorOrSkipCount = loadResult.erroredOrSkipped();
      this.setPacks(loadResult.packs());
      this.setImages(loadResult.images());
      this.sendSummaryToAll();
      onSucceed.accept(this.server);
    }, this.server);
  }

  public void setImages(HashMap<CustomId, Image> images) {
    HashResult hashResult = ResourceUtil.hashImages(images);
    this.images.setAll(images, hashResult.imageHashes());
    this.combinedImageHash = hashResult.combinedImageHash();
  }

  public void sendSummaryToAll() {
    ServerNetworking.sendSummaryPacketToAll(this.server, this.packsList, this.combinedImageHash,
        this.finishedMigrations, this.safeMode, this.loadErrorOrSkipCount
    );
  }

  public void sendSummaryToPlayer(ServerPlayerEntity player) {
    ServerNetworking.sendSummaryPacket(player, this.packsList, this.combinedImageHash, this.finishedMigrations,
        this.safeMode, this.loadErrorOrSkipCount
    );
  }

  public void checkPlayerHashes(ServerPlayerEntity player, Map<CustomId, String> hashes) {
    HashMap<CustomId, Image> images = new HashMap<>();
    this.images.forEach((id, image, hash) -> {
      if (hash.equals(hashes.get(id))) {
        return;
      }
      images.put(id, image);
    });

    if (images.isEmpty()) {
      CustomPaintingsMod.LOGGER.info(
          "{} has incorrect combined hash, but all correct images. " +
          "This is likely due to paintings being removed server-side.", player.getName().getString());
      ServerNetworking.sendDownloadSummaryPacket(player, new HashSet<>(0), 0, 0);
      return;
    }

    CustomPaintingsMod.LOGGER.info(
        "{} needs to download {} image(s). Sending to client.", player.getName().getString(), images.size());
    ImagePacketQueue.getInstance().add(player, images);
  }

  public void markMigrationFinished(CustomId migrationId, boolean succeeded) {
    this.finishedMigrations.put(migrationId, succeeded);
  }

  private LoadResult loadPaintingPacks() {
    CustomPaintingsMod.LOGGER.info("Loading painting packs");
    Path packsDir = PathAccessor.getInstance().getPerWorldModDir(CustomPaintingsMod.MOD_ID);

    if (packsDir == null || Files.notExists(packsDir)) {
      CustomPaintingsMod.LOGGER.info("Unable to locate packs directory, skipping");
      return LoadResult.empty(0);
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(packsDir)) {
      Set<String> disabledPacks = ServerInfo.getInstance().getDisabledPacks();
      HashMap<String, PackData> packs = new HashMap<>();
      HashMap<String, String> packFilenames = new HashMap<>();
      HashMap<CustomId, Image> images = new HashMap<>();
      var erroredOrSkipped = new Object() {
        int value = 0;
      };
      directoryStream.forEach((path) -> {
        PackMetadata<PackResource> meta = readPackMetadata(path);
        if (meta == null) {
          erroredOrSkipped.value++;
          return;
        }

        PackResource resource = meta.pack();
        String packId = resource.id();
        String filename = path.getFileName().toString();

        String existingFilename = packFilenames.get(packId);
        if (existingFilename != null) {
          CustomPaintingsMod.LOGGER.warn(
              "Multiple packs with id \"{}\" detected. Only the first will be kept. Please make sure packs have " +
              "unique IDs!\nKeeping \"{}\" and discarding \"{}\"", packId, existingFilename, filename);
          erroredOrSkipped.value++;
          return;
        }

        packFilenames.put(packId, filename);

        PackFileUid packFileUid = meta.packFileUid();
        boolean disabled = disabledPacks.contains(packFileUid.stringValue());
        packs.put(packId, resource.toData(packFileUid, disabled));

        if (meta.icon() != null) {
          images.put(PackIcons.customId(packId), meta.icon());
        }
        if (!disabled) {
          images.putAll(readPaintingImages(path, meta.pack()));
        }
      });

      CustomPaintingsMod.LOGGER.info("Loaded {} pack(s) with {} painting(s)", packs.size(),
          packs.values().stream().mapToInt((pack) -> pack.paintings().size()).sum()
      );
      return new LoadResult(packs, images, erroredOrSkipped.value);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(LOG_FAIL_ALL, e);
      return LoadResult.empty(1);
    }
  }

  private static PackMetadata<PackResource> readPackMetadata(Path path) {
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readPackMetadataFromDirectory(path);
      }

      if (fileAttributes.isRegularFile()) {
        return readPackMetadataFromZip(path);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_FAIL_GENERIC, path.getFileName()), e);
    }

    return null;
  }

  private static PackMetadata<PackResource> readPackMetadataFromDirectory(Path path) {
    String dirname = path.getFileName().toString();

    if (!Files.isRegularFile(path.resolve(META_FILENAME), LinkOption.NOFOLLOW_LINKS)) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_META, dirname, META_FILENAME);
      return null;
    }

    PackResource pack;
    try {
      pack = CustomPaintingsMod.GSON.fromJson(Files.newBufferedReader(path.resolve(META_FILENAME)), PackResource.class);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_META_PARSE_FAIL, dirname, META_FILENAME), e);
      return null;
    }

    try {
      pack.validateIds();
    } catch (InvalidIdException e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_ID_VALIDATION_FAIL, dirname, META_FILENAME), e);
      return null;
    }

    if (pack.paintings().isEmpty()) {
      CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, dirname);
      return null;
    }

    long lastModified = ResourceUtil.lastModified(path);
    long fileSize = ResourceUtil.fileSize(path);
    PackFileUid packFileUid = new PackFileUid(false, dirname, lastModified, fileSize);

    Path iconImagePath = getIconPath(path, dirname);
    Image packIcon = null;
    if (iconImagePath != null) {
      try {
        BufferedImage image = ImageIO.read(Files.newInputStream(iconImagePath, LinkOption.NOFOLLOW_LINKS));
        if (image == null) {
          throw new IOException("BufferedImage is null");
        }

        packIcon = Image.read(image);
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(String.format(LOG_ICON_READ_FAIL, pack.id()), e);
      }
    }

    return new PackMetadata<>(packFileUid, pack, packIcon);
  }

  private static PackMetadata<PackResource> readPackMetadataFromZip(Path path) {
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
      String folderPrefix = ResourceUtil.getFolderPrefix(zip);
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
        pack = CustomPaintingsMod.GSON.fromJson(new InputStreamReader(stream), PackResource.class);
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.warn(String.format(LOG_META_PARSE_FAIL, filename, META_FILENAME), e);
        return null;
      }

      try {
        pack.validateIds();
      } catch (InvalidIdException e) {
        CustomPaintingsMod.LOGGER.warn(String.format(LOG_ID_VALIDATION_FAIL, filename, META_FILENAME), e);
        return null;
      }

      if (pack.paintings().isEmpty()) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_PAINTINGS, filename);
        return null;
      }

      long lastModified = ResourceUtil.lastModified(path);
      long fileSize = ResourceUtil.fileSize(path);
      PackFileUid packFileUid = new PackFileUid(true, filename, lastModified, fileSize);

      Image packIcon = null;
      ZipEntry zipIconImage = getIconZipEntry(zip, folderPrefix, filename);
      if (zipIconImage != null) {
        try (InputStream stream = zip.getInputStream(zipIconImage)) {
          BufferedImage image = ImageIO.read(stream);
          if (image == null) {
            throw new IOException("BufferedImage is null");
          }

          packIcon = Image.read(image);
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(String.format(LOG_ICON_READ_FAIL, pack.id()), e);
        }
      }

      return new PackMetadata<>(packFileUid, pack, packIcon);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_FAIL_GENERIC, filename), e);
    }

    return null;
  }

  private static ZipEntry getIconZipEntry(ZipFile zip, String folderPrefix, String filename) {
    ZipEntry entry = ResourceUtil.getImageZipEntry(zip, folderPrefix, ICON_PNG);
    if (entry == null) {
      entry = ResourceUtil.getImageZipEntry(zip, folderPrefix, PACK_PNG);
      if (entry == null) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_ICON, filename);
      } else {
        CustomPaintingsMod.LOGGER.warn(LOG_LEGACY_ICON, filename);
      }
    }
    return entry;
  }

  private static HashMap<CustomId, Image> readPaintingImages(Path path, PackResource pack) {
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readPaintingImagesFromDirectory(path, pack);
      }

      if (fileAttributes.isRegularFile()) {
        return readPaintingImagesFromZip(path, pack);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(String.format(LOG_FAIL_IMAGES, path.getFileName()), e);
    }

    return new HashMap<>();
  }

  private static HashMap<CustomId, Image> readPaintingImagesFromDirectory(Path path, PackResource pack) {
    HashMap<CustomId, Image> images = new HashMap<>();

    if (Files.notExists(path)) {
      return images;
    }

    pack.paintings().forEach((painting) -> {
      CustomId id = new CustomId(pack.id(), painting.id());
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
        CustomPaintingsMod.LOGGER.warn(String.format(LOG_PAINTING_READ_FAIL, id), e);
      }
    });

    return images;
  }

  private static HashMap<CustomId, Image> readPaintingImagesFromZip(Path path, PackResource pack) {
    HashMap<CustomId, Image> images = new HashMap<>();

    String filename = path.getFileName().toString();
    if (!filename.endsWith(".zip")) {
      return images;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      return images;
    }

    try (ZipFile zip = new ZipFile(path.toFile())) {
      String folderPrefix = ResourceUtil.getFolderPrefix(zip);

      pack.paintings().forEach((painting) -> {
        CustomId id = new CustomId(pack.id(), painting.id());
        ZipEntry zipImage = ResourceUtil.getImageZipEntry(zip, folderPrefix, "images", painting.id() + ".png");
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
          CustomPaintingsMod.LOGGER.warn(String.format(LOG_PAINTING_READ_FAIL, id), e);
        }
      });
    } catch (IOException ignored) {
    }

    return images;
  }

  private static Path getIconPath(Path parent, String dirname) {
    Path path = parent.resolve(ICON_PNG);
    if (!Files.exists(path)) {
      path = parent.resolve(PACK_PNG);
      if (!Files.exists(path)) {
        CustomPaintingsMod.LOGGER.warn(LOG_NO_ICON, dirname);
        return null;
      } else {
        CustomPaintingsMod.LOGGER.warn(LOG_LEGACY_ICON, dirname);
      }
    }
    return path;
  }

  private record LoadResult(HashMap<String, PackData> packs, HashMap<CustomId, Image> images, int erroredOrSkipped) {
    public static LoadResult empty(int erroredOrSkipped) {
      return new LoadResult(new HashMap<>(0), new HashMap<>(0), erroredOrSkipped);
    }
  }
}
