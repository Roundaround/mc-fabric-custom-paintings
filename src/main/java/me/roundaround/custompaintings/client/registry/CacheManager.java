package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CacheManager {
  private static CacheManager instance = null;

  private CacheManager() {
  }

  public static CacheManager getInstance() {
    if (instance == null) {
      instance = new CacheManager();
    }
    return instance;
  }

  public HashMap<Identifier, Image> loadFromFile(
      UUID serverId, HashSet<String> packIds, HashSet<Identifier> paintingIds
  ) {
    HashMap<Identifier, Image> images = new HashMap<>();
    Path cacheDir = this.getCacheDir();
    Path dataFile = this.getDataFile(cacheDir);

    if (Files.notExists(dataFile) || !Files.isRegularFile(dataFile)) {
      return images;
    }

    NbtCompound nbt;
    try {
      nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to load cache data");
      return images;
    }

    int version = nbt.contains("Version", NbtElement.NUMBER_TYPE) ? nbt.getInt("Version") : 1;
    NbtCompound data = nbt.contains("Data", NbtElement.COMPOUND_TYPE) ? nbt.getCompound("Data") : new NbtCompound();

    String serverIdStr = serverId.toString();
    if (!data.contains(serverIdStr, NbtElement.COMPOUND_TYPE)) {
      return images;
    }

    ServerCacheData server = this.parseServer(serverIdStr, data.getCompound(serverIdStr));
    HashMap<Identifier, String> requestedHashes = new HashMap<>();
    for (PackCacheData pack : server.packs.values()) {
      if (pack.packId.equals(PackIcons.ICON_NAMESPACE)) {
        Identifier iconId = PackIcons.identifier(pack.packId);
        String iconHash = pack.paintingHashes.get(iconId);
        if (iconHash != null && !iconHash.isBlank()) {
          requestedHashes.put(iconId, iconHash);
        }
      }

      if (packIds.contains(pack.packId)) {
        for (Identifier paintingId : paintingIds) {
          String paintingHash = pack.paintingHashes.get(paintingId);
          if (paintingHash != null && !paintingHash.isBlank()) {
            requestedHashes.put(paintingId, paintingHash);
          }
        }
      }
    }

    HashSet<String> usedHashes = new HashSet<>(requestedHashes.values());
    server.packs.values()
        .stream()
        .flatMap((pack) -> pack.paintingHashes.values().stream())
        .filter((hash) -> !usedHashes.contains(hash))
        .filter((hash) -> {
          // TODO: Check if the hash is used by any other server within the last X days and keep it if so.
          //   Until this is implemented, don't clear images from the cache.
          return false;
        })
        .forEach((hash) -> this.deleteImage(cacheDir, hash));

    requestedHashes.forEach((id, hash) -> {
      Image image = this.loadImage(cacheDir, hash);
      if (image == null || image.isEmpty()) {
        return;
      }
      images.put(id, image);
    });

    return images;
  }

  public void saveToFile(UUID serverId, HashMap<Identifier, Image> images) {
    Path cacheDir = this.getCacheDir();
    Path dataFile = this.getDataFile(cacheDir);

    HashMap<String, PackCacheData> packs = new HashMap<>();
    images.forEach((id, image) -> {

    });
  }

  private Path getCacheDir() {
    return FabricLoader.getInstance().getGameDir().resolve("data").resolve(CustomPaintingsMod.MOD_ID);
  }

  private Path getDataFile(Path cacheDir) {
    return cacheDir.resolve("data.dat");
  }

  private ServerCacheData parseServer(String serverId, NbtCompound nbt) {
    HashMap<String, PackCacheData> packs = new HashMap<>();
    for (String packId : nbt.getKeys()) {
      packs.put(packId, this.parsePack(packId, nbt.getCompound(packId)));
    }
    return new ServerCacheData(serverId, packs);
  }

  private PackCacheData parsePack(String packId, NbtCompound nbt) {
    HashMap<Identifier, String> paintingHashes = new HashMap<>();
    for (String paintingId : nbt.getKeys()) {
      paintingHashes.put(new Identifier(packId, paintingId), nbt.getString(paintingId));
    }
    return new PackCacheData(packId, paintingHashes);
  }

  private void deleteImage(Path cacheDir, String hash) {
    Path path = cacheDir.resolve(hash + ".png");
    if (Files.notExists(path) || !Files.isRegularFile(path)) {
      return;
    }

    try {
      Files.delete(path);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Failed to delete stale cached image {}.png", hash);
    }
  }

  private Image loadImage(Path cacheDir, String hash) {
    Path path = cacheDir.resolve(hash + ".png");
    if (Files.notExists(path) || !Files.isRegularFile(path)) {
      return Image.empty();
    }

    try {
      return Image.read(Files.newInputStream(path));
    } catch (IOException e) {
      return Image.empty();
    }
  }

  private record ServerCacheData(String serverId, HashMap<String, PackCacheData> packs) {
  }

  private record PackCacheData(String packId, HashMap<Identifier, String> paintingHashes) {
  }
}
