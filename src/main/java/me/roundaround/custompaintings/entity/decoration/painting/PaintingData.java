package me.roundaround.custompaintings.entity.decoration.painting;

import me.roundaround.custompaintings.resource.PackIcons;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PaintingData(Identifier id, int width, int height, String name, String artist, boolean isVanilla) {
  public static final PaintingData EMPTY = new PaintingData(null, 0, 0);
  public static final PacketCodec<PacketByteBuf, PaintingData> PACKET_CODEC = PacketCodec.of(
      PaintingData::writeToPacketByteBuf, PaintingData::fromPacketByteBuf);

  public PaintingData(Identifier id, int width, int height) {
    this(id, width, height, "", "");
  }

  public PaintingData(Identifier id, int width, int height, String name, String artist) {
    this(id, width, height, name, artist, false);
  }

  public PaintingData(PaintingVariant vanillaVariant) {
    this(Registries.PAINTING_VARIANT.getId(vanillaVariant), vanillaVariant.getWidth() / 16,
        vanillaVariant.getHeight() / 16, Registries.PAINTING_VARIANT.getId(vanillaVariant).getPath(), "", true
    );
  }

  public static PaintingData packIcon(String packId) {
    return new PaintingData(PackIcons.identifier(packId), 16, 16, "", "");
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
    return this.isVanilla() || this.name != null && !this.name.isEmpty();
  }

  public boolean hasArtist() {
    return this.isVanilla() || this.artist != null && !this.artist.isEmpty();
  }

  public boolean hasLabel() {
    return this.hasName() || this.hasArtist();
  }

  public MutableText getNameText() {
    if (!this.hasName()) {
      return Text.empty();
    }

    if (this.isVanilla()) {
      return Text.translatable(this.id().toTranslationKey("painting", "title")).formatted(Formatting.YELLOW);
    }

    return Text.literal(this.name).formatted(Formatting.LIGHT_PURPLE);
  }

  public MutableText getArtistText() {
    if (!this.hasArtist()) {
      return Text.empty();
    }

    if (this.isVanilla()) {
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
    MutableText idText = Text.literal("(" + this.id + ")");
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

  public PaintingData setId(Identifier id) {
    return new PaintingData(id, this.width, this.height, this.name, this.artist, this.isVanilla);
  }

  public PaintingData setWidth(int width) {
    return new PaintingData(this.id, width, this.height, this.name, this.artist, this.isVanilla);
  }

  public PaintingData setHeight(int height) {
    return new PaintingData(this.id, this.width, height, this.name, this.artist, this.isVanilla);
  }

  public PaintingData setDimensions(int width, int height) {
    return new PaintingData(this.id, width, height, this.name, this.artist, this.isVanilla);
  }

  public PaintingData setName(String name) {
    return new PaintingData(this.id, this.width, this.height, name, this.artist, this.isVanilla);
  }

  public PaintingData setArtist(String artist) {
    return new PaintingData(this.id, this.width, this.height, this.name, artist, this.isVanilla);
  }

  public PaintingData setLabel(String name, String artist) {
    return new PaintingData(this.id, this.width, this.height, name, artist, this.isVanilla);
  }

  public boolean isMismatched(PaintingData knownData) {
    return this.isMismatched(knownData, MismatchedCategory.EVERYTHING);
  }

  public boolean isMismatched(PaintingData knownData, MismatchedCategory category) {
    return switch (category) {
      case SIZE -> this.width() != knownData.width() || this.height() != knownData.height();
      case INFO -> !this.name().equals(knownData.name()) || !this.artist().equals(knownData.artist()) ||
          this.isVanilla() != knownData.isVanilla();
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

    return this.width == that.width && this.height == that.height && this.isVanilla == that.isVanilla &&
        Objects.equals(this.name, that.name) && Objects.equals(this.id, that.id) &&
        Objects.equals(this.artist, that.artist);
  }

  public NbtCompound writeToNbt() {
    NbtCompound nbt = new NbtCompound();
    if (this.isEmpty()) {
      return nbt;
    }

    nbt.putString("Id", this.id.toString());
    nbt.putInt("Width", this.width);
    nbt.putInt("Height", this.height);
    nbt.putString("Name", this.name == null ? "" : this.name);
    nbt.putString("Artist", this.artist == null ? "" : this.artist);
    nbt.putBoolean("Vanilla", this.isVanilla);
    return nbt;
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    if (this.isEmpty()) {
      buf.writeBoolean(false);
      return;
    }
    buf.writeBoolean(true);
    buf.writeIdentifier(this.id);
    buf.writeInt(this.width);
    buf.writeInt(this.height);
    buf.writeString(this.name == null ? "" : this.name);
    buf.writeString(this.artist == null ? "" : this.artist);
    buf.writeBoolean(this.isVanilla);
  }

  public static PaintingData fromNbt(NbtCompound nbt) {
    if (!nbt.contains("Id")) {
      return EMPTY;
    }

    Identifier id = Identifier.tryParse(nbt.getString("Id"));
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    String name = nbt.getString("Name");
    String artist = nbt.getString("Artist");
    boolean isVanilla = nbt.getBoolean("Vanilla");
    return new PaintingData(id, width, height, name, artist, isVanilla);
  }

  public static PaintingData fromPacketByteBuf(PacketByteBuf buf) {
    if (!buf.readBoolean()) {
      return EMPTY;
    }
    Identifier id = buf.readIdentifier();
    int width = buf.readInt();
    int height = buf.readInt();
    String name = buf.readString();
    String artist = buf.readString();
    boolean isVanilla = buf.readBoolean();
    return new PaintingData(id, width, height, name, artist, isVanilla);
  }

  public enum MismatchedCategory {
    SIZE, INFO, EVERYTHING
  }
}
