package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(CustomPaintingsMod.MOD_ID).then(reloadSub()));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> reloadSub() {
    return CommandManager.literal("reload").requires((source) -> source.hasPermissionLevel(3)).executes((context) -> {
      ServerPaintingRegistry.getInstance().reloadPaintingPacks(ServerPaintingManager::syncAllDataForAllPlayers);
      context.getSource().sendFeedback(() -> Text.translatable("custompaintings.reload.message"), true);
      return 1;
    });
  }
}
