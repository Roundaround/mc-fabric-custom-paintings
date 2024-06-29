package me.roundaround.custompaintings.server;

import com.google.common.collect.ImmutableList;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PaintingVariantTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OldServerPaintingManager {

  public static HashSet<UnknownPainting> getUnknownPaintings(ServerPlayerEntity player) {
    HashSet<UnknownPainting> unknown = new HashSet<>();
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
        return !known.containsKey(painting.getCustomData().id());
      }).forEach((painting) -> {
        PaintingData currentData = painting.getCustomData();
        Identifier currentId = currentData.id();

        PaintingData suggestedData = known.values().stream().filter((knownData) -> {
          return knownData.id().getPath().equals(currentId.getPath()) && knownData.width() == currentData.width() &&
              knownData.height() == currentData.height() && knownData.name().equals(currentData.name()) &&
              knownData.artist().equals(currentData.artist());
        }).findFirst().orElse(null);

        unknown.add(new UnknownPainting(painting.getUuid(), painting, currentData, suggestedData));
      });
    });

    return unknown;
  }

  public static HashSet<MismatchedPainting> getMismatchedPaintings(ServerPlayerEntity player) {
    return getMismatchedPaintings(player, PaintingData.MismatchedCategory.EVERYTHING);
  }

  public static HashSet<MismatchedPainting> getMismatchedPaintings(
      ServerPlayerEntity player, PaintingData.MismatchedCategory category
  ) {
    HashSet<MismatchedPainting> mismatched = new HashSet<>();

    if (!CustomPaintingsMod.knownPaintings.containsKey(player.getUuid())) {
      return mismatched;
    }

    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, painting -> {
        return known.containsKey(painting.getCustomData().id());
      }).forEach((painting) -> {
        PaintingData currentData = painting.getCustomData();

        if (known.containsKey(currentData.id())) {
          PaintingData knownData = known.get(currentData.id());

          if (currentData.isMismatched(knownData, category)) {
            mismatched.add(new MismatchedPainting(painting, currentData, knownData));
          }
        }
      });
    });

    return mismatched;
  }

  public static int setId(ServerPlayerEntity player, UUID paintingUuid, Identifier id) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
            return painting.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(entity -> setId(entity, id)).orElse(0);
  }

  public static int setId(PaintingEntity painting, Identifier id) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(id, paintingData.width(), paintingData.height(), paintingData.name(), paintingData.artist(),
        paintingData.isVanilla()
    );

    return 1;
  }

  public static int setWidth(ServerPlayerEntity player, UUID paintingUuid, int width) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
            return painting.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(entity -> setWidth(entity, width)).orElse(0);
  }

  public static int setWidth(PaintingEntity painting, int width) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), width, paintingData.height(), paintingData.name(), paintingData.artist(),
        paintingData.isVanilla()
    );

    return 1;
  }

  public static int setHeight(ServerPlayerEntity player, UUID paintingUuid, int height) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(entity -> setHeight(entity, height)).orElse(0);
  }

  public static int setHeight(PaintingEntity painting, int height) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), paintingData.width(), height, paintingData.name(), paintingData.artist(),
        paintingData.isVanilla()
    );

    return 1;
  }

  public static int setSize(ServerPlayerEntity player, UUID paintingUuid, int width, int height) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(entity -> setSize(entity, width, height)).orElse(0);
  }

  public static int setSize(PaintingEntity painting, int width, int height) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), width, height, paintingData.name(), paintingData.artist(),
        paintingData.isVanilla()
    );

    return 1;
  }

  public static int setName(ServerPlayerEntity player, UUID paintingUuid, String name) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(painting -> setName(player, painting, name)).orElse(0);
  }

  public static int setName(ServerPlayerEntity player, PaintingEntity painting, String name) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), paintingData.width(), paintingData.height(), name, paintingData.artist(),
        paintingData.isVanilla()
    );
    return 1;
  }

  public static int setArtist(ServerPlayerEntity player, UUID paintingUuid, String artist) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
            return painting.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(painting -> setArtist(player, painting, artist)).orElse(0);
  }

  public static int setArtist(ServerPlayerEntity player, PaintingEntity painting, String artist) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), paintingData.width(), paintingData.height(), paintingData.name(), artist,
        paintingData.isVanilla()
    );
    return 1;
  }

  public static int setLabel(ServerPlayerEntity player, UUID paintingUuid, String name, String artist) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
            return painting.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(painting -> setLabel(player, painting, name, artist)).orElse(0);
  }

  public static int setLabel(ServerPlayerEntity player, PaintingEntity painting, String name, String artist) {
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(paintingData.id(), paintingData.width(), paintingData.height(), name, artist,
        paintingData.isVanilla()
    );
    return 1;
  }

  public static int reassign(ServerPlayerEntity player, UUID paintingUuid, Identifier id) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(painting -> reassign(player, painting, id)).orElse(0);
  }

  public static int reassign(ServerPlayerEntity player, PaintingEntity painting, Identifier id) {
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    if (!known.containsKey(id)) {
      return -1;
    }

    PaintingData paintingData = known.get(id);

    painting.setCustomData(id, paintingData.width(), paintingData.height(), paintingData.name(), paintingData.artist(),
        paintingData.isVanilla()
    );

    if (paintingData.isVanilla()) {
      painting.setVariant(id);
    }

    return 1;
  }

  public static int reassign(ServerPlayerEntity player, Identifier from, Identifier to) {
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    if (!known.containsKey(to)) {
      return -1;
    }

    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> {
        return entity.getCustomData().id().equals(from);
      }).forEach((painting) -> {
        PaintingData paintingData = known.get(to);

        painting.setCustomData(to, paintingData.width(), paintingData.height(), paintingData.name(),
            paintingData.artist(), paintingData.isVanilla()
        );

        if (paintingData.isVanilla()) {
          painting.setVariant(to);
        }

        count.incrementAndGet();
      });
    });

    return count.get();
  }

  public static void updatePainting(ServerPlayerEntity player, UUID uuid) {
    updatePaintings(player, ImmutableList.of(uuid));
  }

  public static void updatePaintings(ServerPlayerEntity player, Collection<UUID> uuids) {
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    uuids.forEach((uuid) -> {
      player.getServer().getWorlds().forEach((world) -> {
        Optional.ofNullable(world.getEntity(uuid)).ifPresent((entity) -> {
          if (!(entity instanceof PaintingEntity painting)) {
            return;
          }

          Identifier id = painting.getCustomData().id();

          if (!known.containsKey(id)) {
            return;
          }

          PaintingData knownData = known.get(id);
          painting.setCustomData(id, knownData.width(), knownData.height(), knownData.name(), knownData.artist(),
              knownData.isVanilla()
          );

          if (knownData.isVanilla()) {
            painting.setVariant(id);
          }
        });
      });
    });
  }

  public static int removePainting(ServerPlayerEntity player, UUID uuid) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(uuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    return paintingEntity.map(painting -> removePainting(player, painting)).orElse(0);
  }

  public static int removePainting(ServerPlayerEntity player, PaintingEntity painting) {
    painting.damage(player.getDamageSources().playerAttack(player), 0);
    return 1;
  }

  public static int removePaintings(ServerPlayerEntity player, Identifier id) {
    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, painting -> painting.getCustomData().id().equals(id))
          .forEach((painting) -> {
            painting.damage(world.getDamageSources().playerAttack(player), 0);
            count.incrementAndGet();
          });
    });

    return count.get();
  }

  public static int removeUnknownPaintings(ServerPlayerEntity player) {
    Set<Identifier> known = getKnownPaintings(player).keySet();

    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, Entity::isAlive).stream().filter((painting) -> {
        Identifier id = painting.getCustomData().id();
        return !known.contains(id);
      }).forEach((painting) -> {
        painting.damage(world.getDamageSources().playerAttack(player), 0f);
        count.incrementAndGet();
      });
    });

    return count.get();
  }

  public static void customifyVanillaPaintings(ServerPlayerEntity player) {
    ArrayList<PaintingEntity> paintings = new ArrayList<>();

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof PaintingEntity)
          .stream()
          .filter((painting) -> painting.getCustomData().isEmpty())
          .forEach(paintings::add);
    });

    if (paintings.isEmpty()) {
      return;
    }

    HashMap<Identifier, PaintingData> map = getVanillaPaintingData();
    paintings.forEach((painting) -> {
      Identifier id = Registries.PAINTING_VARIANT.getId(painting.getVariant().value());
      if (!map.containsKey(id)) {
        return;
      }
      painting.setCustomData(map.get(id));
    });
  }

  public static Map<Identifier, PaintingData> getKnownPaintings(ServerPlayerEntity player) {
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));
    known.putAll(getVanillaPaintingData());
    return known;
  }

  public static HashMap<Identifier, PaintingData> getVanillaPaintingData() {
    HashMap<Identifier, PaintingData> placeable = new HashMap<>();
    HashMap<Identifier, PaintingData> unplaceable = new HashMap<>();

    Registries.PAINTING_VARIANT.stream().forEach((vanillaVariant) -> {
      Identifier id = Registries.PAINTING_VARIANT.getId(vanillaVariant);
      RegistryKey<PaintingVariant> key = RegistryKey.of(Registries.PAINTING_VARIANT.getKey(), id);
      Optional<RegistryEntry.Reference<PaintingVariant>> maybeEntry = Registries.PAINTING_VARIANT.getEntry(key);

      if (maybeEntry.isEmpty()) {
        return;
      }

      RegistryEntry<PaintingVariant> entry = maybeEntry.get();
      boolean isPlaceable = entry.isIn(PaintingVariantTags.PLACEABLE);
      HashMap<Identifier, PaintingData> map = isPlaceable ? placeable : unplaceable;
      PaintingData paintingData = new PaintingData(vanillaVariant);
      map.put(paintingData.id(), paintingData);
    });

    HashMap<Identifier, PaintingData> datas = new HashMap<>();
    datas.putAll(placeable);
    datas.putAll(unplaceable);
    return datas;
  }

  public static int applyMigration(ServerPlayerEntity player, Migration migration) {
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    HashMap<Identifier, PaintingData> knownMappings = new HashMap<>();
    HashMap<Identifier, Identifier> unknownMappings = new HashMap<>();

    migration.pairs().forEach((pair) -> {
      Identifier sourceId = new Identifier(pair.getLeft());
      Identifier targetId = new Identifier(pair.getRight());
      PaintingData targetData = known.getOrDefault(targetId, null);
      if (targetData == null) {
        unknownMappings.put(sourceId, targetId);
      } else {
        knownMappings.put(sourceId, targetData);
      }
    });

    if (knownMappings.isEmpty() && unknownMappings.isEmpty()) {
      return 0;
    }

    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, painting -> {
        return (knownMappings.containsKey(painting.getCustomData().id()) ||
            unknownMappings.containsKey(painting.getCustomData().id()));
      }).forEach((painting) -> {
        PaintingData currentData = painting.getCustomData();
        Identifier currentId = painting.getCustomData().id();
        if (knownMappings.containsKey(currentId)) {
          painting.setCustomData(knownMappings.get(currentId));
          count.incrementAndGet();
        } else if (unknownMappings.containsKey(currentId)) {
          painting.setCustomData(unknownMappings.get(currentId), currentData.width(), currentData.height(),
              currentData.name(), currentData.artist(), currentData.isVanilla()
          );
          count.incrementAndGet();
        }
      });
    });

    return count.get();
  }

  public static Optional<PaintingEntity> getPaintingInCrosshair(ServerPlayerEntity player) {
    Entity camera = player.getCameraEntity();
    double distance = 64;
    Vec3d posVec = camera.getCameraPosVec(0f);
    Vec3d rotationVec = camera.getRotationVec(1f);
    Vec3d targetVec = posVec.add(rotationVec.x * distance, rotationVec.y * distance, rotationVec.z * distance);

    HitResult crosshairTarget = ProjectileUtil.raycast(player.getCameraEntity(), posVec, targetVec,
        camera.getBoundingBox().stretch(rotationVec.multiply(distance)).expand(1.0, 1.0, 1.0),
        entity -> entity instanceof PaintingEntity, distance * distance
    );
    if (!(crosshairTarget instanceof EntityHitResult entityHitResult)) {
      return Optional.empty();
    }

    if (!(entityHitResult.getEntity() instanceof PaintingEntity)) {
      return Optional.empty();
    }

    return Optional.of((PaintingEntity) entityHitResult.getEntity());
  }
}
