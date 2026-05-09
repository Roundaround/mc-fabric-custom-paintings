package me.roundaround.custompaintings.client.gui.screen.set;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.PackListWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class PackSelectScreen extends BaseSetPaintingScreen {
  protected final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  public PackSelectScreen(PaintingEditState state) {
    super(Component.translatable("custompaintings.pack.title"), state);
  }

  @Override
  public void init() {
    // If we have filters set, then go back to the group select screen, we should clear the filters.
    this.state.getFilters().reset();

    this.layout.addHeader(this.font, this.title);

    PackListWidget groupsListWidget = this.layout.addBody(
        new PackListWidget(this.minecraft, this.layout, this::selectPack));
    groupsListWidget.setGroups(this.state.getPacks());

    this.layout.addFooter(Button.builder(CommonComponents.GUI_CANCEL, this::close).build());

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public void onClose() {
    this.saveEmpty();
    super.onClose();
  }

  protected void close(Button button) {
    this.onClose();
  }

  protected void selectPack(String id) {
    this.state.setCurrentPack(id);
    Objects.requireNonNull(this.minecraft)
        .getSoundManager()
        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    Objects.requireNonNull(this.minecraft).setScreen(new PaintingSelectScreen(this.state));
  }
}
