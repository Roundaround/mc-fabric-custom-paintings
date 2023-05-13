package me.roundaround.custompaintings.client.gui.screen.edit;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.GroupListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class GroupSelectScreen extends PaintingEditScreen {
  private GroupListWidget groupsListWidget;

  public GroupSelectScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.group.title"), state);
  }

  @Override
  public void init() {
    this.groupsListWidget = new GroupListWidget(
        this,
        this.client,
        this.width,
        this.height,
        this.getHeaderHeight(),
        this.height - this.getFooterHeight(),
        (id) -> {
          this.state.setCurrentGroup(id);
          this.client.setScreen(new PaintingSelectScreen(this.state));
        });
    this.groupsListWidget.setGroups(this.state.getGroups());

    addSelectableChild(this.groupsListWidget);
    addDrawableChild(ButtonWidget.builder(
        ScreenTexts.CANCEL,
        (button) -> {
          saveEmpty();
        })
        .position((this.width - BUTTON_WIDTH) / 2, this.height - BUTTON_HEIGHT - 10)
        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        saveEmpty();
        return true;
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
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
