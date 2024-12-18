package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerInfo {
  private static final String NBT_SERVER_ID = "ServerId";
  private static final String NBT_DISABLED_PACKS = "DisabledPacks";

  private static ServerInfo instance = null;

  private final Path savePath;
  private final UUID serverId;
  private final HashSet<String> disabledPacks = new HashSet<>();

  private boolean dirty;

  private ServerInfo(Path savePath, UUID serverId, Set<String> disabledPacks, boolean initiallyDirty) {
    this.savePath = savePath;
    this.serverId = serverId;
    this.disabledPacks.addAll(disabledPacks);
    this.dirty = initiallyDirty;

    ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
      try {
        this.save();
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn("Failed to save Custom Paintings mod server info");
      }
    });

    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      this.clear();
    });
  }

  public static void init(LevelStorage.Session session) {
    Path savePath = session.getWorldDirectory(World.OVERWORLD)
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID + "_server" + ".dat");

    InitialData data;
    try {
      data = load(savePath);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Failed to load Custom Paintings mod server info; setting defaults");
      data = InitialData.defaultValue();
    }

    instance = new ServerInfo(savePath, data.serverId(), data.disabledPacks(), data.dirty());
  }

  public static ServerInfo getInstance() {
    assert instance != null;
    return instance;
  }

  public UUID getServerId() {
    return this.serverId;
  }

  public Set<String> getDisabledPacks() {
    return Set.copyOf(this.disabledPacks);
  }

  public boolean markPackDisabled(String packFileUid) {
    if (this.disabledPacks.add(packFileUid)) {
      this.markDirty();
      return true;
    }
    return false;
  }

  public boolean markPackEnabled(String packFileUid) {
    if (this.disabledPacks.remove(packFileUid)) {
      this.markDirty();
      return true;
    }
    return false;
  }

  private void markDirty() {
    this.dirty = true;
  }

  private void clear() {
    instance = null;
  }

  private void save() throws IOException {
    if (!this.dirty) {
      return;
    }

    NbtCompound nbt = new NbtCompound();
    nbt.putUuid(NBT_SERVER_ID, this.serverId);
    NbtList list = new NbtList();
    for (String disabledPack : this.disabledPacks) {
      list.add(NbtString.of(disabledPack));
    }
    nbt.put(NBT_DISABLED_PACKS, list);

    NbtIo.writeCompressed(nbt, this.savePath);
    this.dirty = false;
  }

  private static InitialData load(Path savePath) throws IOException {
    NbtCompound nbt = Files.exists(savePath) ?
        NbtIo.readCompressed(savePath, NbtSizeTracker.ofUnlimitedBytes()) :
        new NbtCompound();

    UUID serverId = nbt.containsUuid(NBT_SERVER_ID) ? nbt.getUuid(NBT_SERVER_ID) : UUID.randomUUID();

    HashSet<String> disabledPacks = new HashSet<>();
    NbtList list = nbt.getList(NBT_DISABLED_PACKS, NbtElement.STRING_TYPE);
    int size = list.size();
    for (int i = 0; i < size; i++) {
      disabledPacks.add(list.getString(i));
    }

    return new InitialData(serverId, disabledPacks, !nbt.containsUuid(NBT_SERVER_ID));
  }

  private record InitialData(UUID serverId, Set<String> disabledPacks, boolean dirty) {
    public static InitialData defaultValue() {
      return new InitialData(UUID.randomUUID(), Set.of(), true);
    }
  }
}
