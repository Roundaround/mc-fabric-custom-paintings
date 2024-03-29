package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.GroupListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class GroupSelectScreen extends PaintingEditScreen {
  private GroupListWidget groupsListWidget;

  public GroupSelectScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.group.title"), state);
  }

  @Override
  public void init() {
    this.groupsListWidget = new GroupListWidget(this,
        this.client,
        this.width,
        this.height - this.getHeaderHeight() - this.getFooterHeight(),
        this.getHeaderHeight(),
        (id) -> {
          this.state.setCurrentGroup(id);
          this.client.setScreen(new PaintingSelectScreen(this.state));
        });
    this.groupsListWidget.setGroups(this.state.getGroups());

    addSelectableChild(this.groupsListWidget);
    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          saveEmpty();
        })
        .position((this.width - ONE_COL_BUTTON_WIDTH) / 2, this.height - BUTTON_HEIGHT - 10)
        .size(ONE_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
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
  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    renderBasicListBackground(drawContext, mouseX, mouseY, partialTicks, this.groupsListWidget);
  }
}
