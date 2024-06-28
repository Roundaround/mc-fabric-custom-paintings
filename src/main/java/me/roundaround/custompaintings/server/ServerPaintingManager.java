package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.VanillaDataManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

public class ServerPaintingManager extends PersistentState {
  private final MinecraftServer server;
  private final VanillaDataManager vanillaDataManager = VanillaDataManager.getInstance();
  private final HashMap<UUID, HashSet<PaintingData>> knownPaintings = new HashMap<>();
  private final HashMap<UUID, PaintingData> allPaintings = new HashMap<>();

  private ServerPaintingManager(MinecraftServer server) {
    this.server = server;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList nbtList = new NbtList();
    this.allPaintings.forEach((uuid, paintingData) -> {
      NbtCompound nbtCompound = paintingData.writeToNbt();
      nbtCompound.putUuid("PaintingUuid", uuid);
      nbtList.add(nbtCompound);
    });
    nbt.put("Paintings", nbtList);

    return nbt;
  }

  private static ServerPaintingManager fromNbt(MinecraftServer server, NbtCompound nbt) {
    ServerPaintingManager manager = new ServerPaintingManager(server);
    NbtList nbtList = nbt.getList("Paintings", NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound nbtCompound = nbtList.getCompound(i);
      manager.allPaintings.put(nbtCompound.getUuid("PaintingUuid"), PaintingData.fromNbt(nbtCompound));
    }
    return manager;
  }

  public static ServerPaintingManager getInstance(MinecraftServer server) {
    ServerWorld world = Objects.requireNonNull(server.getWorld(World.OVERWORLD));
    Type<ServerPaintingManager> persistentStateType = new PersistentState.Type<>(
        () -> new ServerPaintingManager(server), (nbt, registryLookup) -> fromNbt(server, nbt), null);
    return world.getPersistentStateManager().getOrCreate(persistentStateType, CustomPaintingsMod.MOD_ID);
  }

  public void init() {
    WorldDataLoadStats stats = this.loadDataFromWorld();
    CustomPaintingsMod.LOGGER.info(
        "Loaded data for every painting ({}) in {}ms", stats.paintingsFound(), stats.timeMs());

    this.vanillaDataManager.init();
  }

  public boolean setPaintingData(UUID uuid, PaintingData paintingData) {
    PaintingData previousData = this.allPaintings.put(uuid, paintingData);
    if (previousData != null && !previousData.equals(paintingData)) {
      this.setDirty(true);
      return true;
    }
    return false;
  }

  public boolean setPaintingDataAndPropagate(UUID uuid, PaintingData paintingData) {
    boolean stateChanged = this.setPaintingData(uuid, paintingData);
    this.setCustomDataOnPainting(uuid, paintingData);
    return stateChanged;
  }

  public boolean setPaintingDataAndPropagate(PaintingEntity painting, PaintingData paintingData) {
    boolean stateChanged = this.setPaintingData(painting.getUuid(), paintingData);
    this.setCustomDataOnPainting(painting, paintingData);
    return stateChanged;
  }

  public void setCustomDataOnPainting(UUID uuid, PaintingData paintingData) {
    this.findPainting(uuid).ifPresent((painting) -> this.setCustomDataOnPainting(painting, paintingData));
  }

  public void setCustomDataOnPainting(PaintingEntity painting, PaintingData paintingData) {
    if (!(painting instanceof ExpandedPaintingEntity expanded)) {
      return;
    }
    expanded.setCustomData(paintingData);
  }

  public Optional<PaintingEntity> findPainting(UUID uuid) {
    return StreamSupport.stream(this.server.getWorlds().spliterator(), false)
        .map((world) -> world.getEntity(uuid))
        .filter((entity) -> entity instanceof PaintingEntity)
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();
  }

  private WorldDataLoadStats loadDataFromWorld() {
    long startTime = Util.getMeasuringTimeMs();
    AtomicInteger paintingsWithStoredData = new AtomicInteger();
    AtomicInteger paintingsWithNoData = new AtomicInteger();
    AtomicInteger paintingsWithSelfData = new AtomicInteger();

    ArrayList<PaintingEntity> paintings = new ArrayList<>();

    this.server.getWorlds()
        .forEach((world) -> paintings.addAll(world.getEntitiesByType(EntityType.PAINTING, Entity::isAlive)));

    if (paintings.isEmpty()) {
      return new WorldDataLoadStats(Util.getMeasuringTimeMs() - startTime, 0, 0, 0, 0);
    }

    paintings.forEach((painting) -> {
      if (!(painting instanceof ExpandedPaintingEntity expanded)) {
        // TODO: Figure out a way around these bits. Theoretically it should ALWAYS be an instance of expanded
        return;
      }

      UUID uuid = painting.getUuid();

      if (this.allPaintings.containsKey(uuid)) {
        PaintingData storedData = this.allPaintings.get(uuid);
        if (!storedData.equals(expanded.getCustomData())) {
          this.setCustomDataOnPainting(painting, storedData);
        }
        paintingsWithStoredData.incrementAndGet();
        return;
      }

      PaintingData paintingData = expanded.getCustomData();

      if (paintingData == null || paintingData.isEmpty()) {
        this.setPaintingDataAndPropagate(painting, this.vanillaDataManager.get(painting.getVariant().value()));
        paintingsWithNoData.incrementAndGet();
        return;
      }

      paintingsWithSelfData.incrementAndGet();
      this.setPaintingData(uuid, expanded.getCustomData());
    });

    return new WorldDataLoadStats(Util.getMeasuringTimeMs() - startTime, paintings.size(),
        paintingsWithStoredData.get(), paintingsWithNoData.get(), paintingsWithSelfData.get()
    );
  }

  private record WorldDataLoadStats(long timeMs, int paintingsFound, int paintingsWithStoredData,
                                    int paintingsWithNoData, int paintingsWithSelfData) {
  }
}
