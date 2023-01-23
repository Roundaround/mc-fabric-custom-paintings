package me.roundaround.custompaintings.client.gui.screen.edit;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.FilterListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.OrderableTooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public class FiltersScreen extends PaintingEditScreen {
  private FilterListWidget filtersListWidget;

  public FiltersScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.filters.title"), state);
  }

  @Override
  public void init() {
    this.filtersListWidget = new FilterListWidget(
        this.state,
        this.client,
        this.filtersListWidget,
        this.width,
        this.height,
        this.getHeaderHeight(),
        this.height - this.getFooterHeight());

    ButtonWidget resetButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH - 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.filter.reset"),
        (button) -> {
          this.state.getFilters().reset();
          this.filtersListWidget.updateFilters();
        });

    ButtonWidget doneButton = new ButtonWidget(
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          this.client.setScreen(new PaintingSelectScreen(this.state));
        });

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
  public void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    matrixStack.push();
    matrixStack.translate(0, 0, 10);
    this.filtersListWidget.render(matrixStack, mouseX, mouseY, partialTicks);
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
        Text.translatable("custompaintings.filter.title"),
        width / 2,
        11,
        0xFFFFFFFF);

    List<OrderedText> tooltip = this.filtersListWidget.getHoveredElement(mouseX, mouseY)
        .map((element) -> {
          if (element instanceof OrderableTooltip) {
            return ((OrderableTooltip) element).getOrderedTooltip();
          }
          return new ArrayList<OrderedText>();
        })
        .orElse(new ArrayList<OrderedText>());

    if (!tooltip.isEmpty()) {
      renderOrderedTooltip(matrixStack, tooltip, mouseX, mouseY);
    }
  }

  private int getHeaderHeight() {
    return 10 + textRenderer.fontHeight + 2 + 10;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT + 10;
  }
}
