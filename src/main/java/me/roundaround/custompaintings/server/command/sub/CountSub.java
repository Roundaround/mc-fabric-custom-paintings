package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.suggest.KnownPaintingIdentifierSuggestionProvider;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class CountSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("count")
        .executes(context -> {
          return CountSub.execute(context.getSource());
        })
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .suggests(new KnownPaintingIdentifierSuggestionProvider())
            .executes(context -> {
              return CountSub.execute(context.getSource(),
                  IdentifierArgumentType.getIdentifier(context, "id"));
            }));
  }

  public static int countPaintings(MinecraftServer server, Identifier identifier) {
    int count = 0;

    for (ServerWorld world : server.getWorlds()) {
      count += world.getEntitiesByType(EntityType.PAINTING,
              entity -> entity instanceof ExpandedPaintingEntity)
          .stream()
          .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData()
              .id()
              .equals(identifier))
          .count();
    }

    return count;
  }

  private static int execute(ServerCommandSource source) {
    Optional<PaintingEntity> maybePainting =
        ServerPaintingManager.getPaintingInCrosshair(source.getPlayer());

    if (!maybePainting.isPresent()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.count.none"), false);
      return 0;
    }

    PaintingEntity vanillaPainting = maybePainting.get();
    if (!(vanillaPainting instanceof ExpandedPaintingEntity)) {
      Identifier id = Registries.PAINTING_VARIANT.getId(vanillaPainting.getVariant().value());
      return execute(source, id);
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) vanillaPainting;
    PaintingData paintingData = painting.getCustomData();
    if (paintingData.isEmpty() || paintingData.isVanilla()) {
      Identifier id = Registries.PAINTING_VARIANT.getId(vanillaPainting.getVariant().value());
      return execute(source, id);
    }

    return execute(source, paintingData.id());
  }

  private static int execute(ServerCommandSource source, Identifier identifier) {
    int count = countPaintings(source.getServer(), identifier);

    source.sendFeedback(() -> Text.translatable("custompaintings.command.count.success",
        identifier.toString(),
        count), false);

    return count;
  }
}
