package me.roundaround.custompaintings.client.gui.screen.page;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class FiltersPage extends PaintingEditScreenPage {
  public FiltersPage(
      PaintingEditScreen parent,
      MinecraftClient client,
      int width,
      int height) {
    super(parent, client, width, height);
  }

  @Override
  public void init() {
    // TODO: Min width: >/</=/<>/etc
    // TODO: Min height: >/</=/<>/etc
    // TODO: Max width: >/</=/<>/etc
    // TODO: Max height: >/</=/<>/etc
    // TODO: Can stay only: true/false
    // TODO: Title: string search
    // TODO: Author: string search

    ButtonWidget resetButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH - 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.filter.reset"),
        (button) -> {
          this.parent.getFilters().reset();
        });

    ButtonWidget doneButton = new ButtonWidget(
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          this.parent.returnToPaintingSelect();
        });
        
    addDrawableChild(resetButton);
    addDrawableChild(doneButton);
  }

  @Override
  public boolean preKeyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        this.parent.returnToPaintingSelect();
        return true;
    }

    return false;
  }

  @Override
  public void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackgroundInRegion(0, height, 0, width);
  }

  @Override
  public void renderForeground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    
  }
}
