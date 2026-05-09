package me.roundaround.custompaintings.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.roundaround.custompaintings.command.argument.PackType;
import me.roundaround.custompaintings.command.suggestion.PackIdSuggestionProvider;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class DisableSub {
  private DisableSub() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("disable")
        .requires((source) -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
        .then(Commands.argument("id", StringArgumentType.word())
            .suggests(new PackIdSuggestionProvider(PackType.ENABLED))
            .executes(DisableSub::execute));
  }

  private static int execute(CommandContext<CommandSourceStack> context) {
    String id = StringArgumentType.getString(context, "id");
    PackData pack = ServerPaintingRegistry.getInstance().getPacks().get(id);
    if (pack == null) {
      context.getSource().sendFailure(Component.translatable("custompaintings.commands.disable.notFound", id));
      return 0;
    }

    if (ServerInfo.getInstance().markPackDisabled(pack.packFileUid())) {
      context.getSource().sendSuccess(() -> Component.translatable("custompaintings.commands.disable.disabled", id), true);
      ServerPaintingRegistry.getInstance().reloadPaintingPacks((server) -> {
        ServerPaintingManager.syncAllDataForAllPlayers(server);
        context.getSource().sendSuccess(() -> Component.translatable("custompaintings.commands.disable.reloaded"), true);
      });
      return 1;
    }

    context.getSource()
        .sendSuccess(() -> Component.translatable("custompaintings.commands.disable.alreadyDisabled", id), false);
    return 0;
  }
}
