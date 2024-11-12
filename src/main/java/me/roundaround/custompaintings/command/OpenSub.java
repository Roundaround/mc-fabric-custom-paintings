package me.roundaround.custompaintings.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class OpenSub {
  private OpenSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("open").requires(ServerCommandSource::isExecutedByPlayer).executes((context) -> {
      ServerNetworking.sendOpenMenuPacket(context.getSource().getPlayer());
      return 1;
    });
  }
}
