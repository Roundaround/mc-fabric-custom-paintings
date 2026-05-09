package me.roundaround.custompaintings.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.roundaround.custompaintings.command.suggestion.PackIdSuggestionProvider;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.StringUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class InfoSub {
  private InfoSub() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("info")
        .then(Commands.argument("id", StringArgumentType.word())
            .suggests(new PackIdSuggestionProvider())
            .executes(InfoSub::execute));
  }

  private static int execute(CommandContext<CommandSourceStack> context) {
    String id = StringArgumentType.getString(context, "id");
    PackData pack = ServerPaintingRegistry.getInstance().getPacks().get(id);
    if (pack == null) {
      context.getSource().sendFailure(Component.translatable("custompaintings.commands.info.notFound", id));
      return 0;
    }

    ArrayList<Component> lines = new ArrayList<>();
    lines.add(Component.translatable("custompaintings.commands.info.name", pack.name()));
    lines.add(Component.translatable("custompaintings.commands.info.id", pack.id()));
    if (pack.description().isPresent() && !pack.description().get().isBlank()) {
      lines.add(Component.translatable("custompaintings.commands.info.description", pack.description().get()));
    }
    if (!pack.paintings().isEmpty()) {
      lines.add(Component.translatable("custompaintings.commands.info.paintings", pack.paintings().size()));
    }
    if (!pack.migrations().isEmpty()) {
      lines.add(Component.translatable("custompaintings.commands.info.migrations", pack.migrations().size()));
    }
    lines.add(Component.translatable("custompaintings.commands.info.fileSize", StringUtil.formatBytes(pack.fileSize())));

    context.getSource().sendSuccess(
        () -> {
          MutableComponent message = Component.empty();
          for (int i = 0; i < lines.size(); i++) {
            message.append(lines.get(i));
            if (i < lines.size() - 1) {
              message.append(CommonComponents.NEW_LINE);
            }
          }
          return message;
        }, false
    );

    return 1;
  }
}
