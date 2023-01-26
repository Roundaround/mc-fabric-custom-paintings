package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashSet;

import me.roundaround.custompaintings.client.gui.widget.UnknownPaintingListWidget;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class UnknownPaintingsScreen extends Screen {
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final ManagePaintingsScreen parent;

  private UnknownPaintingListWidget list;
  private ButtonWidget reassignButton;
  private ButtonWidget removeButton;
  private UnknownPainting selected = null;

  public UnknownPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.unknown.title"));
    this.parent = parent;
  }

  public void setUnknownPaintings(HashSet<UnknownPainting> unknownPaintings) {
    if (this.list != null) {
      this.list.receiveData(unknownPaintings);
    }
  }

  public void setSelected(UnknownPainting selected) {
    this.selected = selected;
    if (this.reassignButton != null) {
      this.reassignButton.active = selected != null;
    }
  }

  public void reassignSelection() {
    if (this.selected == null) {
      return;
    }
    this.client.setScreen(new ReassignScreen(this, this.selected));
  }

  public void removeSelection() {
    if (this.selected == null) {
      return;
    }

  }

  @Override
  public void init() {
    this.list = new UnknownPaintingListWidget(
        this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
    addSelectableChild(this.list);

    this.reassignButton = new ButtonWidget(
        (this.width - BUTTON_WIDTH) / 2 - BUTTON_WIDTH - PADDING,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.unknown.reassign"),
        (button) -> {
          reassignSelection();
        });
    this.reassignButton.active = false;
    addDrawableChild(this.reassignButton);

    this.removeButton = new ButtonWidget(
        (this.width - BUTTON_WIDTH) / 2,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.unknown.remove"),
        (button) -> {
          removeSelection();
        });
    this.removeButton.active = false;
    addDrawableChild(this.removeButton);

    addDrawableChild(new ButtonWidget(
        (this.width + BUTTON_WIDTH / 2 + PADDING),
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.CANCEL,
        (button) -> {
          this.close();
        }));
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.list.render(matrixStack, mouseX, mouseY, partialTicks);

    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
