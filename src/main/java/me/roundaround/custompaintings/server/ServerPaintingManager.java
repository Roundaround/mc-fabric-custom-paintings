package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.PaintingIdPair;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;

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
  private final HashMap<UUID, PaintingIdPair> allIds = new HashMap<>();

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

      // TODO: If painting data is unknown (or non matching?) send full data object instead of just id
      ServerNetworking.sendSetPaintingPacketToAll(
          loadedWorld.getServer(), painting.getId(), painting.getCustomData().id());
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
    ServerNetworking.sendSyncAllDataPacket(player, this.allIds.values().stream().toList());
  }

  public void remove(UUID uuid) {
    this.allPaintings.remove(uuid);
    this.allIds.remove(uuid);
    this.markDirty();
  }

  public void setPaintingData(PaintingEntity painting, PaintingData paintingData) {
    painting.setCustomData(paintingData);
    if (this.setTrackedData(painting.getUuid(), painting.getId(), paintingData)) {
      ServerNetworking.sendSetPaintingPacketToAll(this.world.getServer(), painting.getId(), paintingData.id());
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

  private boolean setTrackedData(UUID paintingUuid, int paintingId, PaintingData paintingData) {
    this.allIds.put(paintingUuid, new PaintingIdPair(paintingId, paintingData.id()));
    PaintingData previousData = this.allPaintings.put(paintingUuid, paintingData);
    if (previousData == null || !previousData.equals(paintingData)) {
      this.markDirty();
      return true;
    }
    return false;
  }
}
