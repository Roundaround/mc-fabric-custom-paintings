package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Function;

public class ServerPaintingManager extends PersistentState {
  private static final String NBT_SERVER_ID = "ServerId";
  private static final String NBT_DISABLED_PACKS = "DisabledPacks";
  private static final String NBT_PAINTINGS = "Paintings";
  private static final String NBT_PAINTING_UUID = "PaintingUuid";

  private final ServerWorld world;
  private final UUID serverId;
  private final HashMap<UUID, PaintingData> allPaintings = new HashMap<>();
  private final HashMap<UUID, Integer> networkIds = new HashMap<>();
  private final HashSet<String> disabledPacks = new HashSet<>();

  private ServerPaintingManager(ServerWorld world) {
    this(world, CustomPaintingsMod.getOrGenerateServerId());
    this.markDirty();
  }

  private ServerPaintingManager(ServerWorld world, UUID serverId) {
    this.world = world;
    this.serverId = serverId;

    ServerEntityEvents.ENTITY_LOAD.register((entity, loadedWorld) -> {
      if (loadedWorld != this.world || !(entity instanceof PaintingEntity painting)) {
        return;
      }
      this.loadPainting(painting);
      this.fixCustomName(painting);

      ServerNetworking.sendSetPaintingPacketToAll(loadedWorld.getServer(),
          PaintingAssignment.from(painting.getId(), painting.getCustomData(),
              ServerPaintingRegistry.getInstance()::contains
          )
      );
    });
  }

  public static void init(ServerWorld world) {
    // Just getting the instance also creates/initializes it
    getInstance(world);
  }

  public static ServerPaintingManager getInstance(MinecraftServer server) {
    ServerWorld world = server.getWorld(World.OVERWORLD);
    if (world == null) {
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
    nbt.putUuid(NBT_SERVER_ID, this.serverId);

    if (!this.disabledPacks.isEmpty()) {
      NbtList disabledPacksList = new NbtList();
      this.disabledPacks.forEach((pack) -> disabledPacksList.add(NbtString.of(pack)));
      nbt.put(NBT_DISABLED_PACKS, disabledPacksList);
    }

    NbtList nbtList = new NbtList();
    this.allPaintings.forEach((uuid, paintingData) -> {
      NbtCompound nbtCompound = paintingData.write();
      nbtCompound.putUuid(NBT_PAINTING_UUID, uuid);
      nbtList.add(nbtCompound);
    });
    nbt.put(NBT_PAINTINGS, nbtList);

    return nbt;
  }

  private static ServerPaintingManager fromNbt(ServerWorld world, NbtCompound nbt) {
    ServerPaintingManager manager = nbt.containsUuid(NBT_SERVER_ID) ?
        new ServerPaintingManager(world, nbt.getUuid(NBT_SERVER_ID)) :
        new ServerPaintingManager(world);

    NbtList disabledPacksList = nbt.getList(NBT_DISABLED_PACKS, NbtElement.STRING_TYPE);
    for (int i = 0; i < disabledPacksList.size(); i++) {
      manager.disabledPacks.add(disabledPacksList.getString(i));
    }

    NbtList nbtList = nbt.getList(NBT_PAINTINGS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound nbtCompound = nbtList.getCompound(i);
      manager.allPaintings.put(nbtCompound.getUuid(NBT_PAINTING_UUID), PaintingData.read(nbtCompound));
    }
    return manager;
  }

  public UUID getServerId() {
    return this.serverId;
  }

  public void syncAllDataForPlayer(ServerPlayerEntity player) {
    if (player.getServerWorld() != this.world) {
      return;
    }
    Function<Identifier, Boolean> lookup = ServerPaintingRegistry.getInstance()::contains;
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

  public void remove(UUID uuid) {
    this.allPaintings.remove(uuid);
    this.networkIds.remove(uuid);
    this.markDirty();
  }

  public void setPaintingData(PaintingEntity painting, PaintingData data) {
    painting.setCustomData(data);
    if (this.setTrackedData(painting.getUuid(), painting.getId(), data)) {
      ServerNetworking.sendSetPaintingPacketToAll(this.world.getServer(),
          PaintingAssignment.from(painting.getId(), data, ServerPaintingRegistry.getInstance()::contains)
      );
    }
  }

  public void markPackDisabled(String packFileUid) {
    if (this.disabledPacks.add(packFileUid)) {
      this.markDirty();
    }
  }

  public void markPackEnabled(String packFileUid) {
    if (this.disabledPacks.remove(packFileUid)) {
      this.markDirty();
    }
  }

  public Set<String> getDisabledPacks() {
    return Set.copyOf(this.disabledPacks);
  }

  private void loadPainting(PaintingEntity painting) {
    UUID uuid = painting.getUuid();

    if (this.allPaintings.containsKey(uuid)) {
      this.setPaintingData(painting, this.allPaintings.get(uuid));
      return;
    }

    PaintingData paintingData = painting.getCustomData();
    if (paintingData == null || paintingData.isEmpty()) {
      this.setPaintingData(painting, VanillaPaintingRegistry.getInstance().get(painting.getVariant().value()));
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

    List<String> potentialOldLabels = List.of(paintingData.name(), paintingData.artist(), paintingData.id().getPath());
    String customNameContent = customName.getString();
    if (potentialOldLabels.stream().anyMatch(customNameContent::startsWith)) {
      painting.setCustomName(null);
      painting.setCustomNameVisible(false);
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

  private static PaintingData dataOrUnknown(PaintingData data) {
    if (data == null || data.unknown()) {
      return data;
    }
    if (!ServerPaintingRegistry.getInstance().contains(data.id())) {
      return data.markUnknown();
    }
    return data;
  }

  public static void syncAllDataForAllPlayers(MinecraftServer server) {
    server.getPlayerManager()
        .getPlayerList()
        .forEach((player) -> getInstance(player.getServerWorld()).syncAllDataForPlayer(player));
  }

  public static void runMigration(ServerPlayerEntity sourcePlayer, Identifier migrationId) {
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

  private static boolean tryRunMigration(ServerPlayerEntity sourcePlayer, Identifier migrationId) {
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
        Identifier id = currentData.id();
        ArrayDeque<Identifier> ids = new ArrayDeque<>(List.of(id));
        migration.pairs().forEach((from, to) -> {
          if (Objects.equals(ids.peek(), from)) {
            ids.push(to);
          }
        });

        Iterator<Identifier> itr = ids.iterator();
        while (itr.hasNext() && !registry.contains(itr.next())) {
          itr.remove();
        }

        Identifier targetId = ids.peek();
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
}
