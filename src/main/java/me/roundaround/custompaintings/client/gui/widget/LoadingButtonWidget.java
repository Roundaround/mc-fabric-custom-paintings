package me.roundaround.custompaintings.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Util;

public class LoadingButtonWidget extends ButtonWidget {
  private boolean loading = false;

  public LoadingButtonWidget(
      int x, int y, int width, int height, Text message, PressAction onPress
  ) {
    super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
  }

  public void setLoading(boolean loading) {
    this.loading = loading;
    this.active = !loading;
  }

  @Override
  public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    if (!this.loading) {
      super.drawMessage(context, textRenderer, color);
      return;
    }

    String spinner = LoadingDisplay.get(Util.getMeasuringTimeMs());
    int x = this.getX() + (this.getWidth() - textRenderer.getWidth(spinner)) / 2;
    int y = this.getY() + (this.getHeight() - (textRenderer.fontHeight - 1)) / 2;
    context.drawText(textRenderer, spinner, x, y, Colors.WHITE, false);
  }
}
