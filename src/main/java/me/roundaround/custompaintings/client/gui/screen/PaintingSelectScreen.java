package me.roundaround.custompaintings.client.gui.screen;

import java.util.ArrayList;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.widget.ButtonWithDisabledTooltipWidget;
import me.roundaround.custompaintings.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaintingSelectScreen extends PaintingEditScreen {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private ArrayList<PaintingData> paintings = new ArrayList<>();

  public PaintingSelectScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.painting.title"), state);
  }

  public void setScrollAmount(double scrollAmount) {
    this.scrollAmount = scrollAmount;
  }

  public double getScrollAmount() {
    return this.scrollAmount;
  }

  public void populateWithAllPaintings() {
    this.state.getCurrentGroup().paintings().forEach((paintingData) -> {
      this.paintings.add(paintingData);
    });
  }

  public void updateFilters() {
    if (!this.state.getFilters().hasFilters()) {
      this.paintings = this.state.getCurrentGroup().paintings();
      if (this.paintingList != null) {
        this.paintingList.setPaintings(this.paintings);
      }
      return;
    }

    // Manually iterate to guarantee order
    this.paintings = new ArrayList<>();
    this.state.getCurrentGroup().paintings().forEach((paintingData) -> {
      if (this.state.getFilters().test(paintingData)) {
        this.paintings.add(paintingData);
      }
    });

    if (this.paintingList != null) {
      this.paintingList.setPaintings(this.paintings);
    }
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public void init() {
    super.init();

    updateFilters();

    PaintingData paintingData = this.state.getCurrentPainting();
    boolean canStay = this.state.canStay(paintingData);
    Text tooBigTooltip = Text.translatable("custompaintings.painting.big", paintingData.width(), paintingData.height());

    this.paneWidth = this.width / 2 - 8;
    this.rightPaneX = this.width - this.paneWidth;

    int headerHeight = getHeaderHeight();
    int footerHeight = getFooterHeight();

    // 10px padding on each side, 4px between search box and filter button
    this.searchBox = new TextFieldWidget(
        this.textRenderer,
        10,
        headerHeight + 4,
        this.paneWidth - IconButtonWidget.WIDTH - 24,
        BUTTON_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.painting.search"));
    this.searchBox.setText(this.state.getFilters().getSearch());
    this.searchBox.setChangedListener((search) -> {
      onSearchBoxChanged(search);
    });

    IconButtonWidget filterButton = new IconButtonWidget(
        this,
        10 + this.searchBox.getWidth() + 4,
        this.searchBox.y,
        0,
        Text.translatable("custompaintings.painting.filter"),
        (button) -> {
          this.client.setScreen(new FiltersScreen(this.state));
        });

    int listTop = this.searchBox.y + this.searchBox.getHeight() + 4;
    int listBottom = this.height - footerHeight - 4;
    int listHeight = listBottom - listTop;
    this.paintingList = new PaintingListWidget(
        this,
        this.state,
        this.client,
        this.paneWidth,
        listHeight,
        listTop,
        listBottom,
        this.paintings);

    int paintingTop = headerHeight + 8
        + (paintingData.hasLabel() ? 3 : 2) * textRenderer.fontHeight
        + (paintingData.hasLabel() ? 2 : 1) * 2;
    int paintingBottom = this.height - footerHeight - 8 - BUTTON_HEIGHT;

    int maxWidth = this.paneWidth / 2 - 8;
    int maxHeight = paintingBottom - paintingTop;

    int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
    int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

    PaintingButtonWidget paintingButton = new PaintingButtonWidget(
        this,
        this.textRenderer,
        this.rightPaneX + (this.paneWidth - scaledWidth) / 2,
        (paintingTop + paintingBottom - scaledHeight) / 2,
        scaledWidth,
        scaledHeight,
        (button) -> {
          saveSelection(paintingData);
        },
        canStay,
        tooBigTooltip,
        paintingData);

    ButtonWidget prevButton = new ButtonWidget(
        this.rightPaneX + this.paneWidth / 2 - BUTTON_WIDTH - 2,
        this.height - footerHeight - 4 - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.previous"),
        (button) -> {
          previousPainting();
        });

    ButtonWidget nextButton = new ButtonWidget(
        this.rightPaneX + this.paneWidth / 2 + 2,
        this.height - footerHeight - 4 - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.next"),
        (button) -> {
          nextPainting();
        });

    if (!hasMultiplePaintings()) {
      prevButton.active = false;
      nextButton.active = false;
    }

    ButtonWidget cancelButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH - 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.CANCEL,
        (button) -> {
          if (this.state.hasMultipleGroups()) {
            this.client.setScreen(new GroupSelectScreen(this.state));
          } else {
            saveEmpty();
          }
        });

    ButtonWidget doneButton = new ButtonWithDisabledTooltipWidget(
        this,
        this.textRenderer,
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          saveCurrentSelection();
        },
        canStay,
        tooBigTooltip);

    addSelectableChild(this.searchBox);
    addDrawableChild(filterButton);
    addSelectableChild(this.paintingList);
    addDrawableChild(paintingButton);
    addDrawableChild(prevButton);
    addDrawableChild(nextButton);
    addDrawableChild(cancelButton);
    addDrawableChild(doneButton);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      if (this.state.hasMultipleGroups()) {
        this.client.setScreen(new GroupSelectScreen(this.state));
      } else {
        saveEmpty();
      }
      return true;
    }
    switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT:
        if (this.state.hasMultiplePaintings()) {
          playClickSound();
          previousPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_RIGHT:
        if (this.state.hasMultiplePaintings()) {
          playClickSound();
          nextPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        if (this.state.hasMultipleGroups()) {
          this.client.setScreen(new GroupSelectScreen(this.state));
        } else {
          saveEmpty();
        }
        return true;
      case GLFW.GLFW_KEY_ENTER:
        if (!this.paintingList.isFocused()) {
          break;
        }
        Optional<PaintingData> painting = this.paintingList.getSelectedPainting();
        if (painting.isPresent() && this.state.canStay(painting.get())) {
          playClickSound();
          saveSelection(painting.get());
          return true;
        }
        break;
      case GLFW.GLFW_KEY_F:
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
          if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            this.client.setScreen(new FiltersScreen(this.state));
            return true;
          }
        }
        break;
    }

    return this.searchBox.keyPressed(keyCode, scanCode, modifiers)
        || this.paintingList.keyPressed(keyCode, scanCode, modifiers)
        || super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int keyCode) {
    return this.searchBox.charTyped(chr, keyCode);
  }

  @Override
  public void tick() {
    this.searchBox.tick();
  }

  @Override
  public void renderBackground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackgroundInRegion(0, height, 0, width);
  }

  @Override
  public void renderForeground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.paintingList.render(matrixStack, mouseX, mouseY, partialTicks);
    this.searchBox.render(matrixStack, mouseX, mouseY, partialTicks);

    Group currentGroup = this.state.getCurrentGroup();

    MutableText title = this.title.copy();
    if (this.state.hasMultipleGroups()) {
      title = Text.literal(this.state.getCurrentGroup().name() + " - ").append(title);
    }

    drawCenteredText(
        matrixStack,
        textRenderer,
        title,
        width / 2,
        11,
        0xFFFFFFFF);

    PaintingData paintingData = this.state.getCurrentPainting();
    int currentPaintingIndex = currentGroup.paintings().indexOf(paintingData);
    int posX = this.rightPaneX + paneWidth / 2;
    int posY = getHeaderHeight() + 4;

    if (paintingData.hasLabel()) {
      drawCenteredText(
          matrixStack,
          textRenderer,
          paintingData.getLabel(),
          posX,
          posY,
          0xFFFFFFFF);

      posY += textRenderer.fontHeight + 2;
    }

    drawCenteredText(
        matrixStack,
        textRenderer,
        Text.literal("(" + paintingData.id().toString() + ")")
            .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
        posX,
        posY,
        0xFFFFFFFF);

    posY += textRenderer.fontHeight + 2;

    drawCenteredText(
        matrixStack,
        textRenderer,
        Text.translatable(
            "custompaintings.painting.dimensions",
            paintingData.width(),
            paintingData.height()),
        posX,
        posY,
        0xFFFFFFFF);

    drawCenteredText(
        matrixStack,
        textRenderer,
        Text.translatable(
            "custompaintings.painting.number",
            currentPaintingIndex + 1,
            currentGroup.paintings().size()),
        posX,
        this.height
            - getFooterHeight()
            - 4
            - BUTTON_HEIGHT
            + (BUTTON_HEIGHT - textRenderer.fontHeight) / 2,
        0xFFFFFFFF);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, amount);
    }
    return false;
  }

  private boolean hasMultiplePaintings() {
    return this.paintings.size() > 1;
  }

  private void previousPainting() {
    if (!hasMultiplePaintings()) {
      return;
    }

    this.state.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      if (currentIndex == -1) {
        currentIndex = 0;
      }
      int nextIndex = (this.paintings.size() + currentIndex - 1) % this.paintings.size();
      return this.paintings.get(nextIndex);
    });
  }

  private void nextPainting() {
    if (!hasMultiplePaintings()) {
      return;
    }

    this.state.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      int nextIndex = (currentIndex + 1) % this.paintings.size();
      return this.paintings.get(nextIndex);
    });
  }

  private void saveCurrentSelection() {
    Group currentGroup = this.state.getCurrentGroup();
    PaintingData currentPainting = this.state.getCurrentPainting();
    if (currentGroup == null || currentPainting == null) {
      saveEmpty();
    }
    saveSelection(currentPainting);
  }

  private int getHeaderHeight() {
    return 11 + textRenderer.fontHeight + 10;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT;
  }

  private void onSearchBoxChanged(String text) {
    if (this.state.getFilters().getSearch().equals(text)) {
      return;
    }

    this.state.getFilters().setSearch(text);
    updateFilters();
  }
}
