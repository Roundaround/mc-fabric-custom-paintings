package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.OldServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class FixSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("fix").executes(context -> {
      return executeTargeted(context.getSource());
    }).then(CommandManager.literal("all")
        .executes(context -> {
          return execute(context.getSource(), null);
        })
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .suggests(new KnownPaintingIdentifierSuggestionProvider(true))
            .executes(context -> {
              return execute(context.getSource(), IdentifierArgumentType.getIdentifier(context, "id"));
            })));
  }

  private static int executeTargeted(ServerCommandSource source) {
    Optional<PaintingEntity> maybePainting = OldServerPaintingManager.getPaintingInCrosshair(source.getPlayer());

    if (maybePainting.isEmpty()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.none"), false);
      return 0;
    }

    PaintingEntity painting = maybePainting.get();
    Map<Identifier, PaintingData> known = OldServerPaintingManager.getKnownPaintings(source.getPlayer());
    PaintingData currentData = painting.getCustomData();

    if (!known.containsKey(currentData.id())) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.unknown", currentData.id()), false);
      return 0;
    }

    source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.success", 1), false);
    painting.setCustomData(known.get(currentData.id()));
    return 1;
  }

  private static int execute(ServerCommandSource source, Identifier id) {
    ArrayList<PaintingEntity> toUpdate = new ArrayList<>();
    Map<Identifier, PaintingData> known = OldServerPaintingManager.getKnownPaintings(source.getPlayer());

    if (id != null && !known.containsKey(id)) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.unknown", id), false);
      return 0;
    }

    source.getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, Entity::isAlive).stream().filter((painting) -> {
        Identifier entityId = painting.getCustomData().id();
        return (id == null || entityId.equals(id)) && known.containsKey(entityId);
      }).forEach(toUpdate::add);
    });

    toUpdate.forEach((painting) -> {
      PaintingData currentData = painting.getCustomData();
      PaintingData knownData = known.get(currentData.id());
      painting.setCustomData(currentData.id(), knownData.width(), knownData.height(), knownData.name(),
          knownData.artist(), knownData.isVanilla()
      );
    });

    if (toUpdate.isEmpty()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.none"), false);
    } else {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.fix.success", toUpdate.size()), false);
    }

    return toUpdate.size();
  }
}
