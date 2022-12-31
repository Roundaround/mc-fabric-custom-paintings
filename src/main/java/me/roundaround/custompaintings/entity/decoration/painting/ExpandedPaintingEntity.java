package me.roundaround.custompaintings.entity.decoration.painting;

import java.util.UUID;

import net.minecraft.util.Identifier;

public interface ExpandedPaintingEntity {
  void setCustomData(PaintingData info);

  void setCustomData(Identifier id, int width, int height, String name, String artist);

  PaintingData getCustomData();

  void setEditor(UUID editor);

  UUID getEditor();

  void setVariant(Identifier id);
}
