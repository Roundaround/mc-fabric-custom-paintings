package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashMap;

import me.roundaround.custompaintings.client.gui.widget.UnknownPaintingListWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class UnknownPaintingsScreen extends Screen {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final ManagePaintingsScreen parent;

  private UnknownPaintingListWidget list;
  private ButtonWidget confirmButton;
  private Identifier selectedId = null;

  public UnknownPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.unknown.title"));
    this.parent = parent;
  }

  public void setUnknownPaintings(HashMap<Identifier, Integer> unknownPaintings) {
    if (this.list != null) {
      this.list.receiveData(unknownPaintings);
    }
  }

  public void setSelectedId(Identifier id) {
    this.selectedId = id;
    if (this.confirmButton != null) {
      this.confirmButton.active = id != null;
    }
  }

  public void confirmSelection() {
    if (this.selectedId == null) {
      return;
    }
    this.client.setScreen(new ReassignScreen(this, this.selectedId));
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

    this.confirmButton = new ButtonWidget(
        (this.width - PADDING) / 2 - BUTTON_WIDTH,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.reassign.confirm"),
        (button) -> {
          confirmSelection();
        });
    this.confirmButton.active = false;
    addDrawableChild(this.confirmButton);

    addDrawableChild(new ButtonWidget(
        (this.width + PADDING) / 2,
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
