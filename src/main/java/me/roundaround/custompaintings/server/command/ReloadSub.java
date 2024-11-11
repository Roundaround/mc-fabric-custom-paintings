package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ReloadSub {
  private ReloadSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("reload")
        .requires((source) -> source.hasPermissionLevel(3))
        .executes((context) -> execute(context.getSource()));
  }

  public static int execute(ServerCommandSource source) {
    source.sendFeedback(() -> Text.translatable("custompaintings.command.reload.begin"), true);
    ServerPaintingRegistry.getInstance().reloadPaintingPacks((server) -> {
      ServerPaintingManager.syncAllDataForAllPlayers(server);
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reload.end"), true);
    });
    return 1;
  }
}
