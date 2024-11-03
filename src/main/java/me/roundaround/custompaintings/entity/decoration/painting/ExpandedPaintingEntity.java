package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.util.Identifier;

import java.util.UUID;

public interface ExpandedPaintingEntity {
  default void setCustomData(PaintingData paintingData) {
  }

  default void setCustomData(
      Identifier id, int width, int height, String name, String artist, boolean vanilla, boolean unknown
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

  default void setVariant(Identifier id) {
  }
}
