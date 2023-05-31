package me.roundaround.custompaintings.client.gui.screen.edit;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public abstract class PaintingEditScreen extends Screen {
  protected static final int BUTTON_WIDTH = 100;
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int ICON_BUTTON_WIDTH = 20;
  protected static final int ICON_BUTTON_HEIGHT = 20;

  protected final PaintingEditState state;

  protected PaintingEditScreen(Text title, PaintingEditState state) {
    super(title);
    this.state = state;
  }

  public PaintingEditState getState() {
    return this.state;
  }

  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
  }

  public void renderForeground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
  }

  @Override
  public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    renderBackground(drawContext, mouseX, mouseY, partialTicks);

    MatrixStack matrixStack = drawContext.getMatrices();
    matrixStack.push();
    matrixStack.translate(0, 0, 12);
    renderForeground(drawContext, mouseX, mouseY, partialTicks);
    super.render(drawContext, mouseX, mouseY, partialTicks);
    matrixStack.pop();
  }

  protected void saveEmpty() {
    saveSelection(PaintingData.EMPTY);
  }

  public void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(this.state.getPaintingUuid(), paintingData);
    close();
  }

  protected void renderBackgroundInRegion(int top, int bottom, int left, int right) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();
    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder.vertex(left, bottom, 0)
        .texture(left / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder.vertex(right, bottom, 0)
        .texture(right / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder.vertex(right, top, 0)
        .texture(right / 32f, top / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder.vertex(left, top, 0).texture(left / 32f, top / 32f).color(64, 64, 64, 255).next();
    tessellator.draw();
  }

  public void playClickSound() {
    this.client.getSoundManager()
        .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
  }
}
