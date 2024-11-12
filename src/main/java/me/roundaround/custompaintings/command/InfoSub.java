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
      // TODO: i18n
      context.getSource().sendFeedback(() -> Text.of(String.format("Pack \"%s\" not found", id)), false);
      return 0;
    }

    ArrayList<String> lines = new ArrayList<>();
    // TODO: i18n
    lines.add(String.format("Name: %s", pack.name()));
    // TODO: i18n
    lines.add(String.format("ID: %s", pack.id()));
    if (pack.description() != null && !pack.description().isBlank()) {
      // TODO: i18n
      lines.add(String.format("Description: %s", pack.description()));
    }
    if (!pack.paintings().isEmpty()) {
      // TODO: i18n
      lines.add(String.format("Paintings: %s", pack.paintings().size()));
    }
    if (!pack.migrations().isEmpty()) {
      // TODO: i18n
      lines.add(String.format("Migrations: %s", pack.migrations().size()));
    }
    // TODO: i18n
    lines.add(String.format("File Size: %s", StringUtil.formatBytes(pack.fileSize())));

    context.getSource().sendFeedback(() -> {
      MutableText message = Text.empty();
      for (int i = 0; i < lines.size(); i++) {
        message.append(lines.get(i));
        if (i < lines.size() - 1) {
          message.append(ScreenTexts.LINE_BREAK);
        }
      }
      return message;
    }, false);

    return 1;
  }
}
