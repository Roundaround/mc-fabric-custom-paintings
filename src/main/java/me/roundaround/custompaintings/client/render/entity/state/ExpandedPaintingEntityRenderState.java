package me.roundaround.custompaintings.client.render.entity.state;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.text.Text;

public interface ExpandedPaintingEntityRenderState {
  default void setCustomData(PaintingData paintingData) {
  }

  default PaintingData getCustomData() {
    return PaintingData.EMPTY;
  }

  default void setCustomName(Text customName) {
  }

  default Text getCustomName() {
    return null;
  }
}
