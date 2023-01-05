package me.roundaround.custompaintings.client.gui.screen;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class PaintingFilterScreen extends Screen {
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 20;

  private final PaintingEditScreen parent;

  protected TextFieldWidget searchBox;

  public PaintingFilterScreen(PaintingEditScreen parent) {
    super(Text.translatable("custompaintings.filter.title"));
    this.parent = parent;
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        resetFilters();
        this.close();
        return true;
      case GLFW.GLFW_KEY_F:
        if (hasControlDown() && !hasShiftDown() && !hasAltDown()) {
          playClickSound();
          this.close();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_ENTER:
        playClickSound();
        this.close();
        return true;
    }

    if (super.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }

    return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    return this.searchBox.charTyped(chr, modifiers);
  }

  @Override
  public void init() {
    this.client.keyboard.setRepeatEvents(true);
    this.searchBox = new TextFieldWidget(
        this.textRenderer,
        this.width / 2 - BUTTON_WIDTH,
        22,
        BUTTON_WIDTH * 2,
        BUTTON_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.filter.search"));
    this.searchBox.setChangedListener((search) -> {
      setSearchText(search);
    });

    this.addSelectableChild(this.searchBox);

    addDrawableChild(new ButtonWidget(
        this.width / 2 - BUTTON_WIDTH - 2,
        this.height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.filter.close"),
        (button) -> {
          this.client.setScreen(this.parent);
        }));

    addDrawableChild(new ButtonWidget(
        this.width / 2 + 2,
        this.height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.filter.reset"),
        (button) -> {
          resetFilters();
        }));

    this.setInitialFocus(this.searchBox);
  }

  @Override
  public void tick() {
    this.searchBox.tick();
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackgroundInRegion(0, height, 0, width);
    this.searchBox.render(matrixStack, mouseX, mouseY, partialTicks);
    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  private void renderBackgroundInRegion(int top, int bottom, int left, int right) {
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

    int headerBottom = top + getHeaderHeight();
    int footerTop = bottom - getFooterHeight();

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder
        .vertex(0, footerTop, 0)
        .texture(0, footerTop / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(width, footerTop, 0)
        .texture(width / 32f, footerTop / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(width, headerBottom, 0)
        .texture(width / 32f, headerBottom / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(0, headerBottom, 0)
        .texture(0, headerBottom / 32f)
        .color(32, 32, 32, 255)
        .next();
    tessellator.draw();
  }

  private int getHeaderHeight() {
    // return 8 + this.textRenderer.fontHeight + BUTTON_HEIGHT;
    return 48;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT + 10;
  }

  private void playClickSound() {
    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
  }

  private void resetFilters() {
    // TODO
  }

  private void setSearchText(String text) {
    // TODO
  }
}
