package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.MismatchedPaintingListWidget;
import me.roundaround.custompaintings.util.MismatchedPainting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Objects;

public class MismatchedPaintingsScreen extends Screen {
  protected final ManagePaintingsScreen parent;
  protected final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private MismatchedPaintingListWidget list;
  private Runnable afterInit = null;

  public MismatchedPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.mismatched.title"));
    this.parent = parent;
  }

  public void setMismatchedPaintings(HashSet<MismatchedPainting> mismatchedPaintings) {
    if (this.list != null) {
      this.list.receiveData(mismatchedPaintings);
    } else {
      this.afterInit = () -> {
        this.list.receiveData(mismatchedPaintings);
      };
    }
  }

  @Override
  public void init() {
    this.layout.addHeader(this.title, this.textRenderer);

    this.list = new MismatchedPaintingListWidget(this, this.client, this.layout);
    this.layout.addBody(this.list);

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.CANCEL, this::close).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();

    if (this.afterInit != null) {
      this.afterInit.run();
    }
    this.afterInit = null;
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  protected void close(ButtonWidget button) {
    this.close();
  }
}
