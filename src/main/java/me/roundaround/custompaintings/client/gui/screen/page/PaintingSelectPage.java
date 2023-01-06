package me.roundaround.custompaintings.client.gui.screen.page;

import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen.Group;
import me.roundaround.custompaintings.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaintingSelectPage extends PaintingEditScreenPage {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private Function<PaintingData, Boolean> filter = (paintingData) -> true;
  private String searchQuery = "";

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

  public Function<PaintingData, Boolean> getFilter() {
    return this.filter;
  }

  @Override
  public void init() {
    PaintingData paintingData = this.parent.getCurrentPainting();
    boolean canStay = this.parent.canStay(paintingData);

    this.paneWidth = this.width / 2 - 8;
    this.rightPaneX = this.width - this.paneWidth;

    int headerHeight = getHeaderHeight();
    int footerHeight = getFooterHeight();

    // 10px padding on each side, 4px between search box and filter button
    this.searchBox = new TextFieldWidget(
        this.textRenderer,
        10,
        headerHeight + 4 + (this.parent.hasMultipleGroups() ? this.textRenderer.fontHeight + 2 : 0),
        this.paneWidth - IconButtonWidget.WIDTH - 24,
        BUTTON_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.painting.search"));
    this.searchBox.setChangedListener((search) -> setSearchQuery(search));

    IconButtonWidget filterButton = new IconButtonWidget(
        this.parent,
        10 + this.searchBox.getWidth() + 4,
        this.searchBox.y,
        0,
        Text.translatable("custompaintings.painting.filter"),
        (button) -> {
          // TODO: Show advanced filters
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
        listBottom);

    int paintingTop = headerHeight + 8
        + (paintingData.hasName() || paintingData.hasArtist() ? 3 : 2) * textRenderer.fontHeight
        + (paintingData.hasName() || paintingData.hasArtist() ? 2 : 1) * 2;
    int paintingBottom = this.height - footerHeight - 8 - BUTTON_HEIGHT;

    int maxWidth = this.paneWidth / 2 - 8;
    int maxHeight = paintingBottom - paintingTop;

    int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
    int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

    PaintingButtonWidget paintingButton = new PaintingButtonWidget(
        this.rightPaneX + (this.paneWidth - scaledWidth) / 2,
        (paintingTop + paintingBottom - scaledHeight) / 2,
        maxWidth,
        maxHeight,
        (button) -> {
          this.parent.saveSelection(paintingData);
        },
        paintingData);

    if (!canStay) {
      paintingButton.active = false;
    }

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

    ButtonWidget doneButton = new ButtonWidget(
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          this.parent.saveCurrentSelection();
        });

    if (!canStay) {
      doneButton.active = false;
    }

    addSelectableChild(this.searchBox);
    addSelectableChild(this.paintingList);
    addDrawableChild(filterButton);
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

    drawCenteredText(
        matrixStack,
        textRenderer,
        Text.translatable("custompaintings.painting.title"),
        width / 2,
        11,
        0xFFFFFFFF);

    Group currentGroup = this.parent.getCurrentGroup();

    if (this.parent.hasMultipleGroups()) {
      drawTextWithShadow(
          matrixStack,
          textRenderer,
          Text.literal(this.parent.getCurrentGroup().name()),
          10,
          getHeaderHeight() + 4,
          0xFFFFFFFF);
    }

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

  private void resetFilters() {
    // TODO
  }

  private void setSearchQuery(String text) {
    if (this.searchQuery.equals(text)) {
      return;
    }

    this.searchQuery = text;
    this.paintingList.setFilter((paintingData) -> {
      if (this.searchQuery.isEmpty()) {
        return true;
      }

      String name = paintingData.name().toLowerCase().replace(" ", "");
      String artist = paintingData.artist().toLowerCase();
      String query = this.searchQuery.toLowerCase();

      return name.contains(query) || artist.contains(query);
    });
  }

  private boolean hasMultiplePaintings() {
    return this.parent.getCurrentGroup().paintings().size() > 1;
  }

  private void previousPainting() {
    Group currentGroup = this.parent.getCurrentGroup();
    this.parent.setCurrentPainting((currentPainting) -> {
      int currentIndex = currentGroup.paintings().indexOf(currentPainting);
      int nextIndex = (currentGroup.paintings().size() + currentIndex - 1)
          % currentGroup.paintings().size();
      return currentGroup.paintings().get(nextIndex);
    });
  }

  private void nextPainting() {
    Group currentGroup = this.parent.getCurrentGroup();
    this.parent.setCurrentPainting((currentPainting) -> {
      int currentIndex = currentGroup.paintings().indexOf(currentPainting);
      int nextIndex = (currentIndex + 1) % currentGroup.paintings().size();
      return currentGroup.paintings().get(nextIndex);
    });
  }
}
