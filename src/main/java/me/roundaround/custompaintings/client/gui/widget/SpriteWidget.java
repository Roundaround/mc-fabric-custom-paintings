package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.util.IntRect;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.CommonColors;

public class SpriteWidget extends DrawableWidget {
  protected TextureAtlasSprite sprite;
  protected IntRect imageBounds = IntRect.zero();
  protected boolean inBatchUpdate = false;

  protected SpriteWidget(int x, int y, int width, int height, TextureAtlasSprite sprite) {
    this(x, y, width, height, sprite, true);
  }

  protected SpriteWidget(
      int x,
      int y,
      int width,
      int height,
      TextureAtlasSprite sprite,
      boolean immediatelyCalculateBounds
  ) {
    super(x, y, width, height);

    this.sprite = sprite;
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
  public void setSize(int width, int height) {
    super.setSize(width, height);
    this.calculateBounds();
  }

  public void setSprite(TextureAtlasSprite sprite) {
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

    SpriteContents spriteContents = this.sprite.contents();
    int imageWidth = spriteContents.width();
    int imageHeight = spriteContents.height();
    float scale = Math.min((float) this.getWidth() / imageWidth, (float) this.getHeight() / imageHeight);
    int scaledWidth = Math.round(scale * imageWidth);
    int scaledHeight = Math.round(scale * imageHeight);

    this.imageBounds = IntRect.byDimensions(
        x + (width - scaledWidth) / 2,
        y + (height - scaledHeight) / 2,
        scaledWidth,
        scaledHeight
    );
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    this.isHovered = this.isHovered && this.imageBounds.contains(mouseX, mouseY);

    context.blitSprite(
        RenderPipelines.GUI_TEXTURED,
        this.sprite,
        this.imageBounds.left(),
        this.imageBounds.top(),
        this.imageBounds.getWidth(),
        this.imageBounds.getHeight(),
        this.active ? CommonColors.WHITE : GuiUtil.genColorInt(0.5f, 0.5f, 0.5f, 1f)
    );
  }

  public static Builder builder(TextureAtlasSprite sprite) {
    return new Builder(sprite);
  }

  public static SpriteWidget create(TextureAtlasSprite sprite) {
    return builder(sprite).build();
  }

  public static class Builder {
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;

    private final TextureAtlasSprite sprite;

    private Builder(TextureAtlasSprite sprite) {
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
