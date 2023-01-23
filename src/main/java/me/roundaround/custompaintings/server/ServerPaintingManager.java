package me.roundaround.custompaintings.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.OutdatedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

public class ServerPaintingManager {
  public static HashSet<UnknownPainting> getUnknownPaintings(ServerPlayerEntity player) {
    HashSet<UnknownPainting> unknownPaintings = new HashSet<>();
    HashMap<Identifier, Integer> counts = new HashMap<>();
    HashMap<Identifier, Identifier> autoFixIds = new HashMap<>();

    if (!CustomPaintingsMod.knownPaintings.containsKey(player.getUuid())) {
      return unknownPaintings;
    }

    Map<Identifier, PaintingData> known = getKnownData(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            PaintingData paintingData = ((ExpandedPaintingEntity) entity).getCustomData();
            return !paintingData.isVanilla() && !known.containsKey(paintingData.id());
          })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData currentData = painting.getCustomData();
            Identifier currentId = currentData.id();

            if (known.containsKey(currentId)) {
              return;
            }

            if (counts.containsKey(currentId)) {
              counts.put(currentId, counts.get(currentId) + 1);
            } else {
              counts.put(currentId, 1);
            }

            Identifier autoFixIdentifier = known.values().stream()
                .filter((knownData) -> {
                  return knownData.id().getPath().equals(currentId.getPath())
                      && knownData.width() == currentData.width()
                      && knownData.height() == currentData.height()
                      && knownData.name().equals(currentData.name())
                      && knownData.artist().equals(currentData.artist());
                })
                .map(PaintingData::id)
                .findFirst()
                .orElse(null);
            autoFixIds.put(currentId, autoFixIdentifier);
          });
    });

    counts.forEach((id, count) -> {
      unknownPaintings.add(new UnknownPainting(
          id,
          count,
          autoFixIds.get(id)));
    });

    return unknownPaintings;
  }

  public static HashSet<OutdatedPainting> getOutdatedPaintings(ServerPlayerEntity player) {
    HashSet<OutdatedPainting> outdatedPaintings = new HashSet<>();

    if (!CustomPaintingsMod.knownPaintings.containsKey(player.getUuid())) {
      return outdatedPaintings;
    }

    Map<Identifier, PaintingData> known = getKnownData(player);

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return known.containsKey(entityId);
          })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData currentData = painting.getCustomData();

            if (known.containsKey(currentData.id())) {
              PaintingData knownData = known.get(currentData.id());

              if (currentData.isMismatched(knownData)) {
                outdatedPaintings.add(new OutdatedPainting(
                    entity.getUuid(),
                    currentData,
                    knownData));
              }
            }
          });
    });

    return outdatedPaintings;
  }

  public static int autoFixPaintings(MinecraftServer server, ServerPlayerEntity player) {
    ArrayList<ExpandedPaintingEntity> missing = new ArrayList<>();
    Map<Identifier, PaintingData> known = getKnownData(player);

    server.getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return known.containsKey(entityId);
          })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData currentData = painting.getCustomData();

            if (!known.containsKey(currentData.id())) {
              missing.add(painting);
            }
          });
    });

    // Try to auto-fix missing paintings. If the same id (path) exists within
    // another namespace with the same info and size, just change the id and
    // index automatically.
    int autoFixed = 0;
    for (ExpandedPaintingEntity painting : missing) {
      PaintingData currentData = painting.getCustomData();
      Identifier currentId = currentData.id();

      PaintingData newData = known.values().stream()
          .filter((knownData) -> {
            return knownData.id().getPath().equals(currentId.getPath())
                && knownData.width() == currentData.width()
                && knownData.height() == currentData.height()
                && knownData.name().equals(currentData.name())
                && knownData.artist().equals(currentData.artist());
          })
          .findFirst()
          .orElse(null);

      if (newData != null) {
        painting.setCustomData(
            newData.id(),
            newData.index(),
            currentData.width(),
            currentData.height(),
            currentData.name(),
            currentData.artist());

        autoFixed++;
      }
    }

    return autoFixed;
  }

  public static void reassignIds(ServerPlayerEntity player, Identifier from, Identifier to, boolean fix) {
    Map<Identifier, PaintingData> known = getKnownData(player);

    if (!known.containsKey(to)) {
      return;
    }

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return entityId.equals(from);
          })
          .forEach((entity) -> {
            ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
            PaintingData paintingData = fix ? known.get(to) : painting.getCustomData();

            painting.setCustomData(
                to,
                paintingData.index(),
                paintingData.width(),
                paintingData.height(),
                paintingData.name(),
                paintingData.artist());
          });
    });
  }

  public static void updatePainting(ServerPlayerEntity player, UUID uuid) {
    updatePaintings(player, ImmutableList.of(uuid));
  }

  public static void updatePaintings(ServerPlayerEntity player, Collection<UUID> uuids) {
    Map<Identifier, PaintingData> known = getKnownData(player);

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
                  knownData.artist());
            });
      });
    });
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

    HashMap<Identifier, PaintingData> map = getVanillaPaintingData();
    paintings.forEach((painting) -> {
      Identifier id = Registry.PAINTING_VARIANT.getId(painting.getVariant().value());
      PaintingData paintingData = map.get(id);
      ((ExpandedPaintingEntity) painting).setCustomData(
          paintingData.id(),
          paintingData.index(),
          paintingData.width(),
          paintingData.height(),
          paintingData.name(),
          paintingData.artist());
    });
  }

  public static Map<Identifier, PaintingData> getKnownData(ServerPlayerEntity player) {
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
}
