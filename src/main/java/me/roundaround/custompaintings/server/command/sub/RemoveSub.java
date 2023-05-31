package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class RemoveSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("remove")
        .executes(context -> {
          return executeTargeted(context.getSource());
        })
        .then(CommandManager.literal("unknown").executes(context -> {
          return execute(context.getSource());
        }))
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .suggests(new KnownPaintingIdentifierSuggestionProvider())
            .executes(context -> {
              return execute(context.getSource(),
                  IdentifierArgumentType.getIdentifier(context, "id"));
            }));
  }

  private static int executeTargeted(ServerCommandSource source) {
    Optional<PaintingEntity> maybePainting =
        ServerPaintingManager.getPaintingInCrosshair(source.getPlayer());

    if (!maybePainting.isPresent()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.none"), false);
      return 0;
    }

    int removed = ServerPaintingManager.removePainting(source.getPlayer(), maybePainting.get());

    if (removed == 0) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.none"), false);
    } else {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.success",
          removed), false);
    }

    return removed;
  }

  private static int execute(ServerCommandSource source) {
    int removed = ServerPaintingManager.removeUnknownPaintings(source.getPlayer());

    if (removed == 0) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.none"), false);
    } else {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.success",
          removed), false);
    }

    return removed;
  }

  private static int execute(ServerCommandSource source, Identifier id) {
    int removed = ServerPaintingManager.removePaintings(source.getPlayer(), id);

    if (removed == 0) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.none"), false);
    } else {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.remove.success",
          removed), false);
    }

    return removed;
  }
}
