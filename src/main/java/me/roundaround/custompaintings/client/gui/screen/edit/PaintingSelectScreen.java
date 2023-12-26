package me.roundaround.custompaintings.client.gui.screen.edit;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.DrawUtils;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.PaintingEditState.PaintingChangeListener;
import me.roundaround.custompaintings.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingChangeListener {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private IconButtonWidget prevButton;
  private IconButtonWidget nextButton;
  private ButtonWidget doneButton;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private ArrayList<PaintingData> paintings = new ArrayList<>();
  private boolean hasHiddenPaintings = false;
  private PaintingData paintingData;
  private boolean canStay = true;
  private Sprite paintingSprite = null;
  private Rect2i paintingRect = new Rect2i(0, 0, 0, 0);

  public PaintingSelectScreen(PaintingEditState state) {
    super(generateTitle(state), state);
  }

  public void setScrollAmount(double scrollAmount) {
    this.scrollAmount = scrollAmount;
  }

  public double getScrollAmount() {
    return this.scrollAmount;
  }

  public void populateWithAllPaintings() {
    this.paintings.addAll(this.state.getCurrentGroup().paintings());
  }

  @Override
  public void onPaintingChange(PaintingData paintingData) {
    this.paintingData = paintingData;
    this.canStay = this.state.canStay(paintingData);
    this.paintingSprite =
        CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);

    int headerHeight = getHeaderHeight();
    int footerHeight = getFooterHeight();

    int paintingTop =
        headerHeight + 8 + (paintingData.hasLabel() ? 3 : 2) * textRenderer.fontHeight +
            (paintingData.hasLabel() ? 2 : 1) * 2;
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
      if (!this.canStay) {
        this.doneButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.painting.big",
            paintingData.width(),
            paintingData.height())));
      } else {
        this.doneButton.setTooltip(null);
      }
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
    this.searchBox = new TextFieldWidget(this.textRenderer,
        10,
        headerHeight + 4,
        this.paneWidth - IconButtonWidget.WIDTH - 24,
        BUTTON_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.painting.search"));
    this.searchBox.setText(this.state.getFilters().getSearch());
    this.searchBox.setChangedListener(this::onSearchBoxChanged);

    IconButtonWidget filterButton =
        IconButtonWidget.builder(Text.translatable("custompaintings.painting.filter"), (button) -> {
              Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
            }, IconButtonWidget.FILTER_ICON)
            .position(10 + this.searchBox.getWidth() + 4, this.searchBox.getY())
            .build();

    int listTop = this.searchBox.getY() + this.searchBox.getHeight() + 4;
    int listHeight = this.height - footerHeight - 4 - listTop;
    this.paintingList = new PaintingListWidget(this,
        this.state,
        this.client,
        this.paneWidth,
        listHeight,
        listTop,
        this.paintings);

    this.prevButton =
        IconButtonWidget.builder(Text.translatable("custompaintings.painting.previous"),
                (button) -> {
                  previousPainting();
                },
                IconButtonWidget.LEFT_ICON)
            .position(this.rightPaneX + 8, this.height - footerHeight - 4 - BUTTON_HEIGHT)
            .build();

    this.nextButton =
        IconButtonWidget.builder(Text.translatable("custompaintings.painting.next"), (button) -> {
              nextPainting();
            }, IconButtonWidget.RIGHT_ICON)
            .position(this.width - 8 - IconButtonWidget.WIDTH,
                this.height - footerHeight - 4 - BUTTON_HEIGHT)
            .build();

    this.prevButton.active = hasMultiplePaintings();
    this.nextButton.active = hasMultiplePaintings();

    ButtonWidget cancelButton =
        ButtonWidget.builder(this.state.hasMultipleGroups() ? ScreenTexts.BACK : ScreenTexts.CANCEL,
                (button) -> {
                  if (this.state.hasMultipleGroups()) {
                    this.client.setScreen(new GroupSelectScreen(this.state));
                  } else {
                    saveEmpty();
                  }
                })
            .position((width - BUTTON_PADDING) / 2 - TWO_COL_BUTTON_WIDTH,
                height - BUTTON_HEIGHT - 10)
            .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();

    this.doneButton = ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
          saveCurrentSelection();
        })
        .position((width + BUTTON_PADDING) / 2, height - BUTTON_HEIGHT - 10)
        .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();

    if (!this.canStay) {
      this.doneButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.painting.big",
          this.paintingData.width(),
          this.paintingData.height())));
    } else {
      this.doneButton.setTooltip(null);
    }

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
      case GLFW.GLFW_KEY_LEFT -> {
        if (!this.state.hasMultiplePaintings() || !hasControlDown()) {
          break;
        }
        playClickSound();
        previousPainting();
        return true;
      }
      case GLFW.GLFW_KEY_RIGHT -> {
        if (!this.state.hasMultiplePaintings() || !hasControlDown()) {
          break;
        }
        playClickSound();
        nextPainting();
        return true;
      }
      case GLFW.GLFW_KEY_ESCAPE -> {
        playClickSound();
        if (this.state.hasMultipleGroups()) {
          Objects.requireNonNull(this.client).setScreen(new GroupSelectScreen(this.state));
        } else {
          saveEmpty();
        }
        return true;
      }
      case GLFW.GLFW_KEY_ENTER -> {
        if (!this.paintingList.isFocused()) {
          break;
        }
        Optional<PaintingData> painting = this.paintingList.getSelectedPainting();
        if (painting.isPresent() && this.state.canStay(painting.get())) {
          playClickSound();
          saveSelection(painting.get());
          return true;
        }
      }
      case GLFW.GLFW_KEY_F -> {
        if (hasControlDown()) {
          if (hasShiftDown()) {
            Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
            return true;
          }

          Element focused = getFocused();
          if (focused != this.searchBox) {
            if (focused != null) {
              focused.setFocused(false);
            }

            this.setFocused(this.searchBox);
            return true;
          }
        }
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    DrawUtils.renderBackgroundInRegion(drawContext, 0, width, height);

    this.paintingList.render(drawContext, mouseX, mouseY, partialTicks);
  }

  @Override
  public void renderForeground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    super.renderForeground(drawContext, mouseX, mouseY, partialTicks);

    this.searchBox.render(drawContext, mouseX, mouseY, partialTicks);

    Group currentGroup = this.state.getCurrentGroup();
    PaintingData paintingData = this.state.getCurrentPainting();

    if (paintingData.isEmpty()) {
      int paneHeight = this.height - this.getHeaderHeight() - this.getFooterHeight();
      int posX = this.rightPaneX + paneWidth / 2;
      int posY = getHeaderHeight() + (paneHeight - this.textRenderer.fontHeight) / 2;

      drawContext.drawCenteredTextWithShadow(textRenderer,
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
        drawContext.drawCenteredTextWithShadow(textRenderer,
            paintingData.getLabel(),
            posX,
            posY,
            0xFFFFFFFF);

        posY += textRenderer.fontHeight + 2;
      }

      drawContext.drawCenteredTextWithShadow(textRenderer,
          Text.literal("(" + paintingData.id().toString() + ")")
              .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
          posX,
          posY,
          0xFFFFFFFF);

      posY += textRenderer.fontHeight + 2;

      drawContext.drawCenteredTextWithShadow(textRenderer,
          Text.translatable("custompaintings.painting.dimensions",
              paintingData.width(),
              paintingData.height()),
          posX,
          posY,
          0xFFFFFFFF);

      drawContext.drawCenteredTextWithShadow(textRenderer,
          Text.translatable("custompaintings.painting.number",
              currentPaintingIndex + 1,
              currentGroup.paintings().size()),
          posX,
          this.height - getFooterHeight() - 4 - BUTTON_HEIGHT +
              (BUTTON_HEIGHT - textRenderer.fontHeight) / 2,
          0xFFFFFFFF);

      renderPainting(drawContext, mouseX, mouseY);
    }
  }

  private void renderPainting(DrawContext drawContext, int mouseX, int mouseY) {
    int x = this.paintingRect.getX();
    int y = this.paintingRect.getY();
    int width = this.paintingRect.getWidth();
    int height = this.paintingRect.getHeight();
    float color = this.canStay ? 1f : 0.5f;

    drawContext.fill(x, y, x + width, y + height, 0xFF000000);

    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    RenderSystem.setShaderColor(color, color, color, 1f);
    RenderSystem.setShaderTexture(0, this.paintingSprite.getAtlasId());
    drawContext.drawSprite(x + 1, y + 1, 0, width - 2, height - 2, this.paintingSprite);
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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

    this.hasHiddenPaintings =
        this.paintings.size() < this.state.getCurrentGroup().paintings().size();
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

  @Override
  protected int getHeaderHeight() {
    return HEADER_FOOTER_PADDING + textRenderer.fontHeight + 1 + HEADER_FOOTER_PADDING;
  }

  @Override
  protected int getFooterHeight() {
    return HEADER_FOOTER_PADDING + BUTTON_HEIGHT;
  }

  private void onSearchBoxChanged(String text) {
    if (this.state.getFilters().getSearch().equals(text)) {
      return;
    }

    this.state.getFilters().setSearch(text);
    applyFilters();
  }

  private static Text generateTitle(PaintingEditState state) {
    MutableText title = Text.translatable("custompaintings.painting.title");
    if (state.hasMultipleGroups()) {
      title = Text.literal(state.getCurrentGroup().name() + " - ").append(title);
    }
    return title;
  }
}
