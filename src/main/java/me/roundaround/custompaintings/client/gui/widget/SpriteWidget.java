package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.roundalib.client.gui.util.IntRect;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;

public class SpriteWidget extends DrawableWidget {
  protected Sprite sprite;
  protected IntRect imageBounds = IntRect.zero();
  protected boolean inBatchUpdate = false;

  protected SpriteWidget(int x, int y, int width, int height, Sprite sprite) {
    super(x, y, width, height);

    this.sprite = sprite;
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

  public void setSprite(Sprite sprite) {
    this.sprite = sprite;
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

    SpriteContents spriteContents = this.sprite.getContents();
    int imageWidth = spriteContents.getWidth();
    int imageHeight = spriteContents.getHeight();
    float scale = Math.min((float) this.getWidth() / imageWidth, (float) this.getHeight() / imageHeight);
    int scaledWidth = Math.round(scale * imageWidth);
    int scaledHeight = Math.round(scale * imageHeight);

    this.imageBounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2, y + (height - scaledHeight) / 2, scaledWidth, scaledHeight);
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    this.hovered = this.hovered && this.imageBounds.contains(mouseX, mouseY);

    float color = this.active ? 1f : 0.5f;

    context.drawSprite(this.imageBounds.left(), this.imageBounds.top(), 1, this.imageBounds.getWidth(),
        this.imageBounds.getHeight(), this.sprite, color, color, color, 1f
    );
  }

  public static Builder builder(Sprite sprite) {
    return new Builder(sprite);
  }

  public static SpriteWidget create(Sprite sprite) {
    return builder(sprite).build();
  }

  public static class Builder {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;

    private final Sprite sprite;

    private Builder(Sprite sprite) {
      this.sprite = sprite;
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

    public SpriteWidget build() {
      return new SpriteWidget(this.x, this.y, this.width, this.height, this.sprite);
    }
  }
}
