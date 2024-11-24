package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.util.IntRect;
import net.minecraft.client.gui.DrawContext;

public class PaintingSpriteWidget extends SpriteWidget {
  private PaintingData paintingData;
  private boolean border;

  private PaintingSpriteWidget(int x, int y, int width, int height, PaintingData paintingData, boolean border) {
    super(x, y, width, height, ClientPaintingRegistry.getInstance().getSprite(paintingData), false);

    this.paintingData = paintingData;
    this.border = border;
    this.calculateBounds();
  }

  public void setPaintingData(PaintingData paintingData) {
    this.visible = paintingData != null && !paintingData.isEmpty();
    this.paintingData = paintingData != null ? paintingData : PaintingData.EMPTY;
    this.setSprite(ClientPaintingRegistry.getInstance().getSprite(this.paintingData));
  }

  public void setBorder(boolean border) {
    this.border = border;
    this.calculateBounds();
  }

  @Override
  public void calculateBounds() {
    if (this.inBatchUpdate || !this.visible) {
      return;
    }

    int width = this.getWidth();
    int height = this.getHeight();
    int x = this.getX();
    int y = this.getY();

    if (this.border) {
      width -= 2;
      height -= 2;
      x += 1;
      y += 1;
    }

    int scaledWidth = this.paintingData.getScaledWidth(width, height);
    int scaledHeight = this.paintingData.getScaledHeight(width, height);

    this.imageBounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2, y + (height - scaledHeight) / 2, scaledWidth, scaledHeight);
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    if (this.border) {
      context.fill(this.imageBounds.left() - 1, this.imageBounds.top() - 1, this.imageBounds.right() + 1,
          this.imageBounds.bottom() + 1, 0xFF000000
      );
    }

    super.renderWidget(context, mouseX, mouseY, delta);
  }

  public static Builder builder(PaintingData paintingData) {
    return new Builder(paintingData);
  }

  public static PaintingSpriteWidget create(PaintingData paintingData) {
    return builder(paintingData).build();
  }

  public static class Builder {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private boolean border = false;

    private final PaintingData paintingData;

    private Builder(PaintingData paintingData) {
      this.paintingData = paintingData;
    }

    public Builder x(int x) {
      this.x = x;
      return this;
    }

    public Builder y(int y) {
      this.y = y;
      return this;
    }

    public Builder position(int x, int y) {
      this.x = x;
      this.y = y;
      return this;
    }

    public Builder width(int width) {
      this.width = width;
      return this;
    }

    public Builder height(int height) {
      this.height = height;
      return this;
    }

    public Builder dimensions(int width, int height) {
      this.width = width;
      this.height = height;
      return this;
    }

    public Builder border(boolean border) {
      this.border = border;
      return this;
    }

    public PaintingSpriteWidget build() {
      return new PaintingSpriteWidget(this.x, this.y, this.width, this.height, this.paintingData, this.border);
    }
  }
}
