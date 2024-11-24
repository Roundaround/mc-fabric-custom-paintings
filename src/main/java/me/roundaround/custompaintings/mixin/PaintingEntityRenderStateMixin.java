package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.render.entity.state.ExpandedPaintingEntityRenderState;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PaintingEntityRenderState.class)
public abstract class PaintingEntityRenderStateMixin implements ExpandedPaintingEntityRenderState {
  @Unique
  private PaintingData paintingData = PaintingData.EMPTY;
  @Unique
  private Text customName = null;

  @Override
  public void setCustomData(PaintingData paintingData) {
    this.paintingData = paintingData;
  }

  @Override
  public PaintingData getCustomData() {
    return this.paintingData;
  }

  @Override
  public void setCustomName(Text customName) {
    this.customName = customName;
  }

  @Override
  public Text getCustomName() {
    return this.customName;
  }
}
