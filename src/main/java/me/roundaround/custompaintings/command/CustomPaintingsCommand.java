package me.roundaround.custompaintings.command;

import com.mojang.brigadier.CommandDispatcher;
import me.roundaround.custompaintings.generated.Constants;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CustomPaintingsCommand {
  private CustomPaintingsCommand() {
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(Constants.MOD_ID)
        .then(OpenSub.build())
        .then(ReloadSub.build())
        .then(EnableSub.build())
        .then(DisableSub.build())
        .then(InfoSub.build())
        .then(ListSub.build()));
  }
}
