package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.nbt.NbtCompound;

public class CustomPaintingInfo {
  public static final CustomPaintingInfo EMPTY = new CustomPaintingInfo("", 0, 0);

  private final String name;
  private final int width;
  private final int height;

  public CustomPaintingInfo(String name, int width, int height) {
    this.name = name;
    this.width = width;
    this.height = height;
  }

  public String getName() {
    return name;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isEmpty() {
    return name.isBlank();
  }

  public NbtCompound writeToNbt() {
    NbtCompound nbt = new NbtCompound();
    if (isEmpty()) {
      return nbt;
    }

    nbt.putString("Name", name);
    nbt.putInt("Width", width);
    nbt.putInt("Height", height);
    return nbt;
  }

  public static CustomPaintingInfo fromNbt(NbtCompound nbt) {
    String name = nbt.getString("Name");
    int width = nbt.getInt("Width");
    int height = nbt.getInt("Height");
    return new CustomPaintingInfo(name, width, height);
  }
}
