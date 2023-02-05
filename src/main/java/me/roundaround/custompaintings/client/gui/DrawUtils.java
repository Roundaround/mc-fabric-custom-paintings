package me.roundaround.custompaintings.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public interface DrawUtils {
  public default void drawTruncatedCenteredTextWithShadow(
      MatrixStack matrixStack,
      TextRenderer textRenderer,
      Text text,
      int x,
      int y,
      int color,
      int maxWidth) {
    StringVisitable trimmed = text;
    if (textRenderer.getWidth(text) > maxWidth) {
      StringVisitable ellipsis = StringVisitable.plain("...");

      trimmed = StringVisitable.concat(
          textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth(ellipsis)),
          ellipsis);
    }

    DrawableHelper.drawCenteredTextWithShadow(
        matrixStack,
        textRenderer,
        Language.getInstance().reorder(trimmed),
        x,
        y,
        color);
  }
}
