package me.roundaround.custompaintings.server.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    LiteralArgumentBuilder<ServerCommandSource> baseCommand = CommandManager.literal(CustomPaintingsMod.MOD_ID)
        .requires(source -> source.hasPermissionLevel(2))
        .requires(source -> source.isExecutedByPlayer());

    LiteralArgumentBuilder<ServerCommandSource> identifySub = CommandManager
        .literal("identify")
        .executes(context -> {
          return executeIdentify(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> listSub = CommandManager
        .literal("list")
        .then(CommandManager.literal("known")
            .executes(context -> {
              return executeListKnown(context.getSource());
            }))
        .then(CommandManager.literal("missing")
            .executes(context -> {
              return executeListMissing(context.getSource());
            }))
        .then(CommandManager.literal("mismatched")
            .executes(context -> {
              return executeListMismatched(context.getSource(), MismatchedCategory.EVERYTHING);
            })
            .then(CommandManager.literal("size")
                .executes(context -> {
                  return executeListMismatched(context.getSource(), MismatchedCategory.SIZE);
                }))
            .then(CommandManager.literal("info")
                .executes(context -> {
                  return executeListMismatched(context.getSource(), MismatchedCategory.INFO);
                }))
            .then(CommandManager.literal("everything")
                .executes(context -> {
                  return executeListMismatched(context.getSource(), MismatchedCategory.EVERYTHING);
                })));

    LiteralArgumentBuilder<ServerCommandSource> removeSub = CommandManager
        .literal("remove")
        .then(CommandManager.literal("missing")
            .executes(context -> {
              return executeRemove(context.getSource(), Optional.empty());
            }))
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .executes(context -> {
              return executeRemove(context.getSource(),
                  Optional.of(IdentifierArgumentType.getIdentifier(context, "id")));
            }));

    LiteralArgumentBuilder<ServerCommandSource> fixSub = CommandManager
        .literal("fix")
        .then(CommandManager.literal("id")
            .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                .suggests(new ExistingPaintingIdentifierSuggestionProvider(true))
                .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                    .suggests(new KnownPaintingIdentifierSuggestionProvider())
                    .executes(context -> {
                      return executeFixIds(
                          context.getSource(),
                          IdentifierArgumentType.getIdentifier(context, "from"),
                          IdentifierArgumentType.getIdentifier(context, "to"));
                    }))))
        .then(CommandManager.literal("size")
            .executes(context -> {
              return executeFixSizes(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixSizes(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })))
        .then(CommandManager.literal("info")
            .executes(context -> {
              return executeFixInfo(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixInfo(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })))
        .then(CommandManager.literal("everything")
            .executes(context -> {
              return executeFixEverything(context.getSource(), null);
            })
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
                .executes(context -> {
                  return executeFixEverything(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })));

    LiteralArgumentBuilder<ServerCommandSource> finalCommand = baseCommand
        .then(identifySub)
        .then(listSub)
        .then(removeSub)
        .then(fixSub);

    dispatcher.register(finalCommand);
  }

  private static int executeIdentify(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();

    Entity camera = player.getCameraEntity();
    double distance = 64;
    Vec3d posVec = camera.getCameraPosVec(0f);
    Vec3d rotationVec = camera.getRotationVec(1f);
    Vec3d targetVec = posVec.add(
        rotationVec.x * distance,
        rotationVec.y * distance,
        rotationVec.z * distance);

    HitResult crosshairTarget = ProjectileUtil.raycast(
        player.getCameraEntity(),
        posVec,
        targetVec,
        camera.getBoundingBox().stretch(rotationVec.multiply(distance)).expand(1.0, 1.0, 1.0),
        entity -> entity instanceof PaintingEntity,
        distance * distance);
    if (!(crosshairTarget instanceof EntityHitResult)) {
      source.sendFeedback(Text.translatable("custompaintings.command.identify.none"), true);
      return 0;
    }

    EntityHitResult entityHitResult = (EntityHitResult) crosshairTarget;
    if (!(entityHitResult.getEntity() instanceof PaintingEntity)) {
      source.sendFeedback(Text.translatable("custompaintings.command.identify.none"), true);
      return 0;
    }

    PaintingEntity vanillaPainting = (PaintingEntity) entityHitResult.getEntity();
    if (!(vanillaPainting instanceof ExpandedPaintingEntity)) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) vanillaPainting;
    PaintingData paintingData = painting.getCustomData();
    if (paintingData.isEmpty() || paintingData.isVanilla()) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ArrayList<Text> lines = new ArrayList<>();
    lines.add(Text.literal(paintingData.id().toString()));

    if (paintingData.hasLabel()) {
      lines.add(paintingData.getLabel());
    }

    lines.add(Text.translatable(
        "custompaintings.painting.dimensions",
        paintingData.width(),
        paintingData.height()));

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));
    if (!known.containsKey(paintingData.id())) {
      lines.add(Text.translatable("custompaintings.command.identify.missing"));
    } else {
      if (isMismatched(paintingData, known.get(paintingData.id()), MismatchedCategory.INFO)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.info"));
      } else if (isMismatched(paintingData, known.get(paintingData.id()), MismatchedCategory.SIZE)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.size"));
      }
    }

    for (Text line : lines) {
      source.sendFeedback(line, true);
    }
    return 1;
  }

  private static void identifyVanillaPainting(ServerCommandSource source, PaintingEntity painting) {
    ArrayList<Text> lines = new ArrayList<>();

    PaintingVariant variant = painting.getVariant().value();
    String id = Registry.PAINTING_VARIANT.getId(variant).toString();

    lines.add(Text.literal(id));
    lines.add(Text.translatable(
        "custompaintings.painting.dimensions",
        variant.getWidth() / 16,
        variant.getHeight() / 16));

    for (Text line : lines) {
      source.sendFeedback(line, true);
    }
  }

  private static int executeListKnown(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();

    if (!CustomPaintingsMod.knownPaintings.containsKey(uuid)) {
      source.sendFeedback(Text.translatable("custompaintings.command.list.known.none"), true);
      return 0;
    }

    List<String> ids = CustomPaintingsMod.knownPaintings
        .get(uuid)
        .stream()
        .map(PaintingData::id)
        .map(Identifier::toString)
        .collect(Collectors.toList());

    if (ids.size() == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.list.known.none"), true);
    } else {
      for (String id : ids) {
        source.sendFeedback(Text.literal(id), true);
      }
    }

    return ids.size();
  }

  private static int executeListMissing(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();
    Set<Identifier> known = CustomPaintingsMod.knownPaintings.get(uuid)
        .stream()
        .map((data) -> data.id())
        .collect(Collectors.toSet());

    HashMap<String, Integer> missing = new HashMap<>();

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .map((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id())
          .filter((id) -> !known.contains(id))
          .forEach((id) -> {
            missing.put(id.toString(), missing.getOrDefault(id.toString(), 0) + 1);
          });
    });

    if (missing.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.list.missing.none"), true);
    } else {
      missing.forEach((id, count) -> {
        source.sendFeedback(Text.literal(String.format("%s (%d)", id, count)), true);
      });
    }

    return missing.size();
  }

  private enum MismatchedCategory {
    SIZE,
    INFO,
    EVERYTHING
  }

  private static boolean isMismatched(PaintingData data, PaintingData knownData, MismatchedCategory category) {
    switch (category) {
      case SIZE:
        return data.width() != knownData.width() || data.height() != knownData.height();
      case INFO:
        return !data.name().equals(knownData.name()) || !data.artist().equals(knownData.artist());
      case EVERYTHING:
        return data.width() != knownData.width() || data.height() != knownData.height()
            || !data.name().equals(knownData.name()) || !data.artist().equals(knownData.artist());
      default:
        return false;
    }
  }

  private static int executeListMismatched(ServerCommandSource source, MismatchedCategory category) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(uuid)
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

    HashMap<String, Integer> mismatched = new HashMap<>();

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .forEach((entity) -> {
            PaintingData data = ((ExpandedPaintingEntity) entity).getCustomData();
            PaintingData knownData = known.get(data.id());

            if (isMismatched(data, knownData, category)) {
              mismatched.put(data.id().toString(), mismatched.getOrDefault(data.id().toString(), 0) + 1);
            }
          });
    });

    if (mismatched.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.list.mismatched.none"), true);
    } else {
      mismatched.forEach((id, count) -> {
        source.sendFeedback(Text.literal(String.format("%s (%d)", id, count)), true);
      });
    }

    return mismatched.size();
  }

  private static int executeRemove(ServerCommandSource source, Optional<Identifier> idToRemove) {
    ServerPlayerEntity player = source.getPlayer();
    ArrayList<PaintingEntity> toRemove = new ArrayList<>();

    if (idToRemove.isPresent()) {
      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(idToRemove.get()))
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    } else {
      Set<Identifier> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
          .stream()
          .map((data) -> data.id())
          .collect(Collectors.toSet());

      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> {
              Identifier id = ((ExpandedPaintingEntity) entity).getCustomData().id();
              return !known.contains(id);
            })
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    }

    toRemove.forEach((painting) -> {
      painting.damage(DamageSource.player(player), 0f);
    });

    if (toRemove.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.success", toRemove.size()), true);
    }

    return toRemove.size();
  }

  private static int executeFixIds(ServerCommandSource source, Identifier from, Identifier to) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(from))
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData data = painting.getCustomData();
      painting.setCustomData(
          to,
          data.index(),
          data.width(),
          data.height(),
          data.name(),
          data.artist());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.ids.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.ids.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixSizes(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(source.getPlayer().getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          currentData.index(),
          knownData.width(),
          knownData.height(),
          currentData.name(),
          currentData.artist());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.sizes.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.sizes.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixInfo(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(source.getPlayer().getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          knownData.index(),
          currentData.width(),
          currentData.height(),
          knownData.name(),
          knownData.artist());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.info.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.info.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }

  private static int executeFixEverything(ServerCommandSource source, Identifier id) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(source.getPlayer().getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> {
            Identifier entityId = ((ExpandedPaintingEntity) entity).getCustomData().id();
            return (id == null || entityId.equals(id)) && known.containsKey(entityId);
          })
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(
          currentData.id(),
          knownData.index(),
          knownData.width(),
          knownData.height(),
          knownData.name(),
          knownData.artist());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.everything.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.fix.everything.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }
}
