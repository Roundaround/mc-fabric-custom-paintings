package me.roundaround.custompaintings.server.command.suggest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
      CommandContext<ServerCommandSource> context,
      SuggestionsBuilder builder) throws CommandSyntaxException {
    Set<Identifier> knownIds = CustomPaintingsMod.knownPaintings
        .getOrDefault(context.getSource().getPlayer().getUuid(), new HashSet<>())
        .stream()
        .map((data) -> data.id())
        .collect(Collectors.toSet());

    context.getSource().getServer().getWorlds().forEach((world) -> {
      world.getEntitiesByType(EntityType.PAINTING, (entity) -> true).forEach((entity) -> {
        if (isVanillaPainting(entity)) {
          if (!this.missingOnly) {
            builder.suggest(Registries.PAINTING_VARIANT.getId(entity.getVariant().value()).toString());
          }
          return;
        }
        PaintingData data = ((ExpandedPaintingEntity) entity).getCustomData();
        if (!this.missingOnly || !knownIds.contains(data.id())) {
          builder.suggest(data.id().toString());
        }
      });
    });

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
