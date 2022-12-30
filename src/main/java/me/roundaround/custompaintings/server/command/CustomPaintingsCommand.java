package me.roundaround.custompaintings.server.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import net.minecraft.entity.EntityType;
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

    LiteralArgumentBuilder<ServerCommandSource> listKnownSub = CommandManager
        .literal("listknown")
        .executes(context -> {
          return executeListKnown(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> listMissingSub = CommandManager
        .literal("listmissing")
        .executes(context -> {
          return executeListMissing(context.getSource());
        });

    LiteralArgumentBuilder<ServerCommandSource> finalCommand = baseCommand
        .then(listKnownSub)
        .then(listMissingSub);

    dispatcher.register(finalCommand);
  }

  private static int executeListKnown(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();

    if (!CustomPaintingsMod.knownPaintings.containsKey(uuid)) {
      source.sendFeedback(Text.translatable("command.custompaintings.listknown.none"), true);
      return 0;
    }

    List<String> ids = CustomPaintingsMod.knownPaintings
        .get(uuid)
        .stream()
        .map(Identifier::toString)
        .collect(Collectors.toList());

    if (ids.size() == 0) {
      source.sendFeedback(Text.translatable("command.custompaintings.listknown.none"), true);
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
      source.sendFeedback(Text.translatable("command.custompaintings.listmissing.none"), true);
    } else {
      List<String> missingPrintouts = new ArrayList<>();
      missing.forEach((id, count) -> {
        missingPrintouts.add(String.format("%s (%d)", id, count));
      });
      source.sendFeedback(Text.literal(String.join("\n", missingPrintouts)), true);
    }

    return missing.size();
  }
}
