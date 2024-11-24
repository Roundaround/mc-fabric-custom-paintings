package me.roundaround.custompaintings.server;

import com.google.common.collect.Streams;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerPaintingManager extends PersistentState {
  private static final String NBT_PAINTINGS = "Paintings";
  private static final String NBT_PAINTING_UUID = "PaintingUuid";

  private final ServerWorld world;
  private final HashMap<UUID, PaintingData> allPaintings = new HashMap<>();
  private final HashMap<UUID, Integer> networkIds = new HashMap<>();

  private ServerPaintingManager(ServerWorld world) {
    this.world = world;
  }

  public static void init(ServerWorld world) {
    // Just getting the instance also creates/initializes it
    getInstance(world);
  }

  public static ServerPaintingManager getInstance(MinecraftServer server) {
    ServerWorld world;
    if (server == null || (world = server.getWorld(World.OVERWORLD)) == null) {
      IllegalStateException exception = new IllegalStateException(
          "Trying to get a ServerPaintingManager instance when server is not running");
      CustomPaintingsMod.LOGGER.error(exception);
      throw exception;
    }
    return getInstance(world);
  }

  public static ServerPaintingManager getInstance(ServerWorld world) {
    Type<ServerPaintingManager> persistentStateType = new PersistentState.Type<>(() -> new ServerPaintingManager(world),
        (nbt, registryLookup) -> fromNbt(world, nbt), null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, CustomPaintingsMod.MOD_ID);
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList nbtList = new NbtList();
    this.allPaintings.forEach((uuid, paintingData) -> {
      NbtCompound nbtCompound = paintingData.write();
      nbtCompound.putUuid(NBT_PAINTING_UUID, uuid);
      nbtList.add(nbtCompound);
    });
    nbt.put(NBT_PAINTINGS, nbtList);

    return nbt;
  }

  public void onEntityLoad(PaintingEntity painting) {
    this.loadPainting(painting);
    this.fixCustomName(painting);

    ServerNetworking.sendSetPaintingPacketToAll(this.world,
        PaintingAssignment.from(painting.getId(), painting.getCustomData(),
            ServerPaintingRegistry.getInstance()::contains
        )
    );
  }

  public void onEntityUnload(PaintingEntity painting) {
    Entity.RemovalReason removalReason = painting.getRemovalReason();
    if (removalReason == null || !removalReason.shouldDestroy()) {
      return;
    }
    this.remove(painting.getUuid());
  }

  public void syncAllDataForPlayer(ServerPlayerEntity player) {
    if (player.getServerWorld() != this.world) {
      return;
    }
    Function<CustomId, Boolean> lookup = ServerPaintingRegistry.getInstance()::contains;
    List<PaintingAssignment> assignments = this.allPaintings.entrySet()
        .stream()
        .filter((entry) -> this.networkIds.containsKey(entry.getKey()))
        .map((entry) -> {
          UUID id = entry.getKey();
          PaintingData data = entry.getValue();
          return PaintingAssignment.from(this.networkIds.get(id), data, lookup);
        })
        .toList();
    ServerNetworking.sendSyncAllDataPacket(player, assignments);
  }

  public void setPaintingData(PaintingEntity painting, PaintingData data) {
    painting.setCustomData(data);
    if (this.setTrackedData(painting.getUuid(), painting.getId(), data)) {
      ServerNetworking.sendSetPaintingPacketToAll(this.world,
          PaintingAssignment.from(painting.getId(), data, ServerPaintingRegistry.getInstance()::contains)
      );
    }
  }

  private void loadPainting(PaintingEntity painting) {
    UUID uuid = painting.getUuid();

    if (this.allPaintings.containsKey(uuid)) {
      this.setPaintingData(painting, this.allPaintings.get(uuid));
      return;
    }

    PaintingData paintingData = painting.getCustomData();
    if (paintingData == null || paintingData.isEmpty()) {
      this.setPaintingData(painting, new PaintingData(painting.getVariant().value()));
      return;
    }

    this.setTrackedData(uuid, painting.getId(), paintingData);
  }

  /**
   * Older versions of the mod forced assigning the label to the painting's custom name and force enabling the custom
   * name to be visible. Now that we have enabled pulling the label directly (and not relying on custom name) and
   * toggling label visibility with interaction, we don't need this hacky implementation. For any existing paintings
   * in the world, we want to try to detect when we might have set these parameters and remove them.
   */
  private void fixCustomName(PaintingEntity painting) {
    PaintingData paintingData = painting.getCustomData();
    Text customName = painting.getCustomName();

    if (paintingData.isEmpty() || !painting.isCustomNameVisible() || customName == null) {
      return;
    }

    if (painting.isCustomNameVisible() && customName.getString().isBlank()) {
      painting.setCustomName(null);
      return;
    }

    if (!paintingData.hasLabel()) {
      return;
    }

    List<String> potentialOldLabels = List.of(paintingData.name(), paintingData.artist(), paintingData.id().resource());
    String customNameContent = customName.getString();
    if (potentialOldLabels.stream().anyMatch(customNameContent::startsWith)) {
      painting.setCustomName(null);
      painting.setCustomNameVisible(false);
    }
  }

  private void remove(UUID uuid) {
    this.networkIds.remove(uuid);
    if (this.allPaintings.remove(uuid) != null) {
      this.markDirty();
    }
  }

  private boolean setTrackedData(UUID paintingUuid, Integer paintingId, PaintingData data) {
    if (paintingId != null) {
      this.networkIds.put(paintingUuid, paintingId);
    }

    PaintingData previousData = this.allPaintings.put(paintingUuid, data);
    if (previousData == null || !previousData.equals(data)) {
      this.markDirty();
      return true;
    }

    return false;
  }

  private static ServerPaintingManager fromNbt(ServerWorld world, NbtCompound nbt) {
    ServerPaintingManager manager = new ServerPaintingManager(world);

    NbtList nbtList = nbt.getList(NBT_PAINTINGS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound nbtCompound = nbtList.getCompound(i);
      manager.allPaintings.put(nbtCompound.getUuid(NBT_PAINTING_UUID), PaintingData.read(nbtCompound));
    }
    return manager;
  }

  public static void syncAllDataForAllPlayers(MinecraftServer server) {
    server.getPlayerManager()
        .getPlayerList()
        .forEach((player) -> getInstance(player.getServerWorld()).syncAllDataForPlayer(player));
  }

  public static void runMigration(ServerPlayerEntity sourcePlayer, CustomId migrationId) {
    boolean succeeded = tryRunMigration(sourcePlayer, migrationId);
    ServerPaintingRegistry.getInstance().markMigrationFinished(migrationId, succeeded);

    if (sourcePlayer == null) {
      return;
    }

    MinecraftServer server = sourcePlayer.getServer();
    if (server == null || !server.isRunning()) {
      return;
    }

    ServerNetworking.sendMigrationFinishPacketToAll(server, migrationId, succeeded);
  }

  private static boolean tryRunMigration(ServerPlayerEntity sourcePlayer, CustomId migrationId) {
    if (sourcePlayer == null || !sourcePlayer.hasPermissionLevel(3) || migrationId == null) {
      return false;
    }

    MinecraftServer server = sourcePlayer.getServer();
    if (server == null || !server.isRunning()) {
      return false;
    }

    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();
    MigrationData migration = registry.getMigration(migrationId);
    if (migration == null) {
      return false;
    }

    var changed = new Object() {
      boolean value = false;
    };

    server.getWorlds().forEach((world) -> {
      ServerPaintingManager manager = ServerPaintingManager.getInstance(world);
      manager.allPaintings.forEach((paintingUuid, currentData) -> {
        CustomId id = currentData.id();
        ArrayDeque<CustomId> ids = new ArrayDeque<>(List.of(id));
        migration.pairs().forEach((from, to) -> {
          if (Objects.equals(ids.peek(), from)) {
            ids.push(to);
          }
        });

        Iterator<CustomId> itr = ids.iterator();
        while (itr.hasNext() && !registry.contains(itr.next())) {
          itr.remove();
        }

        CustomId targetId = ids.peek();
        if (id.equals(targetId)) {
          return;
        }

        PaintingData data = registry.get(targetId);
        Integer paintingId = null;
        if (world.getEntity(paintingUuid) instanceof PaintingEntity painting) {
          paintingId = painting.getId();
          painting.setCustomData(data);
        }

        changed.value = changed.value || manager.setTrackedData(paintingUuid, paintingId, data);
      });
    });

    if (changed.value) {
      syncAllDataForAllPlayers(server);
    }
    return true;
  }

  public Set<UUID> getUnknownPaintings() {
    return this.getUnknownPaintings(null);
  }

  public Set<UUID> getUnknownPaintings(CustomId dataId) {
    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();
    return this.allPaintings.entrySet().stream().filter((entry) -> {
      CustomId id = entry.getValue().id();
      return !registry.contains(id) && (dataId == null || dataId.equals(id));
    }).map(Map.Entry::getKey).collect(Collectors.toSet());
  }

  public Map<CustomId, Integer> getUnknownPaintingCounts() {
    HashMap<CustomId, Integer> counts = new HashMap<>();
    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();

    this.allPaintings.forEach((uuid, data) -> {
      CustomId id = data.id();
      if (registry.contains(id)) {
        return;
      }
      counts.put(id, counts.getOrDefault(id, 0) + 1);
    });

    return counts;
  }

  public int fixUnknownPaintings(CustomId dataId, CustomId targetId) {
    PaintingData targetData = ServerPaintingRegistry.getInstance().get(targetId);

    if (targetData == null || targetData.isEmpty()) {
      // TODO: Throw illegal argument exception?
      return 0;
    }

    Set<UUID> needsFixed = this.getUnknownPaintings(dataId);
    var changed = new Object() {
      int count = 0;
    };

    needsFixed.forEach((uuid) -> {
      Integer paintingId = null;

      if (this.world.getEntity(uuid) instanceof PaintingEntity painting) {
        paintingId = painting.getId();
        painting.setCustomData(targetData);
      }

      if (this.setTrackedData(uuid, paintingId, targetData)) {
        changed.count++;
      }
    });

    return changed.count;
  }

  public Set<MismatchedReference> getMismatchedPaintings() {
    return this.getMismatchedPaintings(PaintingData.MismatchedCategory.EVERYTHING);
  }

  public Set<MismatchedReference> getMismatchedPaintings(PaintingData.MismatchedCategory category) {
    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();
    return this.allPaintings.entrySet().stream().map((entry) -> {
      PaintingData currentData = entry.getValue();
      PaintingData knownData = registry.get(currentData.id());
      if (knownData == null || knownData.isEmpty() || !currentData.isMismatched(knownData, category)) {
        return null;
      }
      return new MismatchedReference(entry.getKey(), knownData);
    }).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  public int fixMismatchedPaintings(PaintingData.MismatchedCategory category) {
    Set<MismatchedReference> needsFixed = this.getMismatchedPaintings(category);
    var changed = new Object() {
      int count = 0;
    };

    needsFixed.forEach((ref) -> {
      UUID uuid = ref.uuid();
      PaintingData knownData = ref.knownData();
      Integer paintingId = null;

      if (this.world.getEntity(uuid) instanceof PaintingEntity painting) {
        paintingId = painting.getId();
        painting.setCustomData(knownData);
      }

      if (this.setTrackedData(uuid, paintingId, knownData)) {
        changed.count++;
      }
    });

    return changed.count;
  }

  public static Set<UUID> getUnknownPaintings(MinecraftServer server, CustomId dataId) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> getInstance(world).getUnknownPaintings(dataId).stream())
        .collect(Collectors.toSet());
  }

  public static Map<CustomId, Integer> getUnknownPaintingCounts(MinecraftServer server) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> getInstance(world).getUnknownPaintingCounts().entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static int fixUnknownPaintings(MinecraftServer server, CustomId dataId, CustomId targetId) {
    return Streams.stream(server.getWorlds())
        .mapToInt((world) -> getInstance(world).fixUnknownPaintings(dataId, targetId))
        .sum();
  }

  public static Set<MismatchedReference> getMismatchedPaintings(MinecraftServer server) {
    return getMismatchedPaintings(server, PaintingData.MismatchedCategory.EVERYTHING);
  }

  public static Set<MismatchedReference> getMismatchedPaintings(
      MinecraftServer server, PaintingData.MismatchedCategory category
  ) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> getInstance(world).getMismatchedPaintings(category).stream())
        .collect(Collectors.toSet());
  }

  public static int fixMismatchedPaintings(MinecraftServer server, PaintingData.MismatchedCategory category) {
    return Streams.stream(server.getWorlds())
        .mapToInt((world) -> getInstance(world).fixMismatchedPaintings(category))
        .sum();
  }

  public record MismatchedReference(UUID uuid, PaintingData knownData) {
    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof MismatchedReference that))
        return false;
      return Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.uuid);
    }
  }
}
