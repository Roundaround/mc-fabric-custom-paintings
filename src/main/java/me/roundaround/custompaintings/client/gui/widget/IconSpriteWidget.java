package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.roundalib.client.gui.util.Dimensions;
import me.roundaround.roundalib.client.gui.util.IntRect;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;

public class IconSpriteWidget extends DrawableWidget {
  private boolean border = false;
  private Sprite sprite;
  private IntRect bounds = IntRect.zero();
  private boolean inBatchUpdate = false;

  private IconSpriteWidget(int x, int y, int width, int height, String packId, boolean border) {
    super(x, y, width, height);

    this.border = border;
    this.sprite = ClientPaintingRegistry.getInstance().getSprite(PackIcons.identifier(packId));
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

  public void setPackId(String packId) {
    this.sprite = ClientPaintingRegistry.getInstance().getSprite(PackIcons.identifier(packId));
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

    Dimensions scaledDimensions = PackIcons.getScaledDimensions(
        this.sprite.getContents().getWidth(), this.sprite.getContents().getHeight(), width, height);
    int scaledWidth = scaledDimensions.width();
    int scaledHeight = scaledDimensions.height();

    this.bounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2, y + (height - scaledHeight) / 2, scaledWidth, scaledHeight);
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    this.hovered = this.hovered && this.bounds.contains(mouseX, mouseY);

    float color = this.active ? 1f : 0.5f;

    if (this.border) {
      context.fill(this.bounds.left() - 1, this.bounds.top() - 1, this.bounds.right() + 1, this.bounds.bottom() + 1,
          0xFF000000
      );
    }

    context.drawSprite(this.bounds.left(), this.bounds.top(), 1, this.bounds.getWidth(), this.bounds.getHeight(),
        this.sprite, color, color, color, 1f
    );
  }

  public static Builder builder(String packId) {
    return new Builder(packId);
  }

  public static IconSpriteWidget create(String packId) {
    return builder(packId).build();
  }

  public static class Builder {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private boolean border = false;

    private final String packId;

    private Builder(String packId) {
      this.packId = packId;
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

    public IconSpriteWidget build() {
      return new IconSpriteWidget(this.x, this.y, this.width, this.height, this.packId, this.border);
    }
  }
}
