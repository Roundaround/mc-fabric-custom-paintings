package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CacheManager {
  private CacheManager() {
  }

  public static CacheManager getInstance(MinecraftClient client) {
    return null;
  }

  public void loadFromFile(UUID serverId, Collection<String> packIds, Collection<Identifier> paintingIds) {
    HashSet<Identifier> ids = Stream.concat(packIds.stream().map(PackIcons::identifier), paintingIds.stream())
        .collect(Collectors.toCollection(HashSet::new));

    Path cacheDir = FabricLoader.getInstance()
        .getGameDir()
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID);
    Path dataFile = cacheDir.resolve("data.dat");

    if (Files.notExists(dataFile) || !Files.isRegularFile(dataFile)) {
      return;
    }

    NbtCompound nbt;
    try {
      nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to load cache data");
      return;
    }

    

    HashMap<Identifier, Image> globalCache = this.useGlobalCache() ? this.loadCache("global", ids) : new HashMap<>();
    HashMap<Identifier, Image> perServerCache = this.loadCache(serverId.toString(), ids);
  }

  private boolean useGlobalCache() {
    return CustomPaintingsConfig.getInstance().useGlobalCache.getValue();
  }

  private HashMap<Identifier, Image> loadCache(String directory, HashSet<Identifier> ids) {
    Path cacheDir = FabricLoader.getInstance()
        .getGameDir()
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID)
        .resolve(directory);

    HashMap<Identifier, Image> cacheMap = new HashMap<>();
    if (Files.notExists(cacheDir)) {
      return cacheMap;
    }

    for (Identifier id : ids) {
      cacheMap.put(id, this.loadImage(id, cacheDir));
    }

    return cacheMap;
  }

  private Image loadImage(Identifier id, Path cacheDir) {
    Path path = cacheDir.resolve(id.getNamespace()).resolve(id.getPath() + ".png");
    if (Files.notExists(path) || !Files.isRegularFile(path)) {
      return Image.empty();
    }

    try {
      return Image.read(Files.newInputStream(path));
    } catch (IOException e) {
      return Image.empty();
    }
  }
}
