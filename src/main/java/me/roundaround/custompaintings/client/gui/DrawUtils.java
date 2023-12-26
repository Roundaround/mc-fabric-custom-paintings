package me.roundaround.custompaintings.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.List;

import static net.minecraft.client.gui.screen.Screen.OPTIONS_BACKGROUND_TEXTURE;

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

  public static void renderBackgroundInRegion(
      DrawContext drawContext, int y, int width, int height) {
    renderBackgroundInRegion(drawContext, 0, y, width, height);
  }

  public static void renderBackgroundInRegion(
      DrawContext drawContext, int x, int y, int width, int height) {
    renderBackgroundInRegion(drawContext, x, y, width, height, 0.25f);
  }

  public static void renderBackgroundInRegion(
      DrawContext drawContext, int y, int width, int height, float brightness) {
    renderBackgroundInRegion(drawContext, 0, y, width, height, brightness);
  }

  public static void renderBackgroundInRegion(
      DrawContext drawContext, int x, int y, int width, int height, float brightness) {
    drawContext.setShaderColor(brightness, brightness, brightness, 1f);
    drawContext.drawTexture(OPTIONS_BACKGROUND_TEXTURE, x, y, 0, 0f, 0f, width, height, 32, 32);
    drawContext.setShaderColor(1f, 1f, 1f, 1f);
  }
}
