package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashMap;

import me.roundaround.custompaintings.client.gui.widget.KnownPaintingListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ReassignScreen extends Screen implements KnownPaintingsTracker {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final Screen parent;
  private final Identifier currentId;

  private KnownPaintingListWidget list;
  private ButtonWidget confirmButton;
  private Identifier selectedId = null;

  public ReassignScreen(Screen parent, Identifier id) {
    super(Text.translatable("custompaintings.reassign.title"));
    this.parent = parent;
    this.currentId = id;
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
    ClientNetworking.sendReassignIdPacket(
        this.currentId,
        this.selectedId);
    this.client.setScreen(null);
  }

  @Override
  public void onKnownPaintingsChanged(HashMap<Identifier, PaintingData> knownPaintings) {
    if (this.list != null) {
      this.list.setPaintings(knownPaintings.values());
    }
  }

  @Override
  public void init() {
    this.list = new KnownPaintingListWidget(
        this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
    this.list.setPaintings(getKnownPaintings().values());
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

    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, PADDING, 0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
