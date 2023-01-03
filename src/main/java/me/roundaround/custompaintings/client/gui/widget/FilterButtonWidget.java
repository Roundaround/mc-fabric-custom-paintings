package me.roundaround.custompaintings.client.gui.widget;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingFilterScreen;
import net.minecraft.client.MinecraftClient;
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
  protected static final Text TOOLTIP = Text.translatable("custompaintings.gui.filter");
  private static final MinecraftClient MINECRAFT = MinecraftClient.getInstance();

  public FilterButtonWidget(
      int x,
      int y,
      PaintingEditScreen parent) {
    super(
        x,
        y,
        WIDTH,
        HEIGHT,
        TOOLTIP,
        (button) -> {
          MINECRAFT.setScreen(new PaintingFilterScreen(parent));
        },
        new TooltipSupplier(parent));
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

  private static class TooltipSupplier implements ButtonWidget.TooltipSupplier {
    private final Screen screen;

    public TooltipSupplier(Screen screen) {
      this.screen = screen;
    }

    @Override
    public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int x, int y) {
      screen.renderTooltip(matrixStack, TOOLTIP, x, y);
    }

    @Override
    public void supply(Consumer<Text> consumer) {
      consumer.accept(TOOLTIP);
    }
  }
}
