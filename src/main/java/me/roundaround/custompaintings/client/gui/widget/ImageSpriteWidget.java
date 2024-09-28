package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.util.IntRect;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;

public class ImageSpriteWidget extends DrawableWidget {
  private PaintingData paintingData;
  private boolean border = false;
  private Sprite sprite;
  private IntRect paintingBounds = IntRect.zero();
  private boolean inBatchUpdate = false;

  private ImageSpriteWidget(int x, int y, int width, int height, PaintingData paintingData, boolean border) {
    super(x, y, width, height);

    this.paintingData = paintingData;
    this.border = border;
    this.sprite = ClientPaintingRegistry.getInstance().getSprite(paintingData);
    this.calculateBounds();
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

  public void setPaintingData(PaintingData paintingData) {
    this.visible = paintingData != null && !paintingData.isEmpty();
    this.paintingData = paintingData != null ? paintingData : PaintingData.EMPTY;
    this.sprite = ClientPaintingRegistry.getInstance().getSprite(this.paintingData);
    this.calculateBounds();
  }

  public void setBorder(boolean border) {
    this.border = border;
    this.calculateBounds();
  }

  public void setActive(boolean active) {
    this.active = active;
  }

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

    this.paintingBounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2, y + (height - scaledHeight) / 2, scaledWidth, scaledHeight);
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    this.hovered = this.hovered && this.paintingBounds.contains(mouseX, mouseY);

    float color = this.active ? 1f : 0.5f;

    if (this.border) {
      context.fill(this.paintingBounds.left() - 1, this.paintingBounds.top() - 1, this.paintingBounds.right() + 1,
          this.paintingBounds.bottom() + 1, 0xFF000000
      );
    }

    context.drawSprite(this.paintingBounds.left(), this.paintingBounds.top(), 1, this.paintingBounds.getWidth(),
        this.paintingBounds.getHeight(), this.sprite, color, color, color, 1f
    );
  }

  public static Builder builder(PaintingData paintingData) {
    return new Builder(paintingData);
  }

  public static Builder builder(String packId) {
    return builder(PaintingData.packIcon(packId));
  }

  public static ImageSpriteWidget create(PaintingData paintingData) {
    return builder(paintingData).build();
  }

  public static ImageSpriteWidget create(String packId) {
    return builder(packId).build();
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

    public ImageSpriteWidget build() {
      return new ImageSpriteWidget(this.x, this.y, this.width, this.height, this.paintingData, this.border);
    }
  }
}
