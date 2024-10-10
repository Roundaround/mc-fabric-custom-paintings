package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.Image;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CacheManager {
  private static CacheManager instance = null;

  private UUID serverId = null;

  private CacheManager() {
  }

  public static CacheManager getInstance() {
    if (instance == null) {
      instance = new CacheManager();
    }
    return instance;
  }

  public CacheRead loadFromFile(
      UUID serverId, HashSet<String> packIds, HashSet<Identifier> paintingIds
  ) {
    this.serverId = serverId;
    Path cacheDir = this.getCacheDir();
    Path dataFile = this.getDataFile(cacheDir);

    if (Files.notExists(dataFile) || !Files.isRegularFile(dataFile)) {
      return null;
    }

    NbtCompound nbt;
    try {
      nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to load cache data");
      return null;
    }

    CacheData data = CacheData.fromNbt(nbt);
    if (!data.byServer.containsKey(this.serverId)) {
      return null;
    }

    ServerCacheData server = data.byServer.get(this.serverId);
    HashMap<Identifier, String> requestedHashes = new HashMap<>();
    for (PackCacheData pack : server.packs) {
      if (packIds.contains(pack.packId)) {
        for (ImageCacheData image : pack.images) {
          if (paintingIds.contains(image.id)) {
            requestedHashes.put(image.id, image.hash);
          }
        }
      }
    }

    HashSet<String> usedHashes = new HashSet<>(requestedHashes.values());
    server.packs.stream()
        .flatMap((pack) -> pack.images.stream().map(ImageCacheData::hash))
        .filter((hash) -> !usedHashes.contains(hash))
        .filter((hash) -> {
          // TODO: Check if the hash is used by any other server within the last X days and keep it if so.
          //   Until this is implemented, don't clear images from the cache.
          return false;
        })
        .forEach((hash) -> this.deleteImage(cacheDir, hash));

    HashMap<Identifier, Image> images = new HashMap<>();
    requestedHashes.forEach((id, hash) -> {
      Image image = this.loadImage(cacheDir, hash);
      if (image == null || image.isEmpty()) {
        return;
      }
      images.put(id, image);
    });

    return new CacheRead(images, requestedHashes, server.combinedHash);
  }

  public void saveToFile(HashMap<Identifier, Image> images, String combinedHash) throws IOException {
    if (this.serverId == null) {
      return;
    }

    Path cacheDir = this.getCacheDir();
    if (Files.notExists(cacheDir)) {
      Files.createDirectories(cacheDir);
    }

    Path dataFile = this.getDataFile(cacheDir);
    HashMap<String, PackCacheData> packs = new HashMap<>();

    images.forEach((id, image) -> {
      try {
        String packId = id.getNamespace();
        PackCacheData pack = packs.computeIfAbsent(packId, (k) -> PackCacheData.empty(packId));

        String hash = image.getHash();
        ImageIO.write(image.toBufferedImage(), "png", cacheDir.resolve(hash + ".png").toFile());
        pack.images.add(new ImageCacheData(id, hash, System.currentTimeMillis()));
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn("Failed to save image to cache: {}", id);
      }
    });

    NbtCompound nbt;
    if (Files.notExists(dataFile)) {
      nbt = new NbtCompound();
    } else {
      try {
        nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Failed to read existing cache data before writing");
        nbt = new NbtCompound();
      }
    }

    CacheData data = CacheData.fromNbt(nbt);
    data.byServer.put(this.serverId, new ServerCacheData(this.serverId, combinedHash, new ArrayList<>(packs.values())));

    NbtIo.writeCompressed(data.toNbt(), dataFile);
  }

  private Path getCacheDir() {
    return FabricLoader.getInstance().getGameDir().resolve("data").resolve(CustomPaintingsMod.MOD_ID);
  }

  private Path getDataFile(Path cacheDir) {
    return cacheDir.resolve("data.dat");
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

  private record CacheData(int version, HashMap<UUID, ServerCacheData> byServer) {
    private static final String NBT_VERSION = "Version";
    private static final String NBT_BY_SERVER = "ByServer";

    public static CacheData fromNbt(NbtCompound nbt) {
      int version = nbt.contains(NBT_VERSION, NbtElement.INT_TYPE) ? nbt.getInt(NBT_VERSION) : 1;
      NbtCompound serversNbt = nbt.contains(NBT_BY_SERVER, NbtElement.COMPOUND_TYPE) ?
          nbt.getCompound(NBT_BY_SERVER) :
          new NbtCompound();
      HashMap<UUID, ServerCacheData> byServer = new HashMap<>();
      for (String key : serversNbt.getKeys()) {
        UUID serverId = UUID.fromString(key);
        byServer.put(serverId, ServerCacheData.fromNbt(serversNbt.getCompound(key)));
      }
      return new CacheData(version, byServer);
    }

    public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      nbt.putInt(NBT_VERSION, this.version);
      NbtCompound serversNbt = new NbtCompound();
      this.byServer.forEach((serverId, server) -> serversNbt.put(serverId.toString(), server.toNbt()));
      nbt.put(NBT_BY_SERVER, serversNbt);
      return nbt;
    }
  }

  private record ServerCacheData(UUID serverId, String combinedHash, ArrayList<PackCacheData> packs) {
    private static final String NBT_ID = "Id";
    private static final String NBT_COMBINED_HASH = "CombinedHash";
    private static final String NBT_PACKS = "Packs";

    public static ServerCacheData fromNbt(NbtCompound nbt) {
      UUID serverId = nbt.getUuid(NBT_ID);
      String combinedHash = nbt.getString(NBT_COMBINED_HASH);
      ArrayList<PackCacheData> packs = new ArrayList<>();
      NbtList list = nbt.getList(NBT_PACKS, NbtElement.COMPOUND_TYPE);
      int size = list.size();
      for (int i = 0; i < size; i++) {
        packs.add(PackCacheData.fromNbt(list.getCompound(i)));
      }
      return new ServerCacheData(serverId, combinedHash, packs);
    }

    public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      nbt.putUuid(NBT_ID, this.serverId);
      nbt.putString(NBT_COMBINED_HASH, this.combinedHash);
      NbtList list = new NbtList();
      this.packs.forEach((pack) -> list.add(pack.toNbt()));
      nbt.put(NBT_PACKS, list);
      return nbt;
    }
  }

  private record PackCacheData(String packId, ArrayList<ImageCacheData> images) {
    private static final String NBT_ID = "Id";
    private static final String NBT_IMAGES = "Images";

    public static PackCacheData empty(String packId) {
      return new PackCacheData(packId, new ArrayList<>());
    }

    public static PackCacheData fromNbt(NbtCompound nbt) {
      String packId = nbt.getString(NBT_ID);
      ArrayList<ImageCacheData> images = new ArrayList<>();
      NbtList list = nbt.getList(NBT_IMAGES, NbtElement.COMPOUND_TYPE);
      int size = list.size();
      for (int i = 0; i < size; i++) {
        images.add(ImageCacheData.fromNbt(list.getCompound(i), packId));
      }
      return new PackCacheData(packId, images);
    }

    public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      nbt.putString(NBT_ID, this.packId);
      NbtList list = new NbtList();
      this.images.forEach((image) -> list.add(image.toNbt()));
      nbt.put(NBT_IMAGES, list);
      return nbt;
    }
  }

  private record ImageCacheData(Identifier id, String hash, long lastAccess) {
    private static final String NBT_ID = "Id";
    private static final String NBT_HASH = "Hash";
    private static final String NBT_LAST_ACCESS = "LastAccess";

    public static ImageCacheData fromNbt(NbtCompound nbt, String packId) {
      return new ImageCacheData(
          new Identifier(packId, nbt.getString(NBT_ID)), nbt.getString(NBT_HASH), nbt.getLong(NBT_LAST_ACCESS));
    }

    public NbtCompound toNbt() {
      NbtCompound nbt = new NbtCompound();
      nbt.putString(NBT_ID, this.id.getPath());
      nbt.putString(NBT_HASH, this.hash);
      nbt.putLong(NBT_LAST_ACCESS, this.lastAccess);
      return nbt;
    }
  }

  public record CacheRead(HashMap<Identifier, Image> images, HashMap<Identifier, String> hashes, String combinedHash) {
  }
}
