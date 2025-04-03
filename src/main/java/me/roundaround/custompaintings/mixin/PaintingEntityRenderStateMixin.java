package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.render.entity.state.PaintingEntityRenderStateExtensions;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(PaintingEntityRenderState.class)
public abstract class PaintingEntityRenderStateMixin implements PaintingEntityRenderStateExtensions {
  @Unique
  private PaintingData data = PaintingData.EMPTY;
  @Unique
  private List<Text> label = null;

  @Override
  public void custompaintings$setData(PaintingData data) {
    this.data = data;
  }

  @Override
  public PaintingData custompaintings$getData() {
    return this.data;
  }

  @Override
  public void custompaintings$setLabel(List<Text> lines) {
    this.label = lines;
  }

  @Override
  public List<Text> custompaintings$getLabel() {
    return this.label;
  }
}
