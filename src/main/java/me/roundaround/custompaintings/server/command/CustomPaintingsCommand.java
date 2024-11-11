package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.CommandDispatcher;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CustomPaintingsCommand {
  private CustomPaintingsCommand() {
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(CustomPaintingsMod.MOD_ID)
        .then(OpenSub.build())
        .then(ReloadSub.build())
        .then(EnableSub.build())
        .then(DisableSub.build()));
  }
}
