package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.layout.IntRect;
import me.roundaround.roundalib.client.gui.widget.DrawableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;

public class PaintingSpriteWidget extends DrawableWidget {
  private PaintingData paintingData;
  private boolean border = false;
  private Sprite sprite;
  private IntRect paintingBounds = IntRect.zero();
  private boolean inBatchUpdate = false;

  public PaintingSpriteWidget(PaintingData paintingData) {
    this(0, 0, paintingData);
  }

  public PaintingSpriteWidget(PaintingData paintingData, boolean border) {
    this(0, 0, paintingData, border);
  }

  public PaintingSpriteWidget(int width, int height, PaintingData paintingData) {
    this(0, 0, width, height, paintingData);
  }

  public PaintingSpriteWidget(int width, int height, PaintingData paintingData, boolean border) {
    this(0, 0, width, height, paintingData, border);
  }

  public PaintingSpriteWidget(int x, int y, int width, int height, PaintingData paintingData) {
    this(x, y, width, height, paintingData, false);
  }

  public PaintingSpriteWidget(int x, int y, int width, int height, PaintingData paintingData, boolean border) {
    super(x, y, width, height);

    this.paintingData = paintingData;
    this.border = border;
    this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
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
    this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(this.paintingData);
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

    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    RenderSystem.setShaderColor(color, color, color, 1f);
    RenderSystem.setShaderTexture(0, this.sprite.getAtlasId());
    context.drawSprite(this.paintingBounds.left(), this.paintingBounds.top(), 1, this.paintingBounds.getWidth(),
        this.paintingBounds.getHeight(), this.sprite
    );
  }
}
