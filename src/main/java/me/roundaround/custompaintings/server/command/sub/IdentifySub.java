package me.roundaround.custompaintings.server.command.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IdentifySub {
  public static LiteralArgumentBuilder<ServerCommandSource> build() {
    return CommandManager.literal("identify").executes(context -> {
      return execute(context.getSource());
    });
  }

  private static int execute(ServerCommandSource source) {
    ServerPlayerEntity player = source.getPlayer();
    Optional<PaintingEntity> maybePainting = ServerPaintingManager.getPaintingInCrosshair(player);

    if (!maybePainting.isPresent()) {
      source.sendFeedback(() -> Text.translatable("custompaintings.command.identify.none"), false);
      return 0;
    }

    PaintingEntity vanillaPainting = maybePainting.get();
    if (!(vanillaPainting instanceof ExpandedPaintingEntity)) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ExpandedPaintingEntity painting = (ExpandedPaintingEntity) vanillaPainting;
    PaintingData paintingData = painting.getCustomData();
    if (paintingData.isEmpty() || paintingData.isVanilla()) {
      identifyVanillaPainting(source, vanillaPainting);
      return 1;
    }

    ArrayList<Text> lines = new ArrayList<>();
    lines.add(Text.literal(paintingData.id().toString()));

    if (paintingData.hasLabel()) {
      lines.add(paintingData.getLabel());
    }

    lines.add(Text.translatable("custompaintings.painting.dimensions",
        paintingData.width(),
        paintingData.height()));

    int count = CountSub.countPaintings(source.getServer(), paintingData.id());
    lines.add(Text.translatable("custompaintings.command.identify.count", count));

    Map<Identifier, PaintingData> known = CustomPaintingsMod.knownPaintings.get(player.getUuid())
        .stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity()));
    if (!known.containsKey(paintingData.id())) {
      lines.add(Text.translatable("custompaintings.command.identify.missing"));
    } else {
      if (paintingData.isMismatched(known.get(paintingData.id()), MismatchedCategory.INFO)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.info"));
      } else if (paintingData.isMismatched(known.get(paintingData.id()), MismatchedCategory.SIZE)) {
        lines.add(Text.translatable("custompaintings.command.identify.mismatched.size"));
      }
    }

    for (Text line : lines) {
      source.sendFeedback(() -> line, false);
    }
    return 1;
  }

  private static void identifyVanillaPainting(ServerCommandSource source, PaintingEntity painting) {
    ArrayList<Text> lines = new ArrayList<>();

    PaintingVariant variant = painting.getVariant().value();
    String id = Registries.PAINTING_VARIANT.getId(variant).toString();

    lines.add(Text.literal(id));
    lines.add(Text.translatable("custompaintings.painting.dimensions",
        variant.getWidth() / 16,
        variant.getHeight() / 16));

    for (Text line : lines) {
      source.sendFeedback(() -> line, false);
    }
  }
}
