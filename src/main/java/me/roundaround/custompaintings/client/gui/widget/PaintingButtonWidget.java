package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PaintingButtonWidget extends ButtonWidget {
  private final PaintingData paintingData;

  public PaintingButtonWidget(
      int x,
      int y,
      int maxWidth,
      int maxHeight,
      PressAction onPress,
      PaintingData paintingData) {
    super(
        x,
        y,
        paintingData.getScaledWidth(maxWidth, maxHeight),
        paintingData.getScaledHeight(maxWidth, maxHeight),
        Text.literal(paintingData.id().toString()),
        onPress);
    this.paintingData = paintingData;
  }

  @Override
  public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
    matrixStack.push();
    matrixStack.translate(0, 0, 150);

    int border = isHovered() ? 0xFFFFFFFF : 0xFF000000;
    fill(matrixStack, x, y, x + width, y + height, border);

    Sprite sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);

    float color = active ? 1f : 0.5f;

    RenderSystem.setShaderColor(color, color, color, 1f);
    RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
    drawSprite(matrixStack, x + 1, y + 1, 1, width - 2, height - 2, sprite);

    matrixStack.pop();
  }

  @Override
  public boolean isHovered() {
    return super.isHovered() && active;
  }
}
