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
              return executeListMismatched(context.getSource());
            }));

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
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider())
                .executes(context -> {
                  return executeFixSizes(
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

  private static int executeListMismatched(ServerCommandSource source) {
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

            if (knownData == null || data.width() != knownData.width() || data.height() != knownData.height()) {
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
      painting.setCustomData(to, data.width(), data.height());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("command.custompaintings.update.ids.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.update.ids.success", toUpdate.size()), true);
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
          .filter(
              (entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(id) && known.containsKey(id))
          .forEach((entity) -> {
            toUpdate.add((ExpandedPaintingEntity) entity);
          });
    });

    toUpdate.forEach((painting) -> {
      PaintingData data = known.get(id);
      painting.setCustomData(id, data.width(), data.height());
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(Text.translatable("command.custompaintings.update.sizes.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.update.sizes.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }
}
