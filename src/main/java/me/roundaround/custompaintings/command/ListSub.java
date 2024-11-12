package me.roundaround.custompaintings.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.roundaround.custompaintings.command.argument.PackType;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Collection;

public class ListSub {
  private ListSub() {
  }

  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("list")
        .executes(ListSub::executeAll)
        .then(CommandManager.literal("enabled").executes(ListSub::executeEnabled))
        .then(CommandManager.literal("disabled").executes(ListSub::executeDisabled));
  }

  private static int executeAll(CommandContext<ServerCommandSource> context) {
    return executeEnabled(context) + executeDisabled(context);
  }

  private static int executeEnabled(CommandContext<ServerCommandSource> context) {
    return execute(context.getSource(), PackType.ENABLED);
  }

  private static int executeDisabled(CommandContext<ServerCommandSource> context) {
    return execute(context.getSource(), PackType.DISABLED);
  }

  private static int execute(ServerCommandSource source, PackType packType) {
    Collection<PackData> packs = packType.getPacks(ServerPaintingRegistry.getInstance());
    if (packs.isEmpty()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.list." + packType + ".none"), false);
    } else {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.list." + packType + ".success", packs.size(),
          Texts.join(packs, PackData::getInformationText)
      ), false);
    }
    return packs.size();
  }
}
