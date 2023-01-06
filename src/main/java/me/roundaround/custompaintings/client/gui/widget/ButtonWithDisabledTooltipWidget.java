package me.roundaround.custompaintings.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ButtonWithDisabledTooltipWidget extends ButtonWidget {
  public ButtonWithDisabledTooltipWidget(
      Screen screen,
      int x,
      int y,
      int width,
      int height,
      Text message,
      PressAction onPress,
      boolean active,
      Text disabledTooltip) {
    super(x, y, width, height, message, onPress, new TooltipSupplier(screen, disabledTooltip));
    this.active = active;
  }

  private static class TooltipSupplier implements ButtonWidget.TooltipSupplier {
    private final Screen screen;
    private final Text tooltip;

    public TooltipSupplier(Screen screen, Text tooltip) {
      this.screen = screen;
      this.tooltip = tooltip;
    }

    @Override
    public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int x, int y) {
      if (buttonWidget.active) {
        return;
      }
      screen.renderTooltip(matrixStack, this.tooltip, x, y);
    }

    @Override
    public void supply(Consumer<Text> consumer) {
      consumer.accept(this.tooltip);
    }
  }
}
