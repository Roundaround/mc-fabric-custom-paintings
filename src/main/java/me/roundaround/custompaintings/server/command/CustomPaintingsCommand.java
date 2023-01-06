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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    LiteralArgumentBuilder<ServerCommandSource> baseCommand = CommandManager.literal(CustomPaintingsMod.MOD_ID)
        .requires(source -> source.hasPermissionLevel(2))
        .requires(source -> source.isExecutedByPlayer());

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
        .then(listSub)
        .then(removeSub)
        .then(fixSub);

    dispatcher.register(finalCommand);
  }

  private static int executeListKnown(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();

    if (!CustomPaintingsMod.knownPaintings.containsKey(uuid)) {
      source.sendFeedback(Text.translatable("command.custompaintings.list.known.none"), true);
      return 0;
    }

    List<String> ids = CustomPaintingsMod.knownPaintings
        .get(uuid)
        .stream()
        .map(PaintingData::id)
        .map(Identifier::toString)
        .collect(Collectors.toList());

    if (ids.size() == 0) {
      source.sendFeedback(Text.translatable("command.custompaintings.list.known.none"), true);
    } else {
      source.sendFeedback(Text.literal(String.join("\n", ids)), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.list.missing.none"), true);
    } else {
      List<String> missingPrintouts = new ArrayList<>();
      missing.forEach((id, count) -> {
        missingPrintouts.add(String.format("%s (%d)", id, count));
      });
      source.sendFeedback(Text.literal(String.join("\n", missingPrintouts)), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.list.mismatched.none"), true);
    } else {
      List<String> mismatchedPrintouts = new ArrayList<>();
      mismatched.forEach((id, count) -> {
        mismatchedPrintouts.add(String.format("%s (%d)", id, count));
      });
      source.sendFeedback(Text.literal(String.join("\n", mismatchedPrintouts)), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.remove.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.remove.success", toRemove.size()), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.fix.ids.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.fix.ids.success", toUpdate.size()), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.fix.sizes.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.fix.sizes.success", toUpdate.size()), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.fix.info.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.fix.info.success", toUpdate.size()), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.fix.everything.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.fix.everything.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }
}
