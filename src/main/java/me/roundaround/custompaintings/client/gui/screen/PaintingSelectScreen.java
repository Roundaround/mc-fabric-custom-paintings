package me.roundaround.custompaintings.client.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.PaintingEditState.PaintingChangeListener;
import me.roundaround.custompaintings.client.gui.widget.ButtonWithDisabledTooltipWidget;
import me.roundaround.custompaintings.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingChangeListener {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private ButtonWidget prevButton;
  private ButtonWidget nextButton;
  private ButtonWithDisabledTooltipWidget doneButton;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private ArrayList<PaintingData> paintings = new ArrayList<>();
  private boolean hasHiddenPaintings = false;
  private boolean canStay = true;
  private List<OrderedText> tooBigTooltip = List.of();
  private Sprite paintingSprite = null;
  private Rect2i paintingRect = new Rect2i(0, 0, 0, 0);

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

  @Override
  public void onPaintingChange(PaintingData paintingData) {
    this.canStay = this.state.canStay(paintingData);
    this.tooBigTooltip = this.textRenderer.wrapLines(
        Text.translatable(
            "custompaintings.painting.big",
            paintingData.width(),
            paintingData.height()),
        200);
    this.paintingSprite = CustomPaintingsClientMod.customPaintingManager
        .getPaintingSprite(paintingData);

    int headerHeight = getHeaderHeight();
    int footerHeight = getFooterHeight();

    int paintingTop = headerHeight + 8
        + (paintingData.hasLabel() ? 3 : 2) * textRenderer.fontHeight
        + (paintingData.hasLabel() ? 2 : 1) * 2;
    int paintingBottom = this.height - footerHeight - 8 - BUTTON_HEIGHT;

    int maxWidth = paneWidth - 2 * 8;
    int maxHeight = paintingBottom - paintingTop;

    int width = paintingData.getScaledWidth(maxWidth, maxHeight);
    int height = paintingData.getScaledHeight(maxWidth, maxHeight);

    int x = this.rightPaneX + (this.paneWidth - width) / 2;
    int y = (paintingTop + paintingBottom - height) / 2;

    this.paintingRect = new Rect2i(x, y, width, height);

    if (this.paintingList != null) {
      this.paintingList.selectPainting(paintingData);
    }

    if (this.doneButton != null) {
      this.doneButton.active = this.canStay;
      this.doneButton.disabledTooltip = this.tooBigTooltip;
    }
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public void init() {
    applyFilters();
    onPaintingChange(this.state.getCurrentPainting());

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

    this.prevButton = new ButtonWidget(
        this.rightPaneX + this.paneWidth / 2 - BUTTON_WIDTH - 2,
        this.height - footerHeight - 4 - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.previous"),
        (button) -> {
          previousPainting();
        });

    this.nextButton = new ButtonWidget(
        this.rightPaneX + this.paneWidth / 2 + 2,
        this.height - footerHeight - 4 - BUTTON_HEIGHT,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.next"),
        (button) -> {
          nextPainting();
        });

    this.prevButton.active = hasMultiplePaintings();
    this.nextButton.active = hasMultiplePaintings();

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

    this.doneButton = new ButtonWithDisabledTooltipWidget(
        this,
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          saveCurrentSelection();
        });

    this.doneButton.active = this.canStay;
    this.doneButton.disabledTooltip = this.tooBigTooltip;

    addSelectableChild(this.searchBox);
    addDrawableChild(filterButton);
    addSelectableChild(this.paintingList);
    addDrawableChild(prevButton);
    addDrawableChild(nextButton);
    addDrawableChild(cancelButton);
    addDrawableChild(doneButton);

    setInitialFocus(this.searchBox);

    this.paintingList.selectFirst();
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        if (hasControlDown()) {
          if (hasShiftDown()) {
            this.client.setScreen(new FiltersScreen(this.state));
            return true;
          }

          Element focused = getFocused();
          if (focused != this.searchBox) {
            if (focused != null) {
              focused.changeFocus(false);
            }

            setInitialFocus(this.searchBox);
            return true;
          }
        }
        break;
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
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

    renderBackgroundInRegion(this.paintingList.getBottom(), height, 0, width);

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

    if (paintingData.isEmpty()) {
      int paneHeight = this.height - this.getHeaderHeight() - this.getFooterHeight();
      int posX = this.rightPaneX + paneWidth / 2;
      int posY = getHeaderHeight() + (paneHeight - this.textRenderer.fontHeight) / 2;

      drawCenteredText(
          matrixStack,
          textRenderer,
          Text.translatable("custompaintings.painting.none")
              .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
          posX,
          posY,
          0xFFFFFFFF);

    } else {
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

      renderPainting(matrixStack, mouseX, mouseY);
    }
  }

  private void renderPainting(MatrixStack matrixStack, int mouseX, int mouseY) {
    int x = this.paintingRect.getX();
    int y = this.paintingRect.getY();
    int width = this.paintingRect.getWidth();
    int height = this.paintingRect.getHeight();
    float color = this.canStay ? 1f : 0.5f;

    fill(matrixStack, x, y, x + width, y + height, 0xFF000000);

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(color, color, color, 1f);
    RenderSystem.setShaderTexture(0, this.paintingSprite.getAtlas().getId());
    drawSprite(matrixStack, x + 1, y + 1, 0, width - 2, height - 2, this.paintingSprite);

    if (!this.canStay
        && mouseX >= x && mouseX < x + width
        && mouseY >= y && mouseY < y + height) {
      renderOrderedTooltip(
          matrixStack,
          this.tooBigTooltip,
          mouseX,
          mouseY);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, amount);
    }
    return false;
  }

  private void applyFilters() {
    if (!this.state.getFilters().hasFilters()) {
      this.hasHiddenPaintings = false;
      this.paintings = this.state.getCurrentGroup().paintings();
      if (this.paintingList != null) {
        this.paintingList.setPaintings(this.paintings);
      }

      if (this.prevButton != null && this.nextButton != null) {
        this.prevButton.active = hasMultiplePaintings();
        this.nextButton.active = hasMultiplePaintings();
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

    this.hasHiddenPaintings = this.paintings.size() < this.state.getCurrentGroup().paintings().size();
    if (this.hasHiddenPaintings) {
      this.paintings.add(PaintingData.EMPTY);
    }

    if (this.paintingList != null) {
      this.paintingList.setPaintings(this.paintings);
    }

    if (this.prevButton != null && this.nextButton != null) {
      this.prevButton.active = hasMultiplePaintings();
      this.nextButton.active = hasMultiplePaintings();
    }
  }

  private boolean hasMultiplePaintings() {
    return this.paintings.size() > (this.hasHiddenPaintings ? 2 : 1);
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
      int nextIndex = this.paintings.size() + currentIndex - 1;
      if (this.hasHiddenPaintings && nextIndex == this.paintings.size() - 1) {
        nextIndex--;
      }
      return this.paintings.get(nextIndex % this.paintings.size());
    });
  }

  private void nextPainting() {
    if (!hasMultiplePaintings()) {
      return;
    }

    this.state.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      int nextIndex = currentIndex + 1;
      if (this.hasHiddenPaintings && nextIndex == this.paintings.size() - 1) {
        nextIndex++;
      }
      return this.paintings.get(nextIndex % this.paintings.size());
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
    applyFilters();
  }
}
