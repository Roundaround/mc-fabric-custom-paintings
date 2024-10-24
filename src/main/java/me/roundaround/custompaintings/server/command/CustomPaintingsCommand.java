package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(CustomPaintingsMod.MOD_ID).then(reloadSub()));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> reloadSub() {
    return CommandManager.literal("reload").requires(CustomPaintingsCommand::isOpPlayer).executes((context) -> {
      ServerPaintingRegistry.getInstance().reloadPaintingPacks(ServerPaintingManager::syncAllDataForAllPlayers);
      return 0;
    });
  }

  private static boolean isOpPlayer(ServerCommandSource source) {
    return source.isExecutedByPlayer() && (source.hasPermissionLevel(2) || source.getServer().isSingleplayer());
  }
}
