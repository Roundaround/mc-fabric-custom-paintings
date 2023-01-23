package me.roundaround.custompaintings.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.OutdatedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ServerPaintingManager {
  public static HashSet<UnknownPainting> getUnknownPaintings(ServerPlayerEntity player) {
    HashSet<UnknownPainting> unknownPaintings = new HashSet<>();
    HashMap<Identifier, Integer> counts = new HashMap<>();
    HashMap<Identifier, Identifier> autoFixIds = new HashMap<>();

    if (!CustomPaintingsMod.knownPaintings.containsKey(player.getUuid())) {
      return unknownPaintings;
    }

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

    player.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return !known.containsKey(entityId);
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

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

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

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

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
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

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
}
