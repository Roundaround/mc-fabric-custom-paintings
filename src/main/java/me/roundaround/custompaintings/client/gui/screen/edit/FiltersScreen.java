package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.FilterListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class FiltersScreen extends PaintingEditScreen {
  private FilterListWidget filtersListWidget;

  public FiltersScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.filter.title"), state);
  }

  @Override
  public void init() {
    this.filtersListWidget = new FilterListWidget(this.state,
        this.client,
        this.filtersListWidget,
        this.width,
        this.height - this.getHeaderHeight() - this.getFooterHeight(),
        this.getHeaderHeight());

    ButtonWidget resetButton =
        ButtonWidget.builder(Text.translatable("custompaintings.filter.reset"), (button) -> {
              this.state.getFilters().reset();
              this.filtersListWidget.updateFilters();
            })
            .position(width / 2 - TWO_COL_BUTTON_WIDTH - 2, height - BUTTON_HEIGHT - 10)
            .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();

    ButtonWidget doneButton = ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
          this.client.setScreen(new PaintingSelectScreen(this.state));
        })
        .position(width / 2 + 2, height - BUTTON_HEIGHT - 10)
        .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();

    addSelectableChild(this.filtersListWidget);
    addDrawableChild(resetButton);
    addDrawableChild(doneButton);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        this.client.setScreen(new PaintingSelectScreen(this.state));
        return true;
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void tick() {
    this.filtersListWidget.tick();
  }

  @Override
  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    this.renderBasicListBackground(drawContext,
        mouseX,
        mouseY,
        partialTicks,
        this.filtersListWidget);
  }
}
