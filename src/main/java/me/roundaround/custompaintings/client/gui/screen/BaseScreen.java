package me.roundaround.custompaintings.client.gui.screen;

import com.google.common.collect.Lists;
import me.roundaround.custompaintings.client.gui.DrawUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public abstract class BaseScreen extends Screen {
  protected static final int ONE_COL_BUTTON_WIDTH = 204;
  protected static final int TWO_COL_BUTTON_WIDTH = 100;
  protected static final int THREE_COL_BUTTON_WIDTH = 100;
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_PADDING = 4;
  protected static final int HEADER_FOOTER_PADDING = 10;
  protected static final int ICON_BUTTON_WIDTH = 20;
  protected static final int ICON_BUTTON_HEIGHT = 20;

  protected final List<Drawable> drawables = Lists.newArrayList();

  protected BaseScreen(Text title) {
    super(title);
  }

  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
  }

  protected void renderForeground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    drawContext.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 11, 0xFFFFFFFF);
    renderDrawables(drawContext, mouseX, mouseY, partialTicks);
  }

  protected void renderDrawables(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    for (Drawable drawable : this.drawables) {
      drawable.render(drawContext, mouseX, mouseY, partialTicks);
    }
  }

  protected void renderBasicListBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks, Drawable listWidget) {
    MatrixStack matrixStack = drawContext.getMatrices();
    matrixStack.push();
    matrixStack.translate(0, 0, 10);
    listWidget.render(drawContext, mouseX, mouseY, partialTicks);
    matrixStack.pop();

    matrixStack.push();
    matrixStack.translate(0, 0, 11);
    DrawUtils.renderBackgroundInRegion(drawContext, 0, width, getHeaderHeight());
    DrawUtils.renderBackgroundInRegion(drawContext,
        height - getFooterHeight(),
        width,
        getFooterHeight());
    matrixStack.pop();
  }

  @Override
  public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    renderBackground(drawContext, mouseX, mouseY, partialTicks);

    MatrixStack matrixStack = drawContext.getMatrices();
    matrixStack.push();
    matrixStack.translate(0, 0, 12);
    renderForeground(drawContext, mouseX, mouseY, partialTicks);
    matrixStack.pop();
  }

  public void playClickSound() {
    if (this.client == null) {
      return;
    }
    this.client.getSoundManager()
        .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
  }

  protected int getHeaderHeight() {
    return HEADER_FOOTER_PADDING + textRenderer.fontHeight + 2 + HEADER_FOOTER_PADDING;
  }

  protected int getFooterHeight() {
    return HEADER_FOOTER_PADDING + BUTTON_HEIGHT + HEADER_FOOTER_PADDING;
  }

  @Override
  protected <T extends Element & Selectable & Drawable> T addDrawableChild(T drawableElement) {
    this.drawables.add(drawableElement);
    return super.addDrawableChild(drawableElement);
  }

  @Override
  protected <T extends Drawable> T addDrawable(T drawable) {
    this.drawables.add(drawable);
    return super.addDrawable(drawable);
  }

  @Override
  protected void remove(Element child) {
    if (child instanceof Drawable) {
      this.drawables.remove(child);
    }
    super.remove(child);
  }

  @Override
  protected void clearChildren() {
    this.drawables.clear();
    super.clearChildren();
  }
}
