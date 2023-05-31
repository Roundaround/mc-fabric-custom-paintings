package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.MismatchedPaintingListWidget;
import me.roundaround.custompaintings.util.MismatchedPainting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashSet;

public class MismatchedPaintingsScreen extends Screen {
  private static final int BUTTON_WIDTH = 204;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final ManagePaintingsScreen parent;

  private MismatchedPaintingListWidget list;

  public MismatchedPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.mismatched.title"));
    this.parent = parent;
  }

  public void setMismatchedPaintings(HashSet<MismatchedPainting> mismatchedPaintings) {
    if (this.list != null) {
      this.list.receiveData(mismatchedPaintings);
    }
  }

  @Override
  public void init() {
    this.list = new MismatchedPaintingListWidget(this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
    addSelectableChild(this.list);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          this.close();
        })
        .position((this.width - BUTTON_WIDTH) / 2, this.height - BUTTON_HEIGHT - PADDING)
        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    this.list.render(drawContext, mouseX, mouseY, partialTicks);

    drawContext.drawCenteredTextWithShadow(this.textRenderer,
        this.title,
        this.width / 2,
        8,
        0xFFFFFF);

    super.render(drawContext, mouseX, mouseY, partialTicks);
  }
}
