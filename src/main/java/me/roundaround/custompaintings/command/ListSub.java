package me.roundaround.custompaintings.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.roundaround.custompaintings.command.argument.PackType;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

import java.util.Collection;

public class ListSub {
  private ListSub() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("list")
        .executes(ListSub::executeAll)
        .then(Commands.literal("enabled").executes(ListSub::executeEnabled))
        .then(Commands.literal("disabled").executes(ListSub::executeDisabled));
  }

  private static int executeAll(CommandContext<CommandSourceStack> context) {
    return executeEnabled(context) + executeDisabled(context);
  }

  private static int executeEnabled(CommandContext<CommandSourceStack> context) {
    return execute(context.getSource(), PackType.ENABLED);
  }

  private static int executeDisabled(CommandContext<CommandSourceStack> context) {
    return execute(context.getSource(), PackType.DISABLED);
  }

  private static int execute(CommandSourceStack source, PackType packType) {
    Collection<PackData> packs = packType.getPacks(ServerPaintingRegistry.getInstance());
    if (packs.isEmpty()) {
      source.sendSuccess(() -> Component.translatable("custompaintings.commands.list." + packType + ".none"), false);
    } else {
      source.sendSuccess(
          () -> Component.translatable("custompaintings.commands.list." + packType + ".success", packs.size(),
              ComponentUtils.formatList(packs, PackData::getInformationText)
          ), false);
    }
    return packs.size();
  }
}
