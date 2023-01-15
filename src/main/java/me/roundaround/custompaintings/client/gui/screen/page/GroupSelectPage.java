package me.roundaround.custompaintings.client.gui.screen.page;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.widget.GroupListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class GroupSelectPage extends PaintingEditScreenPage {
  private GroupListWidget groupsListWidget;

  public GroupSelectPage(
      PaintingEditScreen parent,
      MinecraftClient client,
      int width,
      int height) {
    super(parent, client, width, height);
  }

  @Override
  public void init() {
    this.groupsListWidget = new GroupListWidget(
        this.parent,
        this.client,
        this.width,
        this.height,
        this.getHeaderHeight(),
        this.height - this.getFooterHeight(),
        (id) -> this.parent.selectGroup(id));
    this.groupsListWidget.setGroups(this.parent.getGroups());

    addSelectableChild(this.groupsListWidget);
    addDrawableChild(
        new ButtonWidget(
            (this.width - BUTTON_WIDTH) / 2,
            this.height - BUTTON_HEIGHT - 10,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            ScreenTexts.CANCEL,
            (button) -> {
              this.parent.saveEmpty();
            }));
  }

  @Override
  public boolean preKeyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        this.parent.saveEmpty();
        return true;
    }

    return false;
  }

  @Override
  public void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    matrixStack.push();
    matrixStack.translate(0, 0, 10);
    groupsListWidget.render(matrixStack, mouseX, mouseY, partialTicks);
    matrixStack.pop();

    matrixStack.push();
    matrixStack.translate(0, 0, 11);
    renderBackgroundInRegion(0, getHeaderHeight(), 0, width);
    renderBackgroundInRegion(height - getFooterHeight(), height, 0, width);
    matrixStack.pop();
  }

  @Override
  public void renderForeground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    drawCenteredText(
        matrixStack,
        textRenderer,
        Text.translatable("custompaintings.group.title"),
        width / 2,
        11,
        0xFFFFFFFF);
  }

  private int getHeaderHeight() {
    return 10 + textRenderer.fontHeight + 2 + 10;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT + 10;
  }
}
