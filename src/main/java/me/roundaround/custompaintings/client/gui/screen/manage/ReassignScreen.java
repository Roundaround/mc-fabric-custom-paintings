package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.KnownPaintingListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class ReassignScreen extends Screen implements PaintingPacksTracker {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int SEARCH_WIDTH = 200;
  private static final int SEARCH_HEIGHT = 20;
  private static final int PADDING = 8;

  private final Screen parent;
  private final UnknownPainting target;
  private final boolean applyToAll;

  private TextFieldWidget searchBox;
  private KnownPaintingListWidget list;
  private ButtonWidget confirmButton;
  private Identifier selectedId = null;

  public ReassignScreen(Screen parent, UnknownPainting target, boolean applyToAll) {
    super(Text.translatable("custompaintings.reassign.title"));
    this.parent = parent;
    this.target = target;
    this.applyToAll = applyToAll;
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

    if (applyToAll) {
      ClientNetworking.sendReassignAllPacket(this.target.currentData().id(), this.selectedId);
    } else {
      ClientNetworking.sendReassignPacket(this.target.uuid(), this.selectedId);
    }

    close();
  }

  private void setFilter(String filter) {
    this.list.setFilter(filter);
  }

  @Override
  public void onKnownPaintingsChanged(HashMap<Identifier, PaintingData> knownPaintings) {
    if (this.list != null) {
      this.list.setPaintings(knownPaintings.values());
    }
  }

  @Override
  public void init() {
    this.searchBox = new TextFieldWidget(this.textRenderer,
        (this.width - SEARCH_WIDTH) / 2,
        20,
        SEARCH_WIDTH,
        SEARCH_HEIGHT,
        this.searchBox,
        Text.translatable("custompaintings.reassign.search"));
    this.searchBox.setChangedListener(this::setFilter);
    addSelectableChild(this.searchBox);

    this.list = new KnownPaintingListWidget(this,
        this.client,
        this.width,
        this.height,
        48,
        this.height - 32);
    this.list.setPaintings(getKnownPaintings().values());
    addSelectableChild(this.list);

    this.confirmButton =
        ButtonWidget.builder(Text.translatable("custompaintings.reassign.confirm"), (button) -> {
              confirmSelection();
            })
            .position((this.width - PADDING) / 2 - BUTTON_WIDTH,
                this.height - BUTTON_HEIGHT - PADDING)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
    this.confirmButton.active = false;
    addDrawableChild(this.confirmButton);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          this.close();
        })
        .position((this.width + PADDING) / 2, this.height - BUTTON_HEIGHT - PADDING)
        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());

    setInitialFocus(this.searchBox);
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (super.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }
    return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    return this.searchBox.charTyped(chr, modifiers);
  }

  @Override
  public void tick() {
    this.searchBox.tick();
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.list.render(matrixStack, mouseX, mouseY, partialTicks);
    this.searchBox.render(matrixStack, mouseX, mouseY, partialTicks);

    drawCenteredTextWithShadow(matrixStack,
        this.textRenderer,
        this.title,
        this.width / 2,
        PADDING,
        0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
