package me.roundaround.custompaintings.client.render.entity.state;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.text.Text;

import java.util.List;

public interface PaintingEntityRenderStateExtensions {
  default void custompaintings$setData(PaintingData data) {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default PaintingData custompaintings$getData() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default void custompaintings$setLabel(List<Text> lines) {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default List<Text> custompaintings$getLabel() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }
}
