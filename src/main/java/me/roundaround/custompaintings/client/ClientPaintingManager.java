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
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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

      this.expiryTimes.remove(painting.getId());

      PaintingData paintingData = this.cachedData.get(painting.getId());
      if (paintingData != null && !paintingData.isEmpty()) {
        this.setPaintingData(painting, paintingData);
      }
    });
    ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      Entity.RemovalReason removalReason = painting.getRemovalReason();
      if (removalReason != null && removalReason.shouldDestroy()) {
        this.remove(painting);
        return;
      }

      this.scheduleRemoval(painting);
    });
    ClientTickEvents.START_CLIENT_TICK.register((client) -> {
      long now = Util.getMeasuringTimeMs();
      var iter = this.expiryTimes.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<Integer, Long> entry = iter.next();
        if (now < entry.getValue()) {
          continue;
        }

        this.cachedData.remove(entry.getKey());
        iter.remove();
      }
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
    int paintingId = assignment.getPaintingId();
    CompletableFuture<PaintingData> future = assignment.isKnown() ?
        ClientPaintingRegistry.getInstance().safeGet(assignment.getDataId()) :
        CompletableFuture.completedFuture(assignment.getData());
    future.thenAccept((paintingData) -> {
      if (paintingData == null || paintingData.isEmpty()) {
        return;
      }

      Entity entity = world.getEntityById(paintingId);
      if (!(entity instanceof PaintingEntity painting)) {
        this.cachedData.put(paintingId, paintingData);
        return;
      }

      this.setPaintingData(painting, paintingData);
    });
  }

  public void clear() {
    this.cachedData.clear();
    this.expiryTimes.clear();
  }

  private void setPaintingData(PaintingEntity painting, PaintingData paintingData) {
    if (paintingData.vanilla()) {
      painting.setVariant(paintingData.id());
    }
    painting.setCustomData(paintingData);
  }

  private void remove(PaintingEntity painting) {
    int id = painting.getId();
    this.cachedData.remove(id);
    this.expiryTimes.remove(id);
  }

  private void scheduleRemoval(PaintingEntity painting) {
    this.expiryTimes.put(painting.getId(), Util.getMeasuringTimeMs() + TTL);
  }
}
