package me.roundaround.custompaintings.client.gui.screen.page;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen.Group;
import me.roundaround.custompaintings.client.gui.widget.FilterButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaintingSelectPage extends PaintingEditScreenPage {
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private int paneWidth;
  private int rightPaneX;

  public PaintingSelectPage(
      PaintingEditScreen parent,
      MinecraftClient client,
      int width,
      int height) {
    super(parent, client, width, height);
  }

  @Override
  public void init() {
    PaintingData paintingData = this.parent.getCurrentGroup()
        .paintings()
        .get(this.parent.getCurrentPainting());
    boolean canStay = this.parent.canStay(paintingData);

    this.paneWidth = this.width / 2 - 8;
    this.rightPaneX = this.width - this.paneWidth;

    // 10px padding on each side, 4px between search box and filter button
    this.searchBox = new TextFieldWidget(
        this.textRenderer,
        10,
        22,
        this.paneWidth - FilterButtonWidget.WIDTH - 24,
        BUTTON_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.painting.search"));
    this.searchBox.setChangedListener((search) -> setSearchQuery(search));

    FilterButtonWidget filterButton = new FilterButtonWidget(
        10 + this.searchBox.getWidth() + 4,
        22,
        this.parent);

    int headerHeight = getHeaderHeight();
    int footerHeight = getFooterHeight();

    this.paintingList = new PaintingListWidget(
        this.parent,
        this.client,
        this.paneWidth,
        this.height - 22 - FilterButtonWidget.HEIGHT - 4 - footerHeight,
        22 + FilterButtonWidget.HEIGHT + 4,
        this.height - getFooterHeight() - 4);

    int maxWidth = this.paneWidth / 2;
    int maxHeight = this.height - headerHeight - footerHeight - BUTTON_HEIGHT - 24;

    int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
    int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

    PaintingButtonWidget paintingButton = new PaintingButtonWidget(
        this.rightPaneX + (this.paneWidth - scaledWidth) / 2,
        (height + headerHeight - footerHeight - scaledHeight) / 2,
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
        height - 2 * BUTTON_HEIGHT - 10 - 4,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.previous"),
        (button) -> {
          previousPainting();
        });

    ButtonWidget nextButton = new ButtonWidget(
        this.rightPaneX + this.paneWidth / 2 + 2,
        height - 2 * BUTTON_HEIGHT - 10 - 4,
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
    Group currentGroup = this.parent.getCurrentGroup();
    int currentPainting = this.parent.getCurrentPainting();
    int posY = this.searchBox.y + this.searchBox.getHeight() + 10;
    PaintingData paintingData = currentGroup.paintings().get(currentPainting);

    this.searchBox.render(matrixStack, mouseX, mouseY, partialTicks);
    this.paintingList.render(matrixStack, mouseX, mouseY, partialTicks);

    if (this.parent.hasMultipleGroups()) {
      drawTextWithShadow(
          matrixStack,
          textRenderer,
          Text.literal(currentGroup.name()),
          10,
          posY,
          0xFFFFFFFF);
    }

    posY += textRenderer.fontHeight + 2;

    drawTextWithShadow(
        matrixStack,
        textRenderer,
        Text.translatable(
            "custompaintings.painting.number",
            currentPainting + 1,
            currentGroup.paintings().size()),
        10,
        posY,
        0xFFFFFFFF);

    posY += textRenderer.fontHeight + 2;

    drawTextWithShadow(
        matrixStack,
        textRenderer,
        Text.translatable(
            "custompaintings.painting.dimensions",
            paintingData.width(),
            paintingData.height()),
        10,
        posY,
        0xFFFFFFFF);

    if (paintingData.hasName() || paintingData.hasArtist()) {
      posY += textRenderer.fontHeight + 2;

      ArrayList<OrderedText> parts = new ArrayList<>();
      if (paintingData.hasName()) {
        parts.add(Text.literal("\"" + paintingData.name() + "\"").asOrderedText());
      }
      if (paintingData.hasName() && paintingData.hasArtist()) {
        parts.add(Text.of(" - ").asOrderedText());
      }
      if (paintingData.hasArtist()) {
        parts.add(OrderedText.styledForwardsVisitedString(
            paintingData.artist(),
            Style.EMPTY.withItalic(true)));
      }

      drawWithShadow(
          matrixStack,
          textRenderer,
          OrderedText.concat(parts),
          10,
          posY,
          0xFFFFFFFF);
    }

    posY += textRenderer.fontHeight + 2;

    drawTextWithShadow(
        matrixStack,
        textRenderer,
        Text.literal("(" + paintingData.id().toString() + ")")
            .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
        10,
        posY,
        0xFFFFFFFF);
  }

  private int getHeaderHeight() {
    return 10 + textRenderer.fontHeight + 2 + 10;
  }

  private int getFooterHeight() {
    return 10 + BUTTON_HEIGHT + 10;
  }

  private void resetFilters() {
    // TODO
  }

  private void setSearchQuery(String text) {
    // TODO
  }

  private boolean hasMultiplePaintings() {
    return this.parent.getCurrentGroup().paintings().size() > 1;
  }

  private void previousPainting() {
    Group currentGroup = this.parent.getCurrentGroup();
    this.parent.setCurrentPainting((currentPainting) -> {
      return (currentGroup.paintings().size() + currentPainting - 1)
          % currentGroup.paintings().size();
    });
  }

  private void nextPainting() {
    Group currentGroup = this.parent.getCurrentGroup();
    this.parent.setCurrentPainting((currentPainting) -> {
      return (currentPainting + 1) % currentGroup.paintings().size();
    });
  }
}
