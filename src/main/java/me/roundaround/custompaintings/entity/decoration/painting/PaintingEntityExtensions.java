package me.roundaround.custompaintings.entity.decoration.painting;

import me.roundaround.custompaintings.util.CustomId;

import java.util.UUID;

public interface PaintingEntityExtensions {
  default void custompaintings$setData(PaintingData paintingData) {
  }

  default PaintingData custompaintings$getData() {
    return PaintingData.EMPTY;
  }

  default void custompaintings$setEditor(UUID editor) {
  }

  default UUID custompaintings$getEditor() {
    return null;
  }

  default void custompaintings$setVariant(CustomId id) {
  }
}
