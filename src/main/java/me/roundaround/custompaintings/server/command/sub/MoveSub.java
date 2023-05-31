package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MoveSub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("move")
        .then(CommandManager.argument("dir", MoveDirectionArgumentType.direction())
            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                  return execute(context.getSource(),
                      MoveDirectionArgumentType.getDirection(context, "dir"),
                      IntegerArgumentType.getInteger(context, "amount"));
                })));
  }

  private static int execute(ServerCommandSource source, MoveDirection dir, int amount) {
    Optional<PaintingEntity> maybePainting =
        ServerPaintingManager.getPaintingInCrosshair(source.getPlayer());

    if (!maybePainting.isPresent()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.move.none"), false);
      return 0;
    }

    PaintingEntity painting = maybePainting.get();
    int x = painting.getDecorationBlockPos().getX();
    int y = painting.getDecorationBlockPos().getY();
    int z = painting.getDecorationBlockPos().getZ();
    Direction facing = painting.getHorizontalFacing();

    int newX = x;
    int newY = y;
    int newZ = z;

    switch (dir) {
      case UP:
        newY += amount;
        break;
      case DOWN:
        newY -= amount;
        break;
      case LEFT:
        switch (facing) {
          case NORTH:
            newX += amount;
            break;
          case SOUTH:
            newX -= amount;
            break;
          case EAST:
            newZ += amount;
            break;
          case WEST:
            newZ -= amount;
            break;
          default:
            break;
        }
        break;
      case RIGHT:
        switch (facing) {
          case NORTH:
            newX -= amount;
            break;
          case SOUTH:
            newX += amount;
            break;
          case EAST:
            newZ -= amount;
            break;
          case WEST:
            newZ += amount;
            break;
          default:
            break;
        }
        break;
    }

    painting.setPosition(newX, newY, newZ);
    if (!painting.canStayAttached()) {
      painting.setPosition(x, y, z);
      source.sendFeedback(() -> Text.translatable("custompaintings.command.move.invalid"), false);
      return 0;
    }

    source.sendFeedback(() -> Text.translatable("custompaintings.command.move.success",
        dir.toString().toLowerCase(),
        amount), false);
    return 1;
  }

  private static enum MoveDirection implements StringIdentifiable {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right");

    private static final Map<String, MoveDirection> BY_NAME;
    private final String name;

    private MoveDirection(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return this.name;
    }

    public static MoveDirection byName(String name) {
      if (name == null) {
        return null;
      }
      return BY_NAME.get(MoveDirection.sanitize(name));
    }

    private static String sanitize(String name) {
      return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    static {
      BY_NAME = Arrays.stream(MoveDirection.values())
          .collect(Collectors.toMap(f -> MoveDirection.sanitize(f.name), f -> f));
    }
  }

  public static class MoveDirectionArgumentType implements ArgumentType<MoveDirection> {
    private MoveDirectionArgumentType() {
    }

    public static MoveDirectionArgumentType direction() {
      return new MoveDirectionArgumentType();
    }

    public static MoveDirection getDirection(
        CommandContext<ServerCommandSource> context, String name) {
      return context.getArgument(name, MoveDirection.class);
    }

    @Override
    public MoveDirection parse(StringReader reader) throws CommandSyntaxException {
      String string = reader.readUnquotedString();
      MoveDirection direction = MoveDirection.byName(string);
      if (direction == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      }
      return direction;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(getExamples(), builder);
    }

    @Override
    public Collection<String> getExamples() {
      return Arrays.stream(MoveDirection.values())
          .map(MoveDirection::asString)
          .collect(Collectors.toList());
    }
  }
}
