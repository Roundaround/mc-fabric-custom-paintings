package me.roundaround.custompaintings.client.gui.widget;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public class ButtonWithDisabledTooltipWidget extends ButtonWidget {
  public List<OrderedText> disabledTooltip = List.of();

  public ButtonWithDisabledTooltipWidget(
      Screen screen,
      int x,
      int y,
      int width,
      int height,
      Text message,
      PressAction onPress) {
    super(x, y, width, height, message, onPress, new TooltipSupplier(screen));
  }

  private static class TooltipSupplier implements ButtonWidget.TooltipSupplier {
    private final Screen screen;

    public TooltipSupplier(Screen screen) {
      this.screen = screen;
    }

    @Override
    public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int x, int y) {
      if (buttonWidget.active) {
        return;
      }

      screen.renderOrderedTooltip(
          matrixStack,
          ((ButtonWithDisabledTooltipWidget) buttonWidget).disabledTooltip,
          x,
          y);
    }

    @Override
    public void supply(Consumer<Text> consumer) {
    }
  }
}
