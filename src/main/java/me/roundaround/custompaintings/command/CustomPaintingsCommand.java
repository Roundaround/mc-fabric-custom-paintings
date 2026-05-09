package me.roundaround.custompaintings.command;

import com.mojang.brigadier.CommandDispatcher;
import me.roundaround.custompaintings.generated.Constants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

public class CustomPaintingsCommand {
  private CustomPaintingsCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal(Constants.MOD_ID)
        .then(OpenSub.build())
        .then(ReloadSub.build())
        .then(EnableSub.build())
        .then(DisableSub.build())
        .then(InfoSub.build())
        .then(ListSub.build()));
  }
}
