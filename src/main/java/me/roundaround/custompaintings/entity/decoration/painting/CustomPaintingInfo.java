package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class CustomPaintingInfo {
  public static final CustomPaintingInfo EMPTY = new CustomPaintingInfo(null, 0, 0);

  private final Identifier id;
  private final int width;
  private final int height;

  public CustomPaintingInfo(Identifier id, int width, int height) {
    this.id = id;
    this.width = width;
    this.height = height;
  }

  public Identifier getId() {
    return id;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getScaledWidth() {
    return getWidth() * 16;
  }

  public int getScaledHeight() {
    return getHeight() * 16;
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
    return nbt;
  }

  public static CustomPaintingInfo fromNbt(NbtCompound nbt) {
    if (!nbt.contains("Id")) {
      return EMPTY;
    }

    Identifier id = Identifier.tryParse(nbt.getString("Id"));
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    return new CustomPaintingInfo(id, width, height);
  }
}
