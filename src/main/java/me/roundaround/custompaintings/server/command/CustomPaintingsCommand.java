package me.roundaround.custompaintings.server.command;

import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
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

    LiteralArgumentBuilder<ServerCommandSource> finalCommand = baseCommand
        .then(listKnownSub);

    dispatcher.register(finalCommand);
  }

  private static int executeListKnown(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    UUID uuid = player.getUuid();

    if (!CustomPaintingsMod.knownPaintings.containsKey(uuid)) {
      source.sendFeedback(Text.translatable("command.custompaintings.listknown.none"), true);
      return 0;
    }

    String ids = CustomPaintingsMod.knownPaintings
        .get(uuid)
        .stream()
        .map(Identifier::toString)
        .collect(Collectors.joining(", "));
    source.sendFeedback(Text.literal(ids), true);
    return 1;
  }
}
