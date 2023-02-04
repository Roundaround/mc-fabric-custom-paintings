package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.CommandDispatcher;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.command.sub.CountSub;
import me.roundaround.custompaintings.server.command.sub.FixSub;
import me.roundaround.custompaintings.server.command.sub.IdentifySub;
import me.roundaround.custompaintings.server.command.sub.ManageSub;
import me.roundaround.custompaintings.server.command.sub.MoveSub;
import me.roundaround.custompaintings.server.command.sub.ReassignSub;
import me.roundaround.custompaintings.server.command.sub.RemoveSub;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CustomPaintingsCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(CustomPaintingsMod.MOD_ID)
        .requires(source -> {
          return source.isExecutedByPlayer()
              && (source.hasPermissionLevel(2) || source.getServer().isSingleplayer());
        })
        .then(IdentifySub.build())
        .then(CountSub.build())
        .then(RemoveSub.build())
        .then(ReassignSub.build())
        .then(FixSub.build())
        .then(ManageSub.build())
        .then(MoveSub.build()));
  }
}
