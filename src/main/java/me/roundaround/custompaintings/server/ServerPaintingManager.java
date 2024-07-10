package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerPaintingManager extends PersistentState {
  private final ServerWorld world;
  private final HashMap<UUID, PaintingData> allPaintings = new HashMap<>();

  public static void init(ServerWorld world) {
    // Just getting the instance also creates/initializes it
    getInstance(world);
  }

  public static ServerPaintingManager getInstance(ServerWorld world) {
    Type<ServerPaintingManager> persistentStateType = new PersistentState.Type<>(
        () -> new ServerPaintingManager(world), (nbt, registryLookup) -> fromNbt(world, nbt), null);
    return world.getPersistentStateManager().getOrCreate(persistentStateType, CustomPaintingsMod.MOD_ID);
  }

  private ServerPaintingManager(ServerWorld world) {
    this.world = world;

    ServerEntityEvents.ENTITY_LOAD.register((entity, loadedWorld) -> {
      if (loadedWorld != this.world || !(entity instanceof PaintingEntity painting)) {
        return;
      }
      this.loadPainting(painting);
      this.fixCustomName(painting);
    });
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

  private static ServerPaintingManager fromNbt(ServerWorld world, NbtCompound nbt) {
    ServerPaintingManager manager = new ServerPaintingManager(world);
    NbtList nbtList = nbt.getList("Paintings", NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound nbtCompound = nbtList.getCompound(i);
      manager.allPaintings.put(nbtCompound.getUuid("PaintingUuid"), PaintingData.fromNbt(nbtCompound));
    }
    return manager;
  }

  public void remove(UUID uuid) {
    this.allPaintings.remove(uuid);
    this.setDirty(true);
  }

  private boolean setTrackedData(UUID uuid, PaintingData paintingData) {
    PaintingData previousData = this.allPaintings.put(uuid, paintingData);
    if (previousData == null || !previousData.equals(paintingData)) {
      this.setDirty(true);
      return true;
    }
    return false;
  }

  public void setPaintingData(int paintingId, Identifier dataId) {
    Entity entity = this.world.getEntityById(paintingId);
    if (!(entity instanceof PaintingEntity painting)) {
      CustomPaintingsMod.LOGGER.error("Received SetPaintingC2S packet with invalid entity id {}", paintingId);
      return;
    }

    PaintingData paintingData = ServerPaintingRegistry.getInstance().get(dataId);
    if (paintingData == null) {
      // Some kind of error handling?
      CustomPaintingsMod.LOGGER.error("Received SetPaintingC2S packet with invalid painting data id {}", dataId);
      return;
    }

    this.setPaintingData(painting, paintingData);
  }

  public void setPaintingData(PaintingEntity painting, PaintingData paintingData) {
    painting.setCustomData(paintingData);
    if (this.setTrackedData(painting.getUuid(), paintingData)) {
      ServerNetworking.sendSetPaintingPacket(painting.getId(), paintingData.id());
    }
  }

  public boolean setPaintingDataAndPropagate(PaintingEntity painting, PaintingData paintingData) {
    boolean stateChanged = this.setTrackedData(painting.getUuid(), paintingData);
    this.setCustomDataOnPainting(painting, paintingData);
    return stateChanged;
  }

  public void setCustomDataOnPainting(UUID uuid, PaintingData paintingData) {
    this.findPainting(uuid).ifPresent((painting) -> this.setCustomDataOnPainting(painting, paintingData));
  }

  public void setCustomDataOnPainting(PaintingEntity painting, PaintingData paintingData) {
    painting.setCustomData(paintingData);
  }

  public Optional<PaintingEntity> findPainting(UUID uuid) {
    Entity entity = this.world.getEntity(uuid);
    if (!(entity instanceof PaintingEntity painting)) {
      return Optional.empty();
    }
    return Optional.of(painting);
  }

  public void loadPainting(PaintingEntity painting) {
    UUID uuid = painting.getUuid();
    PaintingData paintingData = painting.getCustomData();

    if (this.allPaintings.containsKey(uuid)) {
      this.setPaintingData(painting, this.allPaintings.get(uuid));
      return;
    }


    if (paintingData == null || paintingData.isEmpty()) {
      this.setPaintingData(
          painting, VanillaPaintingRegistry.getInstance().get(painting.getVariant().value()));
      return;
    }

    this.setTrackedData(uuid, paintingData);
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
}
