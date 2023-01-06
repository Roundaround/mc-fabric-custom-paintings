package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PaintingButtonWidget extends ButtonWithDisabledTooltipWidget {
  private final PaintingData paintingData;

  public PaintingButtonWidget(
      Screen screen,
      int x,
      int y,
      int width,
      int height,
      PressAction onPress,
      boolean active,
      Text disabledTooltip,
      PaintingData paintingData) {
    super(
        screen,
        x,
        y,
        width,
        height,
        Text.literal(paintingData.id().toString()),
        onPress,
        active,
        disabledTooltip);
    this.paintingData = paintingData;
    this.active = active;
  }

  @Override
  public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
    int border = isHovered() && active ? 0xFFFFFFFF : 0xFF000000;
    fill(matrixStack, x, y, x + width, y + height, border);

    Sprite sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);

    float color = active ? 1f : 0.5f;

    RenderSystem.setShaderColor(color, color, color, 1f);
    RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
    drawSprite(matrixStack, x + 1, y + 1, 0, width - 2, height - 2, sprite);

    if (isHovered()) {
      renderTooltip(matrixStack, mouseX, mouseY);
    }
  }
}
