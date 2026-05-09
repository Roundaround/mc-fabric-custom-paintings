package me.roundaround.custompaintings.client.gui.screen.set;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.FilterListWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class FiltersScreen extends BaseSetPaintingScreen {
  protected final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  private FilterListWidget filtersListWidget;

  public FiltersScreen(PaintingEditState state) {
    super(Component.translatable("custompaintings.filter.title"), state);
  }

  @Override
  public void init() {
    this.layout.addHeader(this.font, this.title);

    this.filtersListWidget = this.layout.addBody(new FilterListWidget(
        this.state.getFilters(),
        this.minecraft,
        this.layout
    ));

    this.layout.addFooter(Button.builder(Component.translatable("custompaintings.filter.reset"), this::resetFilters)
        .size(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT)
        .build());
    this.layout.addFooter(Button.builder(CommonComponents.GUI_DONE, this::close)
        .size(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT)
        .build());

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();

    this.filtersListWidget.updateFilters();
    this.setInitialFocus(this.filtersListWidget.getFirstFocusable());
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public void onClose() {
    Objects.requireNonNull(this.minecraft).setScreen(new PaintingSelectScreen(this.state));
  }

  protected void close(Button button) {
    this.onClose();
  }

  protected void resetFilters(Button button) {
    this.state.getFilters().reset();
    this.filtersListWidget.updateFilters();
  }
}
