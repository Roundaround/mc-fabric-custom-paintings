package me.roundaround.custompaintings.entity.decoration.painting;

import me.roundaround.custompaintings.util.CustomId;

import java.util.UUID;

public interface ExpandedPaintingEntity {
  default void setCustomData(PaintingData paintingData) {
  }

  default void setCustomData(
      CustomId id, int width, int height, String name, String artist, boolean vanilla, boolean unknown
  ) {
    this.setCustomData(new PaintingData(id, width, height, name, artist, vanilla, unknown));
  }

  default PaintingData getCustomData() {
    return PaintingData.EMPTY;
  }

  default void setEditor(UUID editor) {
  }

  default UUID getEditor() {
    return null;
  }

  default void setVariant(CustomId id) {
  }
}
