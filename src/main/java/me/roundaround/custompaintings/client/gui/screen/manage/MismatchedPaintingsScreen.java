package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.client.gui.widget.MismatchedPaintingListWidget;
import me.roundaround.custompaintings.util.MismatchedPainting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashSet;

public class MismatchedPaintingsScreen extends BaseScreen {
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
        this.height - this.getHeaderHeight() - this.getFooterHeight(),
        this.getHeaderHeight());
    addSelectableChild(this.list);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          this.close();
        })
        .position((this.width - ONE_COL_BUTTON_WIDTH) / 2,
            this.height - BUTTON_HEIGHT - HEADER_FOOTER_PADDING)
        .size(ONE_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    renderBasicListBackground(drawContext, mouseX, mouseY, partialTicks, this.list);
  }
}
