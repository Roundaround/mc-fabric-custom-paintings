package me.roundaround.custompaintings.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.roundaround.custompaintings.command.suggestion.PackIdSuggestionProvider;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.StringUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class InfoSub {
  private InfoSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("info")
        .then(CommandManager.argument("id", StringArgumentType.word())
            .suggests(new PackIdSuggestionProvider())
            .executes(InfoSub::execute));
  }

  private static int execute(CommandContext<ServerCommandSource> context) {
    String id = StringArgumentType.getString(context, "id");
    PackData pack = ServerPaintingRegistry.getInstance().getPacks().get(id);
    if (pack == null) {
      context.getSource().sendError(Text.translatable("custompaintings.commands.info.notFound", id));
      return 0;
    }

    ArrayList<Text> lines = new ArrayList<>();
    lines.add(Text.translatable("custompaintings.commands.info.name", pack.name()));
    lines.add(Text.translatable("custompaintings.commands.info.id", pack.id()));
    if (pack.description().isPresent() && !pack.description().get().isBlank()) {
      lines.add(Text.translatable("custompaintings.commands.info.description", pack.description().get()));
    }
    if (!pack.paintings().isEmpty()) {
      lines.add(Text.translatable("custompaintings.commands.info.paintings", pack.paintings().size()));
    }
    if (!pack.migrations().isEmpty()) {
      lines.add(Text.translatable("custompaintings.commands.info.migrations", pack.migrations().size()));
    }
    lines.add(Text.translatable("custompaintings.commands.info.fileSize", StringUtil.formatBytes(pack.fileSize())));

    context.getSource().sendFeedback(
        () -> {
          MutableText message = Text.empty();
          for (int i = 0; i < lines.size(); i++) {
            message.append(lines.get(i));
            if (i < lines.size() - 1) {
              message.append(ScreenTexts.LINE_BREAK);
            }
          }
          return message;
        }, false
    );

    return 1;
  }
}
