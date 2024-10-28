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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ServerPaintingManager extends PersistentState {
  private static final String NBT_SERVER_ID = "ServerId";
  private static final String NBT_PAINTINGS = "Paintings";
  private static final String NBT_PAINTING_UUID = "PaintingUuid";

  private final ServerWorld world;
  private final UUID serverId;
  private final HashMap<UUID, PaintingData> allPaintings = new HashMap<>();
  private final HashMap<UUID, Integer> networkIds = new HashMap<>();
  private final ArrayDeque<MigrationData> runMigrations = new ArrayDeque<>();

  public static void init(ServerWorld world) {
    // Just getting the instance also creates/initializes it
    getInstance(world);
  }

  public static ServerPaintingManager getInstance(ServerWorld world) {
    Type<ServerPaintingManager> persistentStateType = new PersistentState.Type<>(() -> new ServerPaintingManager(world),
        (nbt, registryLookup) -> fromNbt(world, nbt), null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, CustomPaintingsMod.MOD_ID);
  }

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
      // TODO: this.runThroughMigrations(painting);

      ServerNetworking.sendSetPaintingPacketToAll(
          loadedWorld.getServer(), PaintingAssignment.from(painting.getId(), dataOrUnknown(painting.getCustomData())));
    });
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    nbt.putUuid(NBT_SERVER_ID, this.serverId);

    NbtList nbtList = new NbtList();
    this.allPaintings.forEach((uuid, paintingData) -> {
      NbtCompound nbtCompound = paintingData.writeToNbt();
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

    NbtList nbtList = nbt.getList(NBT_PAINTINGS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound nbtCompound = nbtList.getCompound(i);
      manager.allPaintings.put(nbtCompound.getUuid(NBT_PAINTING_UUID), PaintingData.fromNbt(nbtCompound));
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
    List<PaintingAssignment> assignments = this.allPaintings.entrySet().stream().map((entry) -> {
      UUID id = entry.getKey();
      PaintingData data = entry.getValue();
      return PaintingAssignment.from(this.networkIds.get(id), dataOrUnknown(data));
    }).toList();
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
      ServerNetworking.sendSetPaintingPacketToAll(
          this.world.getServer(), PaintingAssignment.from(painting.getId(), dataOrUnknown(data)));
    }
  }

  public void loadPainting(PaintingEntity painting) {
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
  public void fixCustomName(PaintingEntity painting) {
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

  private boolean setTrackedData(UUID paintingUuid, int paintingId, PaintingData data) {
    this.networkIds.put(paintingUuid, paintingId);
    PaintingData previousData = this.allPaintings.put(paintingUuid, data);
    if (previousData == null || !previousData.equals(data)) {
      this.markDirty();
      return true;
    }
    return false;
  }

  private static PaintingData dataOrUnknown(PaintingData data) {
    if (data == null || data.isUnknown()) {
      return data;
    }
    if (!ServerPaintingRegistry.getInstance().contains(data.id())) {
      return data.toUnknown();
    }
    return data;
  }

  public static void syncAllDataForAllPlayers(MinecraftServer server) {
    server.getPlayerManager()
        .getPlayerList()
        .forEach((player) -> getInstance(player.getServerWorld()).syncAllDataForPlayer(player));
  }

  public static void runMigration(ServerPlayerEntity sourcePlayer, MigrationData migration) {
    if (sourcePlayer == null || !sourcePlayer.hasPermissionLevel(2)) {
      return;
    }

    MinecraftServer server = sourcePlayer.getServer();
    if (server == null || !server.isRunning()) {
      return;
    }

    server.getWorlds().forEach((world) -> {
      ServerPaintingManager.getInstance(world).runMigrations.push(migration);
      migration.pairs().forEach((from, to) -> {
        // TODO: getAllPaintings(world, from).forEach(convert(to))
      });
    });

    syncAllDataForAllPlayers(server);
  }
}
