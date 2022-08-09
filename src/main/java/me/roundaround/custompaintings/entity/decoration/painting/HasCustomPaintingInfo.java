package me.roundaround.custompaintings.entity.decoration.painting;

public interface HasCustomPaintingInfo {
  void setCustomInfo(CustomPaintingInfo info);
  void setCustomInfo(String name, int width, int height);
  CustomPaintingInfo getCustomPaintingInfo();
}
