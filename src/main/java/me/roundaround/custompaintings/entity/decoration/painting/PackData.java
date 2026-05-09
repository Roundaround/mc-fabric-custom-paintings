package me.roundaround.custompaintings.entity.decoration.painting;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Optional;

public record PackData(String packFileUid,
                       boolean disabled,
                       long fileSize,
                       String id,
                       String name,
                       Optional<String> description,
                       Optional<String> sourceLegacyPack,
                       List<PaintingData> paintings,
                       List<MigrationData> migrations) {
  public static final StreamCodec<ByteBuf, PackData> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      PackData::packFileUid,
      ByteBufCodecs.BOOL,
      PackData::disabled,
      ByteBufCodecs.LONG,
      PackData::fileSize,
      ByteBufCodecs.STRING_UTF8,
      PackData::id,
      ByteBufCodecs.STRING_UTF8,
      PackData::name,
      ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
      PackData::description,
      ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
      PackData::sourceLegacyPack,
      PaintingData.PACKET_CODEC.apply(ByteBufCodecs.list()),
      PackData::paintings,
      MigrationData.PACKET_CODEC.apply(ByteBufCodecs.list()),
      PackData::migrations,
      PackData::new
  );

  public static PackData virtual(String id, Component name, Component description, List<PaintingData> paintings) {
    return new PackData(
        "",
        false,
        0,
        id,
        name.getString(),
        Optional.of(description.getString()),
        Optional.empty(),
        paintings,
        List.of()
    );
  }

  public Component getInformationText() {
    MutableComponent idText = Component.empty().append(this.id);
    if (this.packFileUid.isBlank()) {
      idText.withStyle(ChatFormatting.GRAY);
    }

    MutableComponent tooltip = Component.empty().append(this.name);
    if (this.description.isPresent() && !this.description.get().isBlank()) {
      tooltip.append("\n").append(this.description.get());
    }
    tooltip.append("\n").append(Component.translatable("custompaintings.packs.paintings", this.paintings.size()));

    return ComponentUtils.wrapInSquareBrackets(idText)
        .withStyle(style -> style.withColor(this.disabled ? ChatFormatting.RED : ChatFormatting.GREEN)
            .withInsertion(StringArgumentType.escapeIfRequired(this.id))
            .withHoverEvent(new HoverEvent.ShowText(tooltip)));
  }
}
