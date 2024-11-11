package me.roundaround.custompaintings.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class DisableSub {
  private DisableSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("disable")
        .requires((source) -> source.hasPermissionLevel(3))
        .then(CommandManager.argument("id", StringArgumentType.word())
            .suggests(new PackIdSuggestionProvider())
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

  private static class PackIdSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<ServerCommandSource> context, SuggestionsBuilder builder
    ) {
      for (PackData pack : ServerPaintingRegistry.getInstance().getActivePacks()) {
        builder.suggest(pack.id());
      }
      return builder.buildFuture();
    }
  }
}
