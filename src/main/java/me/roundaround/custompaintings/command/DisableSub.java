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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DisableSub {
  private DisableSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("disable")
        .requires((source) -> source.hasPermissionLevel(3))
        .then(CommandManager.argument("id", StringArgumentType.word())
            .suggests(new PackIdSuggestionProvider(PackType.ENABLED))
            .executes(DisableSub::execute));
  }

  private static int execute(CommandContext<ServerCommandSource> context) {
    String id = StringArgumentType.getString(context, "id");
    PackData pack = ServerPaintingRegistry.getInstance().getPacks().get(id);
    if (pack == null) {
      // TODO: i18n
      context.getSource().sendFeedback(() -> Text.of(String.format("Pack \"%s\" not found", id)), false);
      return 0;
    }

    if (ServerInfo.getInstance().markPackDisabled(pack.packFileUid())) {
      // TODO: i18n
      context.getSource().sendFeedback(() -> Text.of(String.format("Pack \"%s\" disabled. Reloading packs", id)), true);
      ServerPaintingRegistry.getInstance().reloadPaintingPacks((server) -> {
        ServerPaintingManager.syncAllDataForAllPlayers(server);
        // TODO: i18n
        context.getSource().sendFeedback(() -> Text.of("Packs reloaded"), true);
      });
      return 1;
    }

    // TODO: i18n
    context.getSource().sendFeedback(() -> Text.of(String.format("Pack \"%s\" already disabled", id)), false);
    return 0;
  }
}
