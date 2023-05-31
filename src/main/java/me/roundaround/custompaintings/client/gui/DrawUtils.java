package me.roundaround.custompaintings.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.List;

public final class DrawUtils {
  public static void drawTruncatedCenteredTextWithShadow(
      DrawContext drawContext,
      TextRenderer textRenderer,
      Text text,
      int x,
      int y,
      int color,
      int maxWidth) {
    StringVisitable trimmed = text;
    if (textRenderer.getWidth(text) > maxWidth) {
      StringVisitable ellipsis = StringVisitable.plain("...");

      trimmed = StringVisitable.concat(textRenderer.trimToWidth(text,
          maxWidth - textRenderer.getWidth(ellipsis)), ellipsis);
    }

    drawContext.drawCenteredTextWithShadow(textRenderer,
        Language.getInstance().reorder(trimmed),
        x,
        y,
        color);
  }

  public static void drawWrappedCenteredTextWithShadow(
      DrawContext drawContext,
      TextRenderer textRenderer,
      Text text,
      int x,
      int y,
      int color,
      int maxWidth) {
    List<OrderedText> lines = textRenderer.wrapLines(text, maxWidth);
    int yCursor = y;
    for (OrderedText line : lines) {
      drawContext.drawCenteredTextWithShadow(textRenderer, line, x, yCursor, color);
      yCursor += textRenderer.fontHeight;
    }
  }
}
