package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public record PaintingData(
    Identifier id,
    int index,
    int width,
    int height,
    String name,
    String artist,
    boolean isVanilla) {

  public static final PaintingData EMPTY = new PaintingData(null, 0, 0, 0);

  public PaintingData(Identifier id, int index, int width, int height) {
    this(id, index, width, height, "", "");
  }

  public PaintingData(Identifier id, int index, int width, int height, String name, String artist) {
    this(id, index, width, height, name, artist, false);
  }

  public PaintingData(PaintingVariant vanillaVariant, int index) {
    this(
        Registry.PAINTING_VARIANT.getId(vanillaVariant),
        index,
        vanillaVariant.getWidth() / 16,
        vanillaVariant.getHeight() / 16,
        Registry.PAINTING_VARIANT.getId(vanillaVariant).getPath(),
        "",
        true);
  }

  public int getScaledWidth() {
    return width() * 16;
  }

  public int getScaledHeight() {
    return height() * 16;
  }

  public int getScaledWidth(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / getScaledWidth(), (float) maxHeight / getScaledHeight());
    return Math.round(scale * getScaledWidth());
  }

  public int getScaledHeight(int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / getScaledWidth(), (float) maxHeight / getScaledHeight());
    return Math.round(scale * getScaledHeight());
  }

  public boolean isEmpty() {
    return id == null;
  }

  public boolean hasName() {
    return name != null && !name.isEmpty();
  }

  public boolean hasArtist() {
    return artist != null && !artist.isEmpty();
  }

  public boolean hasLabel() {
    return hasName() || hasArtist();
  }

  public Text getLabel() {
    if (!hasLabel()) {
      return Text.empty();
    }

    if (!hasArtist() || isVanilla) {
      return Text.literal(name);
    }

    if (!hasName()) {
      return Text.literal(artist).setStyle(Style.EMPTY.withItalic(true));
    }

    return Text.literal("\"" + name + "\" - ")
        .append(Text.literal(artist).setStyle(Style.EMPTY.withItalic(true)));
  }

  public NbtCompound writeToNbt() {
    NbtCompound nbt = new NbtCompound();
    if (isEmpty()) {
      return nbt;
    }

    nbt.putString("Id", id.toString());
    nbt.putInt("Index", index);
    nbt.putInt("Width", width);
    nbt.putInt("Height", height);
    nbt.putString("Name", name);
    nbt.putString("Artist", artist);
    nbt.putBoolean("Vanilla", isVanilla);
    return nbt;
  }

  public static PaintingData fromNbt(NbtCompound nbt) {
    if (!nbt.contains("Id")) {
      return EMPTY;
    }

    Identifier id = Identifier.tryParse(nbt.getString("Id"));
    int index = nbt.getInt("Index");
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    String name = nbt.getString("Name");
    String artist = nbt.getString("Artist");
    boolean isVanilla = nbt.getBoolean("Vanilla");
    return new PaintingData(id, index, width, height, name, artist, isVanilla);
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    if (isEmpty()) {
      buf.writeBoolean(false);
      return;
    }
    buf.writeBoolean(true);
    buf.writeIdentifier(id);
    buf.writeInt(index);
    buf.writeInt(width);
    buf.writeInt(height);
    buf.writeString(name);
    buf.writeString(artist);
    buf.writeBoolean(isVanilla);
  }

  public static PaintingData fromPacketByteBuf(PacketByteBuf buf) {
    if (!buf.readBoolean()) {
      return EMPTY;
    }
    Identifier id = buf.readIdentifier();
    int index = buf.readInt();
    int width = buf.readInt();
    int height = buf.readInt();
    String name = buf.readString();
    String artist = buf.readString();
    boolean isVanilla = buf.readBoolean();
    return new PaintingData(id, index, width, height, name, artist, isVanilla);
  }

  public boolean isMismatched(PaintingData knownData) {
    return isMismatched(knownData, MismatchedCategory.EVERYTHING);
  }

  public boolean isMismatched(PaintingData knownData, MismatchedCategory category) {
    switch (category) {
      case SIZE:
        return width() != knownData.width() || height() != knownData.height();
      case INFO:
        return !name().equals(knownData.name()) || !artist().equals(knownData.artist());
      case EVERYTHING:
        return width() != knownData.width() || height() != knownData.height()
            || !name().equals(knownData.name()) || !artist().equals(knownData.artist());
      default:
        return false;
    }
  }

  public enum MismatchedCategory {
    SIZE,
    INFO,
    EVERYTHING
  }
}
