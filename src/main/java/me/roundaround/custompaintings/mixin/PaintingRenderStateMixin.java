package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.render.entity.state.PaintingRenderStateExtensions;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(PaintingRenderState.class)
public abstract class PaintingRenderStateMixin implements PaintingRenderStateExtensions {
  @Unique
  private PaintingData data = PaintingData.EMPTY;
  @Unique
  private List<Component> label = null;

  @Override
  public void custompaintings$setData(PaintingData data) {
    this.data = data;
  }

  @Override
  public PaintingData custompaintings$getData() {
    return this.data;
  }

  @Override
  public void custompaintings$setLabel(List<Component> lines) {
    this.label = lines;
  }

  @Override
  public List<Component> custompaintings$getLabel() {
    return this.label;
  }
}
