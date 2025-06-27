package me.roundaround.custompaintings.server;

import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerPaintingManager extends PersistentState {
  public static final Codec<ServerPaintingManager> CODEC =
      RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
          PaintingDataWithUuid.CODEC)
      .xmap(ServerPaintingManager::listToMap, ServerPaintingManager::mapToList)
      .fieldOf("Paintings")
      .forGetter((i) -> i.paintings)).apply(instance, ServerPaintingManager::new));
  public static final PersistentStateType<ServerPaintingManager> STATE_TYPE = new PersistentStateType<>(
      Constants.MOD_ID,
      ServerPaintingManager::new,
      CODEC,
      null
  );

  private final HashMap<UUID, PaintingData> paintings = new HashMap<>();
  private final HashMap<UUID, Integer> networkIds = new HashMap<>();

  private ServerPaintingManager() {
    this.markDirty();
  }

  private ServerPaintingManager(Map<UUID, PaintingData> paintings) {
    this.paintings.putAll(paintings);
  }

  private static Map<UUID, PaintingData> listToMap(List<PaintingDataWithUuid> list) {
    HashMap<UUID, PaintingData> map = new HashMap<>(list.size());
    for (var item : list) {
      map.put(item.paintingUuid(), item);
    }
    return map;
  }

  private static List<PaintingDataWithUuid> mapToList(Map<UUID, PaintingData> map) {
    return map.entrySet().stream().map((entry) -> new PaintingDataWithUuid(entry.getKey(), entry.getValue())).toList();
  }

  public void onEntityLoad(PaintingEntity painting) {
    this.loadPainting(painting);
    this.fixCustomName(painting);
  }

  public void onEntityUnload(PaintingEntity painting) {
    Entity.RemovalReason removalReason = painting.getRemovalReason();
    if (removalReason == null || !removalReason.shouldDestroy()) {
      return;
    }
    this.remove(painting.getUuid());
  }

  public void syncAllDataForPlayer(ServerPlayerEntity player) {
    Function<CustomId, Boolean> lookup = ServerPaintingRegistry.getInstance()::contains;
    List<PaintingAssignment> assignments = this.paintings.entrySet()
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
    this.setPaintingData(painting, data, false);
  }

  public void setPaintingData(PaintingEntity painting, PaintingData data, boolean forceSync) {
    painting.custompaintings$setData(data);
    if (this.setTrackedData(painting.getUuid(), painting.getId(), data) || forceSync) {
      ServerNetworking.sendSetPaintingPacketToAll(
          (ServerWorld) painting.getWorld(),
          PaintingAssignment.from(painting.getId(), data, ServerPaintingRegistry.getInstance()::contains)
      );
    }
  }

  private void loadPainting(PaintingEntity painting) {
    UUID uuid = painting.getUuid();

    PaintingData data = this.paintings.get(uuid);
    if (data == null) {
      data = painting.custompaintings$getData();
    }
    if (data.isEmpty()) {
      data = new PaintingData(painting.getVariant().value());
    }

    this.setPaintingData(painting, data, true);
  }

  /**
   * Older versions of the mod forced assigning the label to the painting's custom name and force enabling the custom
   * name to be visible. Now that we have enabled pulling the label directly (and not relying on custom name) and
   * toggling label visibility with interaction, we don't need this hacky implementation. For any existing paintings
   * in the world, we want to try to detect when we might have set these parameters and remove them.
   */
  private void fixCustomName(PaintingEntity painting) {
    PaintingData data = painting.custompaintings$getData();
    Text customName = painting.getCustomName();

    if (data.isEmpty() || !painting.isCustomNameVisible() || customName == null) {
      return;
    }

    if (painting.isCustomNameVisible() && customName.getString().isBlank()) {
      painting.setCustomName(null);
      return;
    }

    if (!data.hasLabel()) {
      return;
    }

    List<String> potentialOldLabels = List.of(data.name(), data.artist(), data.id().resource());
    String customNameContent = customName.getString();
    if (potentialOldLabels.stream().anyMatch(customNameContent::startsWith)) {
      painting.setCustomName(null);
      painting.setCustomNameVisible(false);
    }
  }

  private void remove(UUID uuid) {
    this.networkIds.remove(uuid);
    if (this.paintings.remove(uuid) != null) {
      this.markDirty();
    }
  }

  private boolean setTrackedData(UUID paintingUuid, Integer paintingId, PaintingData data) {
    if (paintingId != null) {
      this.networkIds.put(paintingUuid, paintingId);
    }

    PaintingData previousData = this.paintings.put(paintingUuid, data);
    if (previousData == null || !previousData.equals(data)) {
      this.markDirty();
      return true;
    }

    return false;
  }

  public static void syncAllDataForAllPlayers(MinecraftServer server) {
    server.getPlayerManager()
        .getPlayerList()
        .forEach((player) -> player.getWorld().custompaintings$getPaintingManager().syncAllDataForPlayer(player));
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
      ServerPaintingManager manager = world.custompaintings$getPaintingManager();
      manager.paintings.forEach((paintingUuid, currentData) -> {
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
          painting.custompaintings$setData(data);
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
    return this.paintings.entrySet().stream().filter((entry) -> {
      CustomId id = entry.getValue().id();
      return !registry.contains(id) && (dataId == null || dataId.equals(id));
    }).map(Map.Entry::getKey).collect(Collectors.toSet());
  }

  public Map<CustomId, Integer> getUnknownPaintingCounts() {
    HashMap<CustomId, Integer> counts = new HashMap<>();
    ServerPaintingRegistry registry = ServerPaintingRegistry.getInstance();

    this.paintings.forEach((uuid, assignment) -> {
      CustomId id = assignment.id();
      if (registry.contains(id)) {
        return;
      }
      counts.put(id, counts.getOrDefault(id, 0) + 1);
    });

    return counts;
  }

  public int fixUnknownPaintings(ServerWorld world, CustomId dataId, CustomId targetId) {
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

      if (world.getEntity(uuid) instanceof PaintingEntity painting) {
        paintingId = painting.getId();
        painting.custompaintings$setData(targetData);
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
    return this.paintings.entrySet().stream().map((entry) -> {
      PaintingData currentData = entry.getValue();
      PaintingData knownData = registry.get(currentData.id());
      if (knownData == null || knownData.isEmpty() || !currentData.isMismatched(knownData, category)) {
        return null;
      }
      return new MismatchedReference(entry.getKey(), knownData);
    }).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  public int fixMismatchedPaintings(ServerWorld world, PaintingData.MismatchedCategory category) {
    Set<MismatchedReference> needsFixed = this.getMismatchedPaintings(category);
    var changed = new Object() {
      int count = 0;
    };

    needsFixed.forEach((ref) -> {
      UUID uuid = ref.uuid();
      PaintingData knownData = ref.knownData();
      Integer paintingId = null;

      if (world.getEntity(uuid) instanceof PaintingEntity painting) {
        paintingId = painting.getId();
        painting.custompaintings$setData(knownData);
      }

      if (this.setTrackedData(uuid, paintingId, knownData)) {
        changed.count++;
      }
    });

    return changed.count;
  }

  public static Set<UUID> getUnknownPaintings(MinecraftServer server, CustomId dataId) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> world.custompaintings$getPaintingManager().getUnknownPaintings(dataId).stream())
        .collect(Collectors.toSet());
  }

  public static Map<CustomId, Integer> getUnknownPaintingCounts(MinecraftServer server) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> world.custompaintings$getPaintingManager().getUnknownPaintingCounts().entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static int fixUnknownPaintings(MinecraftServer server, CustomId dataId, CustomId targetId) {
    return Streams.stream(server.getWorlds())
        .mapToInt((world) -> world.custompaintings$getPaintingManager().fixUnknownPaintings(world, dataId, targetId))
        .sum();
  }

  public static Set<MismatchedReference> getMismatchedPaintings(MinecraftServer server) {
    return getMismatchedPaintings(server, PaintingData.MismatchedCategory.EVERYTHING);
  }

  public static Set<MismatchedReference> getMismatchedPaintings(
      MinecraftServer server,
      PaintingData.MismatchedCategory category
  ) {
    return Streams.stream(server.getWorlds())
        .flatMap((world) -> world.custompaintings$getPaintingManager().getMismatchedPaintings(category).stream())
        .collect(Collectors.toSet());
  }

  public static int fixMismatchedPaintings(MinecraftServer server, PaintingData.MismatchedCategory category) {
    return Streams.stream(server.getWorlds())
        .mapToInt((world) -> world.custompaintings$getPaintingManager().fixMismatchedPaintings(world, category))
        .sum();
  }

  public record MismatchedReference(UUID uuid, PaintingData knownData) {
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MismatchedReference that)) {
        return false;
      }
      return Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.uuid);
    }
  }

  private static class PaintingDataWithUuid extends PaintingData {
    public static final Codec<PaintingDataWithUuid> CODEC = RecordCodecBuilder.create((instance) -> mapBaseCodecFields(
        instance).and(Uuids.INT_STREAM_CODEC.fieldOf("PaintingUuid").forGetter(PaintingDataWithUuid::paintingUuid))
        .apply(instance, PaintingDataWithUuid::new));

    private final UUID paintingUuid;

    public PaintingDataWithUuid(
        CustomId id,
        int width,
        int height,
        @NotNull String name,
        @NotNull String artist,
        boolean vanilla,
        boolean unknown,
        UUID paintingUuid
    ) {
      super(id, width, height, name, artist, vanilla, unknown);
      this.paintingUuid = paintingUuid;
    }

    public PaintingDataWithUuid(UUID paintingUuid, PaintingData base) {
      this(
          base.id(),
          base.width(),
          base.height(),
          base.name(),
          base.artist(),
          base.vanilla(),
          base.unknown(),
          paintingUuid
      );
    }

    public UUID paintingUuid() {
      return this.paintingUuid;
    }
  }
}
