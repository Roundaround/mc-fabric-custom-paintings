package me.roundaround.custompaintings.client.gui.screen.page;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen.Group;
import me.roundaround.custompaintings.client.gui.widget.ButtonWithDisabledTooltipWidget;
import me.roundaround.custompaintings.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaintingSelectPage extends PaintingEditScreenPage {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private ArrayList<PaintingData> paintings = new ArrayList<>();

  public PaintingSelectPage(
      PaintingEditScreen parent,
      MinecraftClient client,
      int width,
      int height) {
    super(parent, client, width, height);
  }

  public void setScrollAmount(double scrollAmount) {
    this.scrollAmount = scrollAmount;
  }

  public double getScrollAmount() {
    return this.scrollAmount;
  }

  public void populateWithAllPaintings() {
    this.parent.getCurrentGroup().paintings().forEach((paintingData) -> {
      this.paintings.add(paintingData);
    });
  }

  @Override
  public void init() {
    updateFilters();

    PaintingData paintingData = this.parent.getCurrentPainting();
    boolean canStay = this.parent.canStay(paintingData);
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
    this.searchBox.setText(this.parent.getFilters().getSearch());
    this.searchBox.setChangedListener((search) -> {
      onSearchBoxChanged(search);
    });

    IconButtonWidget filterButton = new IconButtonWidget(
        this.parent,
        10 + this.searchBox.getWidth() + 4,
        this.searchBox.y,
        0,
        Text.translatable("custompaintings.painting.filter"),
        (button) -> {
          this.parent.openFiltersPage();
        });

    int listTop = this.searchBox.y + this.searchBox.getHeight() + 4;
    int listBottom = this.height - footerHeight - 4;
    int listHeight = listBottom - listTop;
    this.paintingList = new PaintingListWidget(
        this,
        this.parent,
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
        this.parent,
        this.textRenderer,
        this.rightPaneX + (this.paneWidth - scaledWidth) / 2,
        (paintingTop + paintingBottom - scaledHeight) / 2,
        scaledWidth,
        scaledHeight,
        (button) -> {
          this.parent.saveSelection(paintingData);
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
          if (this.parent.hasMultipleGroups()) {
            this.parent.clearGroup();
          } else {
            this.parent.saveEmpty();
          }
        });

    ButtonWidget doneButton = new ButtonWithDisabledTooltipWidget(
        this.parent,
        this.textRenderer,
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          this.parent.saveCurrentSelection();
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
  public boolean preKeyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT:
        if (hasMultiplePaintings()) {
          playClickSound();
          previousPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_RIGHT:
        if (hasMultiplePaintings()) {
          playClickSound();
          nextPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_ESCAPE:
        playClickSound();
        this.parent.clearGroup();
        return true;
    }

    return false;
  }

  @Override
  public boolean postKeyPressed(int keyCode, int scanCode, int modifiers) {
    return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int keyCode) {
    return this.searchBox.charTyped(chr, keyCode);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, amount);
    }
    return false;
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

    Group currentGroup = this.parent.getCurrentGroup();

    MutableText title = Text.translatable("custompaintings.painting.title");
    if (this.parent.hasMultipleGroups()) {
      title = Text.literal(this.parent.getCurrentGroup().name() + " - ").append(title);
    }

    drawCenteredText(
        matrixStack,
        textRenderer,
        title,
        width / 2,
        11,
        0xFFFFFFFF);

    PaintingData paintingData = this.parent.getCurrentPainting();
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

  private int getHeaderHeight() {
    return 11 + textRenderer.fontHeight + 10;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT;
  }

  private void onSearchBoxChanged(String text) {
    if (this.parent.getFilters().getSearch().equals(text)) {
      return;
    }

    this.parent.getFilters().setSearch(text);
    updateFilters();
  }

  public void updateFilters() {
    if (!this.parent.getFilters().hasFilters()) {
      this.paintings = this.parent.getCurrentGroup().paintings();
      if (this.paintingList != null) {
        this.paintingList.setPaintings(this.paintings);
      }
      return;
    }

    // Manually iterate to guarantee order
    this.paintings = new ArrayList<>();
    this.parent.getCurrentGroup().paintings().forEach((paintingData) -> {
      if (this.parent.getFilters().test(paintingData)) {
        this.paintings.add(paintingData);
      }
    });

    if (this.paintingList != null) {
      this.paintingList.setPaintings(this.paintings);
    }
  }

  private boolean hasMultiplePaintings() {
    return this.paintings.size() > 1;
  }

  private void previousPainting() {
    if (!hasMultiplePaintings()) {
      return;
    }

    this.parent.setCurrentPainting((currentPainting) -> {
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

    this.parent.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      int nextIndex = (currentIndex + 1) % this.paintings.size();
      return this.paintings.get(nextIndex);
    });
  }
}
