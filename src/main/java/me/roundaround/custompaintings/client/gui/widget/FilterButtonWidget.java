package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FilterButtonWidget extends ButtonWidget {
  public static final int WIDTH = 20;
  public static final int HEIGHT = 20;
  protected static final Identifier WIDGETS_TEXTURE = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "textures/gui/filter.png");

  public FilterButtonWidget(
      int x,
      int y,
      Screen parentScreen) {
    super(
        x,
        y,
        WIDTH,
        HEIGHT,
        Text.translatable("custompaintings.gui.filter"),
        (button) -> {
        });
  }

  @Override
  public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
    RenderSystem.enableDepthTest();

    int vIndex = isHovered() ? 2 : 1;
    drawTexture(matrixStack, x, y, 0, vIndex * HEIGHT, WIDTH, HEIGHT, WIDTH, HEIGHT * 3);

    if (hovered) {
      renderTooltip(matrixStack, mouseX, mouseY);
    }
  }
}
