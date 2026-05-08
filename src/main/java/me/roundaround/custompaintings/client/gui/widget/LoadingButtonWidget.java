package me.roundaround.custompaintings.client.gui.widget;

import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Util;

public class LoadingButtonWidget extends ButtonWidget.Text {
  private boolean loading = false;

  public LoadingButtonWidget(
      int x,
      int y,
      int width,
      int height,
      net.minecraft.text.Text message,
      PressAction onPress
  ) {
    super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
  }

  public void setLoading(boolean loading) {
    this.loading = loading;
    this.active = !loading;
  }

  public boolean isLoading() {
    return this.loading;
  }

  @Override
  public void drawLabel(DrawnTextConsumer drawer) {
    if (!this.loading) {
      super.drawLabel(drawer);
      return;
    }

    String spinner = LoadingDisplay.get(Util.getMeasuringTimeMs());
    this.drawTextWithMargin(drawer, net.minecraft.text.Text.literal(spinner), 2);
    //    int x = this.getX() + (this.getWidth() - textRenderer.getWidth(spinner)) / 2;
    //    int y = this.getY() + (this.getHeight() - (textRenderer.fontHeight - 1)) / 2;
    //    context.drawText(textRenderer, spinner, x, y, Colors.WHITE, false);
  }
}
