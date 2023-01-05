package me.roundaround.custompaintings.client.gui.screen.page;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public abstract class PaintingEditScreenPage extends DrawableHelper {
  protected static final int BUTTON_WIDTH = 100;
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int ICON_BUTTON_WIDTH = 20;
  protected static final int ICON_BUTTON_HEIGHT = 20;

  protected final PaintingEditScreen parent;

  protected final MinecraftClient client;
  protected final TextRenderer textRenderer;
  protected final int width;
  protected final int height;

  protected PaintingEditScreenPage(PaintingEditScreen parent, MinecraftClient client, int width, int height) {
    this.parent = parent;
    this.client = client;
    this.textRenderer = client.textRenderer;
    this.width = width;
    this.height = height;
  }

  public abstract void init();

  public abstract void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks);

  public abstract void renderForeground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks);
  
  public boolean preKeyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }

  public boolean postKeyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }

  public boolean charTyped(char chr, int modifiers) {
    return false;
  }

  public void tick() {

  }

  protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
    return this.parent.addDrawableChild(drawableElement);
  }

  protected <T extends Drawable> T addDrawable(T drawable) {
    return this.parent.addDrawable(drawable);
  }

  protected <T extends Element & Selectable> T addSelectableChild(T child) {
    return this.parent.addSelectableChild(child);
  }

  protected void playClickSound() {
    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
  }

  protected void renderBackgroundInRegion(int top, int bottom, int left, int right) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder
        .vertex(left, bottom, 0)
        .texture(left / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(right, bottom, 0)
        .texture(right / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(right, top, 0)
        .texture(right / 32f, top / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(left, top, 0)
        .texture(left / 32f, top / 32f)
        .color(64, 64, 64, 255)
        .next();
    tessellator.draw();
  }
}
