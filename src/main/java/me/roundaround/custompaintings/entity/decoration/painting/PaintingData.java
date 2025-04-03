package me.roundaround.custompaintings.entity.decoration.painting;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PaintingData {
  public static final PaintingData EMPTY = new PaintingData(null, 0, 0);
  public static final Codec<PaintingData> CODEC =
      RecordCodecBuilder.create((instance) -> mapBaseCodecFields(instance).apply(instance,
      PaintingData::new
  ));
  public static final PacketCodec<ByteBuf, PaintingData> PACKET_CODEC = PacketCodec.tuple(
      CustomId.PACKET_CODEC,
      PaintingData::id,
      PacketCodecs.INTEGER,
      PaintingData::width,
      PacketCodecs.INTEGER,
      PaintingData::height,
      PacketCodecs.STRING,
      PaintingData::name,
      PacketCodecs.STRING,
      PaintingData::artist,
      PacketCodecs.BOOLEAN,
      PaintingData::vanilla,
      PacketCodecs.BOOLEAN,
      PaintingData::unknown,
      PaintingData::new
  );
  private final CustomId id;
  private final int width;
  private final int height;
  private final @NotNull String name;
  private final @NotNull String artist;
  private final boolean vanilla;
  private final boolean unknown;

  public PaintingData(
      CustomId id,
      int width,
      int height,
      @NotNull String name,
      @NotNull String artist,
      boolean vanilla,
      boolean unknown
  ) {
    assert name != null;
    assert artist != null;
    this.id = id;
    this.width = width;
    this.height = height;
    this.name = name;
    this.artist = artist;
    this.vanilla = vanilla;
    this.unknown = unknown;
  }

  public PaintingData(CustomId id, int width, int height) {
    this(id, width, height, "", "");
  }

  public PaintingData(CustomId id, int width, int height, @NotNull String name, @NotNull String artist) {
    this(id, width, height, name, artist, false, false);
  }

  public PaintingData(PaintingVariant vanillaVariant) {
    this(
        CustomId.from(vanillaVariant.assetId()),
        vanillaVariant.width(),
        vanillaVariant.height(),
        vanillaVariant.assetId().getPath(),
        "",
        true,
        false
    );
  }

  public PaintingVariant toVariant() {
    return new PaintingVariant(
        this.width(),
        this.height(),
        CustomId.toIdentifier(this.id()),
        Optional.of(this.getNameText()),
        Optional.of(this.getArtistText())
    );
  }

  public int getScaledWidth() {
    return this.width() * 16;
  }

  public int getScaledHeight() {
    return this.height() * 16;
  }

  public int getScaledWidth(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / this.width(), (float) maxHeight / this.height());
    return Math.round(scale * this.width());
  }

  public int getScaledHeight(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / this.width(), (float) maxHeight / this.height());
    return Math.round(scale * this.height());
  }

  public boolean isEmpty() {
    return this.id() == null;
  }

  public boolean hasName() {
    return this.vanilla() || !this.name().isEmpty();
  }

  public boolean hasArtist() {
    return this.vanilla() || !this.artist().isEmpty();
  }

  public boolean hasLabel() {
    return this.hasName() || this.hasArtist();
  }

  public MutableText getNameText() {
    if (!this.hasName()) {
      return Text.empty();
    }

    if (this.vanilla()) {
      return Text.translatable(this.id().toTranslationKey("painting", "title")).formatted(Formatting.YELLOW);
    }

    return Text.literal(this.name()).formatted(Formatting.LIGHT_PURPLE);
  }

  public MutableText getArtistText() {
    if (!this.hasArtist()) {
      return Text.empty();
    }

    if (this.vanilla()) {
      return Text.translatable(this.id().toTranslationKey("painting", "author")).formatted(Formatting.ITALIC);
    }

    return Text.literal(this.artist).formatted(Formatting.ITALIC);
  }

  public Text getLabel() {
    if (!this.hasLabel()) {
      return Text.empty();
    }

    if (!this.hasArtist()) {
      return this.getNameText();
    }

    if (!this.hasName()) {
      return this.getArtistText();
    }

    return Text.empty().append(this.getNameText()).append(" - ").append(this.getArtistText());
  }

  public List<Text> getLabelAsLines() {
    if (!this.hasLabel()) {
      return List.of();
    }

    if (!this.hasArtist()) {
      return List.of(this.getNameText());
    }

    if (!this.hasName()) {
      return List.of(this.getArtistText());
    }

    return List.of(this.getNameText(), this.getArtistText());
  }

  public Text getIdText() {
    MutableText idText = Text.literal("(" + this.id().resource() + ")");
    if (this.hasLabel()) {
      idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
    }
    return idText;
  }

  public Text getDimensionsText() {
    return Text.translatable("custompaintings.painting.dimensions", this.width, this.height);
  }

  public List<Text> getInfoLines() {
    ArrayList<Text> lines = new ArrayList<>();
    if (this.hasLabel()) {
      lines.add(this.getLabel());
    }
    lines.add(this.getIdText());
    lines.add(this.getDimensionsText());
    return lines;
  }

  public PaintingData setId(CustomId id) {
    return new PaintingData(id, this.width, this.height, this.name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setWidth(int width) {
    return new PaintingData(this.id, width, this.height, this.name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setHeight(int height) {
    return new PaintingData(this.id, this.width, height, this.name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setDimensions(int width, int height) {
    return new PaintingData(this.id, width, height, this.name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setName(@NotNull String name) {
    return new PaintingData(this.id, this.width, this.height, name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setArtist(@NotNull String artist) {
    return new PaintingData(this.id, this.width, this.height, this.name, artist, this.vanilla, this.unknown);
  }

  public PaintingData setLabel(@NotNull String name, @NotNull String artist) {
    return new PaintingData(this.id, this.width, this.height, name, artist, this.vanilla, this.unknown);
  }

  public PaintingData markUnknown() {
    return new PaintingData(this.id, this.width, this.height, this.name, this.artist, this.vanilla, true);
  }

  public boolean isMismatched(PaintingData knownData) {
    return this.isMismatched(knownData, MismatchedCategory.EVERYTHING);
  }

  public boolean isMismatched(PaintingData knownData, MismatchedCategory category) {
    return switch (category) {
      case SIZE -> this.width() != knownData.width() || this.height() != knownData.height();
      case INFO -> !this.name().equals(knownData.name()) || !this.artist().equals(knownData.artist()) ||
                   this.vanilla() != knownData.vanilla();
      case EVERYTHING -> this.width() != knownData.width() || this.height() != knownData.height() ||
                         !this.name().equals(knownData.name()) || !this.artist().equals(knownData.artist());
    };
  }

  public boolean idEquals(PaintingData other) {
    return Objects.equals(this.id(), other.id());
  }

  public CustomId id() {
    return this.id;
  }

  public int width() {
    return this.width;
  }

  public int height() {
    return this.height;
  }

  public String name() {
    return this.name;
  }

  public String artist() {
    return this.artist;
  }

  public boolean vanilla() {
    return this.vanilla;
  }

  public boolean unknown() {
    return this.unknown;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PaintingData that)) {
      return false;
    }

    return this.width == that.width && this.height == that.height && this.vanilla == that.vanilla &&
           Objects.equals(this.name, that.name) && Objects.equals(this.id, that.id) &&
           Objects.equals(this.artist, that.artist);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.width, this.height, this.name, this.artist, this.vanilla, this.unknown);
  }

  @Override
  public String toString() {
    return "PaintingData[" + "id=" + this.id + ", " + "width=" + this.width + ", " + "height=" + this.height + ", " +
           "name=" + this.name + ", " + "artist=" + this.artist + ", " + "vanilla=" + this.vanilla + ", " + "unknown=" +
           this.unknown + ']';
  }

  public static <T extends PaintingData> Products.P7<RecordCodecBuilder.Mu<T>, CustomId, Integer, Integer, String,
      String, Boolean, Boolean> mapBaseCodecFields(
      RecordCodecBuilder.Instance<T> instance
  ) {
    return instance.group(
        CustomId.CODEC.fieldOf("Id").forGetter(PaintingData::id),
        Codec.INT.fieldOf("Width").forGetter(PaintingData::width),
        Codec.INT.fieldOf("Height").forGetter(PaintingData::height),
        Codec.STRING.optionalFieldOf("Name", "").forGetter(PaintingData::name),
        Codec.STRING.optionalFieldOf("Artist", "").forGetter(PaintingData::artist),
        Codec.BOOL.fieldOf("Vanilla").forGetter(PaintingData::vanilla),
        Codec.BOOL.optionalFieldOf("Unknown", false).forGetter(PaintingData::unknown)
    );
  }

  public enum MismatchedCategory {
    SIZE, INFO, EVERYTHING
  }
}
