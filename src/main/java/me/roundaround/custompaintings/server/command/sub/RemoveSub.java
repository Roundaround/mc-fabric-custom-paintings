package me.roundaround.custompaintings.server.command.sub;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RemoveSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager
        .literal("remove")
        .then(CommandManager.literal("unknown")
            .executes(context -> {
              return execute(context.getSource(), Optional.empty());
            }))
        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
            .executes(context -> {
              return execute(context.getSource(),
                  Optional.of(IdentifierArgumentType.getIdentifier(context, "id")));
            }));
  }

  private static int execute(ServerCommandSource source, Optional<Identifier> idToRemove) {
    ServerPlayerEntity player = source.getPlayer();
    ArrayList<PaintingEntity> toRemove = new ArrayList<>();

    if (idToRemove.isPresent()) {
      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> ((ExpandedPaintingEntity) entity).getCustomData().id().equals(idToRemove.get()))
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    } else {
      Set<Identifier> known = ServerPaintingManager.getKnownPaintings(player).keySet();

      source.getServer().getWorlds().forEach((world) -> {
        world.getEntitiesByType(EntityType.PAINTING, entity -> entity instanceof ExpandedPaintingEntity)
            .stream()
            .filter((entity) -> {
              Identifier id = ((ExpandedPaintingEntity) entity).getCustomData().id();
              return !known.contains(id);
            })
            .forEach((entity) -> {
              toRemove.add((PaintingEntity) entity);
            });
      });
    }

    toRemove.forEach((painting) -> {
      painting.damage(DamageSource.player(player), 0f);
    });

    if (toRemove.isEmpty()) {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.none"), true);
    } else {
      source.sendFeedback(Text.translatable("custompaintings.command.remove.success", toRemove.size()), true);
    }

    return toRemove.size();
  }
}
