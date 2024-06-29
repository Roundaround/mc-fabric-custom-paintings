package me.roundaround.custompaintings.server.command.suggest;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ExistingPaintingIdentifierSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
  private final boolean missingOnly;

  public ExistingPaintingIdentifierSuggestionProvider() {
    this(false);
  }

  public ExistingPaintingIdentifierSuggestionProvider(boolean missingOnly) {
    this.missingOnly = missingOnly;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<ServerCommandSource> context, SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    Set<Identifier> knownIds = CustomPaintingsMod.knownPaintings.getOrDefault(
            context.getSource().getPlayer().getUuid(), new HashSet<>())
        .stream()
        .map(PaintingData::id)
        .collect(Collectors.toSet());

    context.getSource().getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, (entity) -> true).forEach((painting) -> {
        if (isVanillaPainting(painting)) {
          if (!this.missingOnly) {
            builder.suggest(Registries.PAINTING_VARIANT.getId(painting.getVariant().value()).toString());
          }
          return;
        }
        PaintingData data = painting.getCustomData();
        if (!this.missingOnly || !knownIds.contains(data.id())) {
          builder.suggest(data.id().toString());
        }
      });
    });

    return builder.buildFuture();
  }

  private static boolean isVanillaPainting(PaintingEntity painting) {
    PaintingData data = painting.getCustomData();
    return data.isEmpty() || data.isVanilla();
  }
}
