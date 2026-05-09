package me.roundaround.custompaintings.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadSub {
  private ReloadSub() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("reload")
        .requires((source) -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
        .executes((context) -> execute(context.getSource()));
  }

  public static int execute(CommandSourceStack source) {
    source.sendSuccess(() -> Component.translatable("custompaintings.commands.reload.begin"), true);
    ServerPaintingRegistry.getInstance().reloadPaintingPacks((server) -> {
      ServerPaintingManager.syncAllDataForAllPlayers(server);
      source.sendSuccess(() -> Component.translatable("custompaintings.commands.reload.end"), true);
    });
    return 1;
  }
}
