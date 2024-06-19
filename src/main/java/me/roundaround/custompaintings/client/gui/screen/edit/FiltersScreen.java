package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.FilterListWidget;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.FullBodyWrapperWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

public class FiltersScreen extends PaintingEditScreen {
  private static final int FOOTER_BUTTON_WIDTH = 150;
  private static final int FOOTER_BUTTON_HEIGHT = 20;
  private static final int FOOTER_BUTTON_SPACING = GuiUtil.PADDING * 2;

  protected final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private FilterListWidget filtersListWidget;

  public FiltersScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.filter.title"), state);
  }

  @Override
  public void init() {
    this.layout.addHeader(this.title, this.textRenderer);

    this.filtersListWidget = new FilterListWidget(this.state, this.client, this.layout);
    this.layout.addBody(new FullBodyWrapperWidget(this.filtersListWidget, this.layout));

    DirectionalLayoutWidget row = DirectionalLayoutWidget.horizontal().spacing(FOOTER_BUTTON_SPACING);
    this.layout.addFooter(row);

    row.add(ButtonWidget.builder(Text.translatable("custompaintings.filter.reset"), this::resetFilters)
        .size(FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT)
        .build());
    row.add(
        ButtonWidget.builder(ScreenTexts.DONE, this::close).size(FOOTER_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(new PaintingSelectScreen(this.state));
  }

  protected void close(ButtonWidget button) {
    this.close();
  }

  protected void resetFilters(ButtonWidget button) {
    this.state.getFilters().reset();
    this.filtersListWidget.updateFilters();
  }
}
