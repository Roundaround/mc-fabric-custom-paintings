package me.roundaround.custompaintings.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

public class ServerPaintingManager {
  public static HashSet<UnknownPainting> getUnknownPaintings(ServerPlayerEntity player) {
    HashSet<UnknownPainting> unknown = new HashSet<>();
    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, (painting) -> {
        return painting instanceof ExpandedPaintingEntity &&
            !known.containsKey(((ExpandedPaintingEntity) painting).getCustomData().id());
      }).forEach((entity) -> {
        ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
        PaintingData currentData = painting.getCustomData();
        Identifier currentId = currentData.id();

        PaintingData suggestedData = known.values().stream()
            .filter((knownData) -> {
              return knownData.id().getPath().equals(currentId.getPath())
                  && knownData.width() == currentData.width()
                  && knownData.height() == currentData.height()
                  && knownData.name().equals(currentData.name())
                  && knownData.artist().equals(currentData.artist());
            })
            .findFirst()
            .orElse(null);

        unknown.add(new UnknownPainting(
            entity.getUuid(),
            entity,
            currentData,
            suggestedData));
      });
    });

    return unknown;
  }

  public static HashSet<MismatchedPainting> getMismatchedPaintings(ServerPlayerEntity player) {
    return getMismatchedPaintings(player, MismatchedCategory.EVERYTHING);
  }

  public static HashSet<MismatchedPainting> getMismatchedPaintings(
      ServerPlayerEntity player,
      MismatchedCategory category) {
    HashSet<MismatchedPainting> mismatched = new HashSet<>();

    if (!CustomPaintingsMod.knownPaintings.containsKey(player.getUuid())) {
      return mismatched;
    }

    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> {
        return entity instanceof ExpandedPaintingEntity &&
            known.containsKey(((ExpandedPaintingEntity) entity).getCustomData().id());
      })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData currentData = painting.getCustomData();

            if (known.containsKey(currentData.id())) {
              PaintingData knownData = known.get(currentData.id());

              if (currentData.isMismatched(knownData, category)) {
                mismatched.add(new MismatchedPainting(
                    entity,
                    currentData,
                    knownData));
              }
            }
          });
    });

    return mismatched;
  }

  public static int setId(ServerPlayerEntity player, UUID paintingUuid, Identifier id) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return setId(player, paintingEntity.get(), id);
  }

  public static int setId(ServerPlayerEntity player, PaintingEntity paintingEntity, Identifier id) {
    if (!(paintingEntity instanceof ExpandedPaintingEntity)) {
      return 0;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) paintingEntity;
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(
        id,
        paintingData.index(),
        paintingData.width(),
        paintingData.height(),
        paintingData.name(),
        paintingData.artist(),
        paintingData.isVanilla());
    return 1;
  }

  public static int setWidth(ServerPlayerEntity player, UUID paintingUuid, int width) {
    Optional<PaintingEntity> paintingEntity = StreamSupport.stream(player.getServer().getWorlds().spliterator(), false)
        .flatMap((world) -> {
          return world.getEntitiesByType(EntityType.PAINTING, (entity) -> {
            return entity.getUuid().equals(paintingUuid);
          }).stream();
        })
        .map((entity) -> (PaintingEntity) entity)
        .findFirst();

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return setWidth(player, paintingEntity.get(), width);
  }

  public static int setWidth(ServerPlayerEntity player, PaintingEntity paintingEntity, int width) {
    if (!(paintingEntity instanceof ExpandedPaintingEntity)) {
      return 0;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) paintingEntity;
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(
        paintingData.id(),
        paintingData.index(),
        width,
        paintingData.height(),
        paintingData.name(),
        paintingData.artist(),
        paintingData.isVanilla());
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

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return setHeight(player, paintingEntity.get(), height);
  }

  public static int setHeight(ServerPlayerEntity player, PaintingEntity paintingEntity, int height) {
    if (!(paintingEntity instanceof ExpandedPaintingEntity)) {
      return 0;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) paintingEntity;
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(
        paintingData.id(),
        paintingData.index(),
        paintingData.width(),
        height,
        paintingData.name(),
        paintingData.artist(),
        paintingData.isVanilla());
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

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return setSize(player, paintingEntity.get(), width, height);
  }

  public static int setSize(ServerPlayerEntity player, PaintingEntity paintingEntity, int width, int height) {
    if (!(paintingEntity instanceof ExpandedPaintingEntity)) {
      return 0;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) paintingEntity;
    PaintingData paintingData = painting.getCustomData();

    painting.setCustomData(
        paintingData.id(),
        paintingData.index(),
        width,
        height,
        paintingData.name(),
        paintingData.artist(),
        paintingData.isVanilla());
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

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return reassign(player, paintingEntity.get(), id);
  }

  public static int reassign(ServerPlayerEntity player, PaintingEntity paintingEntity, Identifier id) {
    if (!(paintingEntity instanceof ExpandedPaintingEntity)) {
      return 0;
    }

    Map<Identifier, PaintingData> known = getKnownPaintings(player);

    if (!known.containsKey(id)) {
      return -1;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) paintingEntity;
    PaintingData paintingData = known.get(id);

    painting.setCustomData(
        id,
        paintingData.index(),
        paintingData.width(),
        paintingData.height(),
        paintingData.name(),
        paintingData.artist(),
        paintingData.isVanilla());
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
        return entity instanceof ExpandedPaintingEntity &&
            ((ExpandedPaintingEntity) entity).getCustomData().id().equals(from);
      })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData paintingData = known.get(to);

            painting.setCustomData(
                to,
                paintingData.index(),
                paintingData.width(),
                paintingData.height(),
                paintingData.name(),
                paintingData.artist(),
                paintingData.isVanilla());

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
        Optional.ofNullable(world.getEntity(uuid))
            .ifPresent((entity) -> {
              if (!(entity instanceof ExpandedPaintingEntity)) {
                return;
              }

              ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
              Identifier id = painting.getCustomData().id();

              if (!known.containsKey(id)) {
                return;
              }

              PaintingData knownData = known.get(id);
              painting.setCustomData(
                  id,
                  knownData.index(),
                  knownData.width(),
                  knownData.height(),
                  knownData.name(),
                  knownData.artist(),
                  knownData.isVanilla());
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

    if (!paintingEntity.isPresent()) {
      return 0;
    }

    return removePainting(player, paintingEntity.get());
  }

  public static int removePainting(ServerPlayerEntity player, PaintingEntity paintingEntity) {
    paintingEntity.damage(DamageSource.player(player), 0);
    return 1;
  }

  public static int removePaintings(ServerPlayerEntity player, Identifier id) {
    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> {
        return entity instanceof ExpandedPaintingEntity &&
            ((ExpandedPaintingEntity) entity).getCustomData().id().equals(id);
      })
          .forEach((entity) -> {
            entity.damage(DamageSource.player(player), 0);
            count.incrementAndGet();
          });
    });

    return count.get();
  }

  public static int removeUnknownPaintings(ServerPlayerEntity player) {
    Set<Identifier> known = ServerPaintingManager.getKnownPaintings(player).keySet();

    AtomicInteger count = new AtomicInteger(0);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier id = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return !known.contains(id);
          })
          .forEach((entity) -> {
            entity.damage(DamageSource.player(player), 0f);
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
          .filter((entity) -> {
            return !(entity instanceof ExpandedPaintingEntity)
                || ((ExpandedPaintingEntity) entity).getCustomData().isEmpty();
          })
          .forEach((entity) -> {
            paintings.add((PaintingEntity) entity);
          });
    });

    if (paintings.isEmpty()) {
      return;
    }

    HashMap<Identifier, PaintingData> map = getVanillaPaintingData();
    paintings.forEach((painting) -> {
      Identifier id = Registry.PAINTING_VARIANT.getId(painting.getVariant().value());
      if (!map.containsKey(id)) {
        return;
      }
      ((ExpandedPaintingEntity) painting).setCustomData(map.get(id));
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

    Registry.PAINTING_VARIANT.stream()
        .forEach((vanillaVariant) -> {
          Identifier id = Registry.PAINTING_VARIANT.getId(vanillaVariant);
          RegistryKey<PaintingVariant> key = RegistryKey.of(Registry.PAINTING_VARIANT_KEY, id);
          Optional<RegistryEntry<PaintingVariant>> maybeEntry = Registry.PAINTING_VARIANT.getEntry(key);

          if (!maybeEntry.isPresent()) {
            return;
          }

          RegistryEntry<PaintingVariant> entry = maybeEntry.get();
          boolean isPlaceable = entry.isIn(PaintingVariantTags.PLACEABLE);
          HashMap<Identifier, PaintingData> map = isPlaceable ? placeable : unplaceable;
          PaintingData paintingData = new PaintingData(vanillaVariant, map.size());
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
      world.getEntitiesByType(EntityType.PAINTING, entity -> {
        return entity instanceof ExpandedPaintingEntity &&
            (knownMappings.containsKey(((ExpandedPaintingEntity) entity).getCustomData().id())
                || unknownMappings.containsKey(((ExpandedPaintingEntity) entity).getCustomData().id()));
      })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData currentData = painting.getCustomData();
            Identifier currentId = painting.getCustomData().id();
            if (knownMappings.containsKey(currentId)) {
              painting.setCustomData(knownMappings.get(currentId));
              count.incrementAndGet();
            } else if (unknownMappings.containsKey(currentId)) {
              painting.setCustomData(
                  unknownMappings.get(currentId),
                  currentData.index(),
                  currentData.width(),
                  currentData.height(),
                  currentData.name(),
                  currentData.artist(),
                  currentData.isVanilla());
              count.incrementAndGet();
            }
          });
    });

    return count.get();
  }
}
