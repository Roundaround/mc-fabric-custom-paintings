package me.roundaround.custompaintings.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

public class OpenSub {
  private OpenSub() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("open").requires(CommandSourceStack::isPlayer).executes((context) -> {
      ServerNetworking.sendOpenMenuPacket(context.getSource().getPlayer());
      return 1;
    });
  }
}
