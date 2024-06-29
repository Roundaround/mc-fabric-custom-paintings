package me.roundaround.custompaintings.server.command.suggest;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
      CommandContext<ServerCommandSource> context, SuggestionsBuilder builder
  ) {
    UUID playerId = context.getSource().getPlayer().getUuid();
    HashSet<Identifier> existing = new HashSet<>();

    if (this.existingOnly) {
      context.getSource().getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, (entity) -> true).forEach((painting) -> {
          if (isVanillaPainting(painting)) {
            existing.add(Registries.PAINTING_VARIANT.getId(painting.getVariant().value()));
            return;
          }
          existing.add(painting.getCustomData().id());
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
    PaintingData data = painting.getCustomData();
    return data.isEmpty() || data.isVanilla();
  }
}
