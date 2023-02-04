package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ManageSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager
        .literal("manage")
        .executes(context -> {
          return execute(context.getSource());
        });
  }

  private static int execute(ServerCommandSource source) {
    ServerNetworking.sendOpenManageScreenPacket(source.getPlayer());
    return 1;
  }
}
