package me.roundaround.custompaintings.server.command.sub;

import java.util.Optional;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SetSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("set")
        .then(CommandManager.literal("id")
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider())
                .executes(context -> {
                  return executeSetId(
                      context.getSource(),
                      IdentifierArgumentType.getIdentifier(context, "id"));
                })))
        .then(CommandManager.literal("width")
            .then(CommandManager.argument("width", IdentifierArgumentType.identifier())
                .executes(context -> {
                  return executeSetWidth(
                      context.getSource(),
                      Integer.parseInt(IdentifierArgumentType.getIdentifier(context, "width").toString()));
                })))
        .then(CommandManager.literal("height")
            .then(CommandManager.argument("height", IdentifierArgumentType.identifier())
                .executes(context -> {
                  return executeSetHeight(
                      context.getSource(),
                      Integer.parseInt(IdentifierArgumentType.getIdentifier(context, "height").toString()));
                })))
        .then(CommandManager.literal("size")
            .then(CommandManager.argument("width", IdentifierArgumentType.identifier())
                .then(CommandManager.argument("height", IdentifierArgumentType.identifier())
                    .executes(context -> {
                      return executeSetSize(
                          context.getSource(),
                          Integer.parseInt(IdentifierArgumentType.getIdentifier(context, "width").toString()),
                          Integer.parseInt(IdentifierArgumentType.getIdentifier(context, "height").toString()));
                    }))));
  }

  private static int executeSetId(ServerCommandSource source, Identifier id) {
    Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    int updated = ServerPaintingManager.setId(
        source.getPlayer(),
        maybePainting.get(),
        id);

    if (updated == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    source.sendFeedback(Text.translatable("custompaintings.command.set.id.success"), false);
    return 1;
  }

  private static int executeSetWidth(ServerCommandSource source, int width) {
    Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    int updated = ServerPaintingManager.setWidth(
        source.getPlayer(),
        maybePainting.get(),
        width);

    if (updated == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    source.sendFeedback(Text.translatable("custompaintings.command.set.width.success"), false);
    return 1;
  }

  private static int executeSetHeight(ServerCommandSource source, int height) {
    Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    int updated = ServerPaintingManager.setHeight(
        source.getPlayer(),
        maybePainting.get(),
        height);

    if (updated == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    source.sendFeedback(Text.translatable("custompaintings.command.set.height.success"), false);
    return 1;
  }

  private static int executeSetSize(ServerCommandSource source, int width, int height) {
    Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    int updated = ServerPaintingManager.setSize(
        source.getPlayer(),
        maybePainting.get(),
        width,
        height);

    if (updated == 0) {
      source.sendFeedback(Text.translatable("custompaintings.command.set.none"), false);
      return 0;
    }

    source.sendFeedback(Text.translatable("custompaintings.command.set.size.success"), false);
    return 1;
  }
}