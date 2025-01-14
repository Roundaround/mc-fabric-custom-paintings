package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.PaintingAssignment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.Util;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClientPaintingManager {
  private static final long TTL = 1000 * 60 * 15; // 15 minutes

  private static ClientPaintingManager instance = null;

  private final HashMap<Integer, PaintingData> cachedData = new HashMap<>();
  private final HashMap<Integer, Long> expiryTimes = new HashMap<>();

  private ClientPaintingManager() {
    ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      int id = painting.getId();
      PaintingData data = this.cachedData.get(id);
      if (data != null && !data.isEmpty()) {
        this.setPaintingData(painting, data);
      }

      this.remove(id);
    });
    ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      Entity.RemovalReason removalReason = painting.getRemovalReason();
      // On client side, unloaded paintings are always "discarded"
      if (removalReason == Entity.RemovalReason.DISCARDED) {
        this.cacheData(painting);
      } else {
        this.remove(painting.getId());
      }
    });
    ClientTickEvents.START_CLIENT_TICK.register((client) -> {
      long now = Util.getEpochTimeMs();
      List<Integer> expiredIds = this.expiryTimes.entrySet()
          .stream()
          .filter((entry) -> now >= entry.getValue())
          .map(Map.Entry::getKey)
          .toList();
      expiredIds.forEach(this::remove);
    });
  }

  public static void init() {
    ClientPaintingManager instance = getInstance();
    // In case clear somehow didn't get called before, clear it now
    instance.clear();
  }

  public static ClientPaintingManager getInstance() {
    if (instance == null) {
      instance = new ClientPaintingManager();
    }
    return instance;
  }

  public void trySetPaintingData(World world, PaintingAssignment assignment) {
    int id = assignment.getPaintingId();
    CompletableFuture<PaintingData> future = assignment.isKnown() ?
        ClientPaintingRegistry.getInstance().safeGet(assignment.getDataId()) :
        CompletableFuture.completedFuture(assignment.getData());
    future.thenAccept((data) -> {
      if (data == null || data.isEmpty()) {
        return;
      }

      Entity entity = world.getEntityById(id);
      if (!(entity instanceof PaintingEntity painting)) {
        this.cachedData.put(id, data);
        return;
      }

      this.setPaintingData(painting, data);
    });
  }

  public void clear() {
    this.cachedData.clear();
    this.expiryTimes.clear();
  }

  private void setPaintingData(PaintingEntity painting, PaintingData data) {
    if (data.vanilla()) {
      painting.setVariant(data.id());
    }
    painting.setCustomData(data);
  }

  private void remove(int id) {
    this.cachedData.remove(id);
    this.expiryTimes.remove(id);
  }

  private void cacheData(PaintingEntity painting) {
    int id = painting.getId();
    PaintingData data = painting.getCustomData();
    if (data != null && !data.isEmpty()) {
      this.cachedData.put(id, data);
      this.expiryTimes.put(id, Util.getEpochTimeMs() + TTL);
    }
  }
}
