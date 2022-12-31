package me.roundaround.custompaintings.server.command;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class KnownPaintingIdentifierSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<ServerCommandSource> context,
      SuggestionsBuilder builder) throws CommandSyntaxException {
    UUID playerId = context.getSource().getPlayer().getUuid();

    Registry.PAINTING_VARIANT.forEach((paintingVariant) -> {
      builder.suggest(Registry.PAINTING_VARIANT.getId(paintingVariant).toString());
    });

    Set<Identifier> knownIds = CustomPaintingsMod.knownPaintings.get(playerId)
        .stream()
        .map((data) -> data.id())
        .collect(Collectors.toSet());
    if (knownIds != null) {
      knownIds.forEach((id) -> {
        builder.suggest(id.toString());
      });
    }

    return builder.buildFuture();
  }
}
