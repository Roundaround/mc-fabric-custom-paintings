package me.roundaround.custompaintings.server.command.suggest;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

public class KnownPaintingIdentifierSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
  public final boolean existingOnly;

  public KnownPaintingIdentifierSuggestionProvider() {
    this(false);
  }

  public KnownPaintingIdentifierSuggestionProvider(boolean existingOnly) {
    this.existingOnly = existingOnly;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<ServerCommandSource> context,
      SuggestionsBuilder builder) throws CommandSyntaxException {
    UUID playerId = context.getSource().getPlayer().getUuid();
    HashSet<Identifier> existing = new HashSet<>();

    if (this.existingOnly) {
      context.getSource().getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, (entity) -> true).forEach((entity) -> {
          if (isVanillaPainting(entity)) {
            existing.add(Registries.PAINTING_VARIANT.getId(entity.getVariant().value()));
            return;
          }
          existing.add(((ExpandedPaintingEntity) entity).getCustomData().id());
        });
      });
    }

    Registries.PAINTING_VARIANT.forEach((paintingVariant) -> {
      Identifier id = Registries.PAINTING_VARIANT.getId(paintingVariant);
      if (this.existingOnly && !existing.contains(id)) {
        return;
      }
      builder.suggest(id.toString());
    });

    HashSet<PaintingData> knownPaintings = CustomPaintingsMod.knownPaintings.get(playerId);
    if (knownPaintings != null) {
      knownPaintings.forEach((data) -> {
        Identifier id = data.id();
        if (this.existingOnly && !existing.contains(id)) {
          return;
        }
        builder.suggest(id.toString());
      });
    }

    return builder.buildFuture();
  }

  private static boolean isVanillaPainting(PaintingEntity painting) {
    if (!(painting instanceof ExpandedPaintingEntity)) {
      return true;
    }

    PaintingData data = ((ExpandedPaintingEntity) painting).getCustomData();
    if (data == null || data.isEmpty() || data.isVanilla()) {
      return true;
    }

    return false;
  }
}
