package me.roundaround.custompaintings.entity.decoration.painting;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

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
  public static final PacketCodec<ByteBuf, PackData> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING,
      PackData::packFileUid,
      PacketCodecs.BOOLEAN,
      PackData::disabled,
      PacketCodecs.LONG,
      PackData::fileSize,
      PacketCodecs.STRING,
      PackData::id,
      PacketCodecs.STRING,
      PackData::name,
      PacketCodecs.optional(PacketCodecs.STRING),
      PackData::description,
      PacketCodecs.optional(PacketCodecs.STRING),
      PackData::sourceLegacyPack,
      PaintingData.PACKET_CODEC.collect(PacketCodecs.toList()),
      PackData::paintings,
      MigrationData.PACKET_CODEC.collect(PacketCodecs.toList()),
      PackData::migrations,
      PackData::new
  );

  public static PackData virtual(String id, Text name, Text description, List<PaintingData> paintings) {
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

  public Text getInformationText() {
    MutableText idText = Text.empty().append(this.id);
    if (this.packFileUid.isBlank()) {
      idText.formatted(Formatting.GRAY);
    }

    MutableText tooltip = Text.empty().append(this.name);
    if (this.description.isPresent() && !this.description.get().isBlank()) {
      tooltip.append("\n").append(this.description.get());
    }
    tooltip.append("\n").append(Text.translatable("custompaintings.packs.paintings", this.paintings.size()));

    return Texts.bracketed(idText)
        .styled(style -> style.withColor(this.disabled ? Formatting.RED : Formatting.GREEN)
            .withInsertion(StringArgumentType.escapeIfRequired(this.id))
            .withHoverEvent(new HoverEvent.ShowText(tooltip)));
  }
}
