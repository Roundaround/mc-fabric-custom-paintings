package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.MismatchedPaintingListWidget;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashSet;

public class MismatchedPaintingsScreen extends Screen {
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
    this.list = new MismatchedPaintingListWidget(this, this.client, this.width, this.height - 33 - 33, 33);
    addSelectableChild(this.list);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
      this.close();
    }).position((this.width - 200) / 2, this.height - 20 - GuiUtil.PADDING).size(200, 20).build());
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }
}
