package me.roundaround.custompaintings.client.gui.widget;

import java.util.Objects;
import java.util.function.Function;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.util.IntRect;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class ImageButtonWidget extends ButtonWidget {
  protected final Function<Image, Identifier> getTextureId;
  protected Image image;
  protected int imageWidth;
  protected int imageHeight;
  protected IntRect imageBounds = IntRect.zero();
  protected boolean inBatchUpdate = false;

  public ImageButtonWidget(
      Text message,
      ButtonWidget.PressAction pressAction,
      Function<Image, Identifier> getTextureId,
      Image image) {
    this(message, pressAction, getTextureId, image, true);
  }

  public ImageButtonWidget(
      Text message,
      ButtonWidget.PressAction pressAction,
      Function<Image, Identifier> getTextureId,
      Image image,
      boolean immediatelyCalculateBounds) {
    super(0, 0, 0, 0, message, pressAction, DEFAULT_NARRATION_SUPPLIER);
    if (message != null && !Objects.equals(message, Text.empty())) {
      this.setTooltip(Tooltip.of(message));
    }
    this.getTextureId = getTextureId;
    this.image = image;
    this.imageWidth = image == null ? 32 : image.width();
    this.imageHeight = image == null ? 32 : image.height();
    if (immediatelyCalculateBounds) {
      this.calculateBounds();
    }
  }

  public void batchUpdates(Runnable runnable) {
    this.inBatchUpdate = true;
    try {
      runnable.run();
    } finally {
      this.inBatchUpdate = false;
      this.calculateBounds();
    }
  }

  @Override
  public void setX(int x) {
    super.setX(x);
    this.calculateBounds();
  }

  @Override
  public void setY(int y) {
    super.setY(y);
    this.calculateBounds();
  }

  @Override
  public void setWidth(int width) {
    super.setWidth(width);
    this.calculateBounds();
  }

  @Override
  public void setHeight(int height) {
    super.setHeight(height);
    this.calculateBounds();
  }

  @Override
  public void setDimensions(int width, int height) {
    super.setDimensions(width, height);
    this.calculateBounds();
  }

  public void setImage(Image image) {
    this.image = image;
    this.imageWidth = image == null ? 32 : image.width();
    this.imageHeight = image == null ? 32 : image.height();
    this.calculateBounds();
  }

  public void calculateBounds() {
    if (this.inBatchUpdate || !this.visible) {
      return;
    }

    int width = this.getWidth() - 2;
    int height = this.getHeight() - 2;
    int x = this.getX() + 1;
    int y = this.getY() + 1;

    float scale = Math.min(
        (float) this.getWidth() / this.imageWidth,
        (float) this.getHeight() / this.imageHeight);
    int scaledWidth = Math.round(scale * this.imageWidth);
    int scaledHeight = Math.round(scale * this.imageHeight);

    this.imageBounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2,
        y + (height - scaledHeight) / 2,
        scaledWidth,
        scaledHeight);
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    return this.visible && this.imageBounds.contains(mouseX, mouseY);
  }

  @Override
  public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    this.hovered = this.hovered && this.imageBounds.contains(mouseX, mouseY);

    context.fill(
        this.imageBounds.left() - 1,
        this.imageBounds.top() - 1,
        this.imageBounds.right() + 1,
        this.imageBounds.bottom() + 1,
        this.active && (this.hovered || this.isFocused()) ? Colors.WHITE : Colors.BLACK);
    context.drawTexture(
        RenderPipelines.GUI_TEXTURED,
        this.getTextureId.apply(this.image),
        this.imageBounds.left(),
        this.imageBounds.top(),
        0,
        0,
        this.imageBounds.getWidth(),
        this.imageBounds.getHeight(),
        this.imageWidth,
        this.imageHeight,
        this.imageWidth,
        this.imageHeight);
  }
}
