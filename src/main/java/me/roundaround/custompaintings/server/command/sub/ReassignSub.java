package me.roundaround.custompaintings.server.command.sub;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.ExistingPaintingIdentifierSuggestionProvider;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ReassignSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager
        .literal("reassign")
        .then(CommandManager.literal("all")
            .then(CommandManager.literal("idonly")
                .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                    .suggests(new ExistingPaintingIdentifierSuggestionProvider())
                    .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                        .suggests(new KnownPaintingIdentifierSuggestionProvider())
                        .executes(context -> {
                          return executeIdOnly(
                              context.getSource(),
                              IdentifierArgumentType.getIdentifier(context, "from"),
                              IdentifierArgumentType.getIdentifier(context, "to"));
                        }))))
            .then(CommandManager.argument("from", IdentifierArgumentType.identifier())
                .suggests(new ExistingPaintingIdentifierSuggestionProvider())
                .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                    .suggests(new KnownPaintingIdentifierSuggestionProvider())
                    .executes(context -> {
                      return execute(
                          context.getSource(),
                          IdentifierArgumentType.getIdentifier(context, "from"),
                          IdentifierArgumentType.getIdentifier(context, "to"));
                    }))))
        .then(CommandManager.literal("idonly")
            .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
                .suggests(new KnownPaintingIdentifierSuggestionProvider())
                .executes(context -> {
                  return executeIdOnly(
                      context.getSource(),
                      null,
                      IdentifierArgumentType.getIdentifier(context, "to"));
                })))
        .then(CommandManager.argument("to", IdentifierArgumentType.identifier())
            .suggests(new KnownPaintingIdentifierSuggestionProvider())
            .executes(context -> {
              return execute(
                  context.getSource(),
                  null,
                  IdentifierArgumentType.getIdentifier(context, "to"));
            }));
  }

  private static int execute(ServerCommandSource source, Identifier from, Identifier to) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = ServerPaintingManager.getKnownPaintings(source.getPlayer());

    if (!known.containsKey(to)) {
      source.sendFeedback(Text.translatable("custompaintings.command.reassign.unknown", to), false);
      return 0;
    }

    if (from == null) {
      Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

      if (maybePainting.isEmpty()) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }

      PaintingEntity painting = maybePainting.get();
      if (!(painting instanceof ExpandedPaintingEntity)) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }

      toUpdate.add((ExpandedPaintingEntity) painting);
    } else {
      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(from))
            .forEach((entity) -> {
              toUpdate.add((ExpandedPaintingEntity) entity);
            });
      });

      if (toUpdate.isEmpty()) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }
    }

    PaintingData knownData = known.get(to);
    toUpdate.forEach((painting) -> {
      painting.setCustomData(
          knownData.id(),
          knownData.index(),
          knownData.width(),
          knownData.height(),
          knownData.name(),
          knownData.artist(),
          knownData.isVanilla());

      if (knownData.isVanilla()) {
        painting.setVariant(knownData.id());
      }
    });

    source.sendFeedback(Text.translatable("custompaintings.command.reassign.success", toUpdate.size()), false);
    return toUpdate.size();
  }

  private static int executeIdOnly(ServerCommandSource source, Identifier from, Identifier to) {
    ArrayList<ExpandedPaintingEntity> toUpdate = new ArrayList<>();

    if (from == null) {
      Optional<PaintingEntity> maybePainting = IdentifySub.getPaintingInCrosshair(source.getPlayer());

      if (maybePainting.isEmpty()) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }

      PaintingEntity painting = maybePainting.get();
      if (!(painting instanceof ExpandedPaintingEntity)) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }

      toUpdate.add((ExpandedPaintingEntity) painting);
    } else {
      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(from))
            .forEach((entity) -> {
              toUpdate.add((ExpandedPaintingEntity) entity);
            });
      });

      if (toUpdate.isEmpty()) {
        source.sendFeedback(Text.translatable("custompaintings.command.reassign.none"), false);
        return 0;
      }
    }

    toUpdate.forEach((painting) -> {
      PaintingData data = painting.getCustomData();
      painting.setCustomData(
          to,
          data.index(),
          data.width(),
          data.height(),
          data.name(),
          data.artist(),
          data.isVanilla());

      if (data.isVanilla()) {
        painting.setVariant(to);
      }
    });

    source.sendFeedback(Text.translatable("custompaintings.command.reassign.success", toUpdate.size()), false);
    return toUpdate.size();
  }
}
