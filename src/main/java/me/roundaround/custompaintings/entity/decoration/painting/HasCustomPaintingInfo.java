package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.util.Identifier;

public interface HasCustomPaintingInfo {
  void setCustomInfo(CustomPaintingInfo info);
  void setCustomInfo(Identifier id, int width, int height);
  CustomPaintingInfo getCustomPaintingInfo();
}
