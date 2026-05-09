package me.roundaround.custompaintings.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.roundaround.custompaintings.command.argument.PackType;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class PackIdSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
  private final PackType packType;

  public PackIdSuggestionProvider() {
    this(PackType.ALL);
  }

  public PackIdSuggestionProvider(PackType packType) {
    this.packType = packType;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder
  ) {
    for (PackData pack : this.packType.getPacks(ServerPaintingRegistry.getInstance())) {
      builder.suggest(pack.id());
    }
    return builder.buildFuture();
  }
}
