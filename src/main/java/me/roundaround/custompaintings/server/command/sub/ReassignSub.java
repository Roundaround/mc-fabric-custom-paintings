package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.ExistingPaintingIdentifierSuggestionProvider;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ReassignSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("reassign")
        .then(CommandManager.literal("all")
            .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                .suggests(new ExistingPaintingIdentifierSuggestionProvider())
                .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                    .suggests(new KnownPaintingIdentifierSuggestionProvider())
                    .executes(context -> {
                      return execute(context.getSource(),
                          IdentifierArgumentType.getIdentifier(context, "from"),
                          IdentifierArgumentType.getIdentifier(context, "to"));
                    }))))
        .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
            .suggests(new KnownPaintingIdentifierSuggestionProvider())
            .executes(context -> {
              return execute(context.getSource(),
                  IdentifierArgumentType.getIdentifier(context, "to"));
            }));
  }

  private static int execute(ServerCommandSource source, Identifier id) {
    Optional<PaintingEntity> maybePainting =
        ServerPaintingManager.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.none"), false);
      return 0;
    }

    int updated = ServerPaintingManager.reassign(source.getPlayer(), maybePainting.get(), id);

    if (updated == -1) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.unknown", id),
          false);
      return 0;
    }

    if (updated == 0) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.none"), false);
      return 0;
    }

    source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.success",
        updated), false);
    return updated;
  }

  private static int execute(ServerCommandSource source, Identifier from, Identifier to) {
    int updated = ServerPaintingManager.reassign(source.getPlayer(), from, to);

    if (updated == -1) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.unknown", to),
          false);
      return 0;
    }

    if (updated == 0) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.none"), false);
      return 0;
    }

    source.sendFeedback(() -> Text.translatable("custompaintings.command.reassign.success",
        updated), false);
    return updated;
  }
}
