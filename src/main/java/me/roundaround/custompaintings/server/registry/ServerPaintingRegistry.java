package me.roundaround.custompaintings.server.registry;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.resource.file.FileUid;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.file.Pack;
import me.roundaround.custompaintings.resource.file.PackReader;
import me.roundaround.custompaintings.roundalib.util.PathAccessor;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

public class ServerPaintingRegistry extends CustomPaintingRegistry {
  private static final String LOG_FAIL_ALL = "Skipping loading packs due to an error";
  private static final String LOG_LARGE_IMAGE = "Image file for {} is too large, skipping";
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
    this.images.clear();
    this.images.putAll(images);
    this.combinedImageHash = images.isEmpty() ? CustomPaintingsMod.EMPTY_HASH : ResourceUtil.hashImages(images);
  }

  public void sendSummaryToAll() {
    ServerNetworking.sendSummaryPacketToAll(this.server, this.packsList, this.combinedImageHash,
        this.finishedMigrations, this.safeMode, this.loadErrorOrSkipCount);
  }

  public void sendSummaryToPlayer(ServerPlayerEntity player) {
    ServerNetworking.sendSummaryPacket(player, this.packsList, this.combinedImageHash, this.finishedMigrations,
        this.safeMode, this.loadErrorOrSkipCount);
  }

  public void checkPlayerHashes(ServerPlayerEntity player, Map<CustomId, String> hashes) {
    HashMap<CustomId, Image> images = new HashMap<>();
    this.images.forEach((id, image) -> {
      if (image.hash().equals(hashes.get(id))) {
        return;
      }
      images.put(id, image);
    });

    if (images.isEmpty()) {
      CustomPaintingsMod.LOGGER.info(
          "{} has incorrect combined hash, but all correct images. " +
              "This is likely due to paintings being removed server-side.",
          player.getName().getString());
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
    Path packsDir = PathAccessor.getInstance().getPerWorldModDir(Constants.MOD_ID);

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
        Metadata meta = PackReader.readMetadata(path);
        if (meta == null) {
          erroredOrSkipped.value++;
          return;
        }

        Pack pack = meta.pack();
        String packId = pack.id();
        String filename = path.getFileName().toString();

        String existingFilename = packFilenames.get(packId);
        if (existingFilename != null) {
          CustomPaintingsMod.LOGGER.warn(
              "Multiple packs with id \"{}\" detected. Only the first will be kept. Please make sure packs have " +
                  "unique IDs!\nKeeping \"{}\" and discarding \"{}\"",
              packId, existingFilename, filename);
          erroredOrSkipped.value++;
          return;
        }

        packFilenames.put(packId, filename);

        FileUid fileUid = meta.fileUid();
        boolean disabled = disabledPacks.contains(fileUid.stringValue());
        packs.put(packId, pack.toData(fileUid, disabled));

        if (meta.icon() != null) {
          images.put(PackIcons.customId(packId), meta.icon());
        }
        if (!disabled) {
          HashMap<CustomId, Image> paintingImages = PackReader.readPaintingImages(meta);
          for (Map.Entry<CustomId, Image> entry : paintingImages.entrySet()) {
            CustomId id = entry.getKey();
            Image image = entry.getValue();
            long size = (long) image.width() * image.height();
            if (size > MAX_SIZE) {
              CustomPaintingsMod.LOGGER.warn(LOG_LARGE_IMAGE, id);
              continue;
            }

            images.put(id, image);
          }
        }
      });

      CustomPaintingsMod.LOGGER.info("Loaded {} pack(s) with {} painting(s)", packs.size(),
          packs.values().stream().mapToInt((pack) -> pack.paintings().size()).sum());
      return new LoadResult(packs, images, erroredOrSkipped.value);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(LOG_FAIL_ALL, e);
      return LoadResult.empty(1);
    }
  }

  private record LoadResult(HashMap<String, PackData> packs, HashMap<CustomId, Image> images, int erroredOrSkipped) {
    public static LoadResult empty(int erroredOrSkipped) {
      return new LoadResult(new HashMap<>(0), new HashMap<>(0), erroredOrSkipped);
    }
  }
}
