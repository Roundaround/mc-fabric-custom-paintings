package me.roundaround.custompaintings.client.render.entity.state;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface PaintingRenderStateExtensions {
  default void custompaintings$setData(PaintingData data) {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default PaintingData custompaintings$getData() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default void custompaintings$setLabel(List<Component> lines) {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default List<Component> custompaintings$getLabel() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }
}
