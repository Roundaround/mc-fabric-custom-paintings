package me.roundaround.custompaintings.client.gui;

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
  private boolean isSelected = false;

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
        getScaledWidth(paintingData, maxWidth, maxHeight),
        getScaledHeight(paintingData, maxWidth, maxHeight),
        Text.literal(paintingData.getId().toString()),
        onPress);
    this.paintingData = paintingData;
  }

  @Override
  public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
    matrixStack.push();
    matrixStack.translate(0, 0, 150);

    int border = isHovered() || isSelected ? 0xFFFFFFFF : 0xFF000000;
    fill(matrixStack, x, y, x + width, y + height, border);

    Sprite sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData.getId()).get();
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
    drawSprite(matrixStack, x + 1, y + 1, 1, width - 2, height - 2, sprite);

    matrixStack.pop();
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public static int getScaledWidth(PaintingData paintingData, int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / paintingData.getScaledWidth(), (float) maxHeight / paintingData.getScaledHeight());
    return Math.round(scale * paintingData.getScaledWidth());
  }

  public static int getScaledHeight(PaintingData paintingData, int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / paintingData.getScaledWidth(), (float) maxHeight / paintingData.getScaledHeight());
    return Math.round(scale * paintingData.getScaledHeight());
  }
}
