package me.roundaround.custompaintings.server.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    LiteralArgumentBuilder<ServerCommandSource> updateSub = CommandManager
        .literal("update")
        .then(CommandManager.literal("id")
            .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                    .executes(context -> {
                      return executeUpdateIds(
                          context.getSource(),
                          IdentifierArgumentType.getIdentifier(context, "from"),
                          IdentifierArgumentType.getIdentifier(context, "to"));
                    }))));

    LiteralArgumentBuilder<ServerCommandSource> finalCommand = baseCommand
        .then(listSub)
        .then(removeSub)
        .then(updateSub);

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
        .map(Identifier::toString)
        .collect(Collectors.toList());

    if (ids.size() == 0) {
      source.sendFeedback(Text.translatable("command.custompaintings.list.known.none"), true);
    } else {
      source.sendFeedback(Text.literal(String.join(", ", ids)), true);
    }

    return ids.size();
  }

  private static int executeListMissing(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();
    HashSet<Identifier> known = CustomPaintingsMod.knownPaintings.get(uuid);

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
      HashSet<Identifier> known = CustomPaintingsMod.knownPaintings.get(player.getUuid());

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

  private static int executeUpdateIds(ServerCommandSource source, Identifier from, Identifier to) {
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
      source.sendFeedback(Text.translatable("command.custompaintings.update.none"), true);
    } else {
      source.sendFeedback(Text.translatable("command.custompaintings.update.success", toUpdate.size()), true);
    }

    return toUpdate.size();
  }
}
