package me.roundaround.custompaintings.entity.decoration.painting;

import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PaintingData(CustomId id, int width, int height, String name, String artist, boolean vanilla,
                           boolean unknown) {
  public static final PaintingData EMPTY = new PaintingData(null, 0, 0);
  public static final PacketCodec<PacketByteBuf, PaintingData> PACKET_CODEC = PacketCodec.of(
      PaintingData::write, PaintingData::read);

  public PaintingData(CustomId id, int width, int height) {
    this(id, width, height, "", "");
  }

  public PaintingData(CustomId id, int width, int height, String name, String artist) {
    this(id, width, height, name, artist, false, false);
  }

  public PaintingData(PaintingVariant vanillaVariant) {
    this(CustomId.from(Registries.PAINTING_VARIANT.getId(vanillaVariant)), vanillaVariant.getWidth() / 16,
        vanillaVariant.getHeight() / 16, Registries.PAINTING_VARIANT.getId(vanillaVariant).getPath(), "", true, false
    );
  }

  public int getScaledWidth() {
    return this.width() * 16;
  }

  public int getScaledHeight() {
    return this.height() * 16;
  }

  public int getScaledWidth(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / this.getScaledWidth(), (float) maxHeight / this.getScaledHeight());
    return Math.round(scale * this.getScaledWidth());
  }

  public int getScaledHeight(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / this.getScaledWidth(), (float) maxHeight / this.getScaledHeight());
    return Math.round(scale * this.getScaledHeight());
  }

  public boolean isEmpty() {
    return this.id == null;
  }

  public boolean hasName() {
    return this.vanilla() || this.name != null && !this.name.isEmpty();
  }

  public boolean hasArtist() {
    return this.vanilla() || this.artist != null && !this.artist.isEmpty();
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

    return Text.literal(this.name).formatted(Formatting.LIGHT_PURPLE);
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

  public PaintingData setName(String name) {
    return new PaintingData(this.id, this.width, this.height, name, this.artist, this.vanilla, this.unknown);
  }

  public PaintingData setArtist(String artist) {
    return new PaintingData(this.id, this.width, this.height, this.name, artist, this.vanilla, this.unknown);
  }

  public PaintingData setLabel(String name, String artist) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PaintingData that))
      return false;

    return this.width == that.width && this.height == that.height && this.vanilla == that.vanilla &&
           Objects.equals(this.name, that.name) && Objects.equals(this.id, that.id) &&
           Objects.equals(this.artist, that.artist);
  }

  public NbtCompound write() {
    NbtCompound nbt = new NbtCompound();
    if (this.isEmpty()) {
      return nbt;
    }

    nbt.putString("Id", this.id.toString());
    nbt.putInt("Width", this.width);
    nbt.putInt("Height", this.height);
    nbt.putString("Name", this.name == null ? "" : this.name);
    nbt.putString("Artist", this.artist == null ? "" : this.artist);
    nbt.putBoolean("Vanilla", this.vanilla);
    return nbt;
  }

  public void write(PacketByteBuf buf) {
    if (this.isEmpty()) {
      buf.writeBoolean(false);
      return;
    }
    buf.writeBoolean(true);
    this.id.write(buf);
    buf.writeInt(this.width);
    buf.writeInt(this.height);
    buf.writeString(this.name == null ? "" : this.name);
    buf.writeString(this.artist == null ? "" : this.artist);
    buf.writeBoolean(this.vanilla);
    buf.writeBoolean(this.unknown);
  }

  public static PaintingData read(NbtCompound nbt) {
    if (!nbt.contains("Id")) {
      return EMPTY;
    }

    CustomId id = CustomId.parse(nbt.getString("Id"));
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    String name = nbt.getString("Name");
    String artist = nbt.getString("Artist");
    boolean isVanilla = nbt.getBoolean("Vanilla");
    return new PaintingData(id, width, height, name, artist, isVanilla, false);
  }

  public static PaintingData read(PacketByteBuf buf) {
    if (!buf.readBoolean()) {
      return EMPTY;
    }
    CustomId id = CustomId.read(buf);
    int width = buf.readInt();
    int height = buf.readInt();
    String name = buf.readString();
    String artist = buf.readString();
    boolean isVanilla = buf.readBoolean();
    boolean isUnknown = buf.readBoolean();
    return new PaintingData(id, width, height, name, artist, isVanilla, isUnknown);
  }

  public enum MismatchedCategory {
    SIZE, INFO, EVERYTHING
  }
}
