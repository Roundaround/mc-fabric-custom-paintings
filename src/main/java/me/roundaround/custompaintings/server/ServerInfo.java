package me.roundaround.custompaintings.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.generated.Constants;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerInfo {
  private static ServerInfo instance = null;

  private final Path savePath;
  private final UUID serverId;
  private final HashSet<String> disabledPacks = new HashSet<>();

  private boolean dirty;

  private ServerInfo(Path savePath) {
    this(savePath, UUID.randomUUID(), Set.of(), true);
  }

  private ServerInfo(Path savePath, UUID serverId, Set<String> disabledPacks) {
    this(savePath, serverId, disabledPacks, false);
  }

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
        CustomPaintingsMod.LOGGER.warn("Failed to save Custom Paintings mod server info:", e);
      }
    });

    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      this.clear();
    });
  }

  public static void init(LevelStorage.Session session) {
    Path savePath = session.getWorldDirectory(World.OVERWORLD)
        .resolve("data")
        .resolve(Constants.MOD_ID + "_server" + ".dat");

    try {
      load(savePath).ifSuccess((data) -> {
        instance = new ServerInfo(savePath, data.serverId(), data.disabledPacks());
      }).ifError((e) -> {
        CustomPaintingsMod.LOGGER.warn("Failed to load Custom Paintings mod server info; setting defaults: {}", e);
        instance = new ServerInfo(savePath);
      });
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn("Failed to load Custom Paintings mod server info; setting defaults:", e);
      instance = new ServerInfo(savePath);
    }
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

    NbtCompound nbt = (NbtCompound) StoredData.CODEC.encodeStart(
        NbtOps.INSTANCE,
        new StoredData(this.serverId, this.disabledPacks)
    ).getOrThrow();
    NbtIo.writeCompressed(nbt, this.savePath);
    this.dirty = false;
  }

  private static DataResult<StoredData> load(Path savePath) throws IOException {
    if (Files.notExists(savePath)) {
      return DataResult.error(() -> "No file to load");
    }
    return StoredData.CODEC.parse(NbtOps.INSTANCE, NbtIo.readCompressed(savePath, NbtSizeTracker.ofUnlimitedBytes()));
  }

  private record StoredData(UUID serverId, Set<String> disabledPacks) {
    public static final Codec<StoredData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        Uuids.INT_STREAM_CODEC.fieldOf("ServerId").forGetter(StoredData::serverId),
        Codec.list(Codec.STRING)
            .xmap(Set::copyOf, List::copyOf)
            .fieldOf("DisabledPacks")
            .forGetter(StoredData::disabledPacks)
    ).apply(instance, StoredData::new));
  }
}
