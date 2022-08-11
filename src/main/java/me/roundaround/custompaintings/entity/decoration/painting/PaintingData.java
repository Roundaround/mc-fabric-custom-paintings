package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public record PaintingData(Identifier id, int width, int height, boolean isVanilla) {

  public static final PaintingData EMPTY = new PaintingData(null, 0, 0);

  public PaintingData(Identifier id, int width, int height) {
    this(id, width, height, false);
  }

  public PaintingData(PaintingVariant vanillaVariant) {
    this(
        Registry.PAINTING_VARIANT.getId(vanillaVariant),
        vanillaVariant.getWidth() / 16,
        vanillaVariant.getHeight() / 16,
        true);
  }

  public int getScaledWidth() {
    return width() * 16;
  }

  public int getScaledHeight() {
    return height() * 16;
  }

  public boolean isEmpty() {
    return id == null;
  }

  public NbtCompound writeToNbt() {
    NbtCompound nbt = new NbtCompound();
    if (isEmpty()) {
      return nbt;
    }

    nbt.putString("Id", id.toString());
    nbt.putInt("Width", width);
    nbt.putInt("Height", height);
    nbt.putBoolean("Vanilla", isVanilla);
    return nbt;
  }

  public static PaintingData fromNbt(NbtCompound nbt) {
    if (!nbt.contains("Id")) {
      return EMPTY;
    }

    Identifier id = Identifier.tryParse(nbt.getString("Id"));
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    boolean isVanilla = nbt.getBoolean("Vanilla");
    return new PaintingData(id, width, height, isVanilla);
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    if (isEmpty()) {
      buf.writeBoolean(false);
      return;
    }
    buf.writeBoolean(true);
    buf.writeIdentifier(id());
    buf.writeInt(width());
    buf.writeInt(height());
    buf.writeBoolean(isVanilla());
  }

  public static PaintingData fromPacketByteBuf(PacketByteBuf buf) {
    if (!buf.readBoolean()) {
      return EMPTY;
    }
    Identifier id = buf.readIdentifier();
    int width = buf.readInt();
    int height = buf.readInt();
    boolean isVanilla = buf.readBoolean();
    return new PaintingData(id, width, height, isVanilla);
  }
}
