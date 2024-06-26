package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.GroupListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Objects;

public class GroupSelectScreen extends PaintingEditScreen {
  protected final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  public GroupSelectScreen(PaintingEditState state) {
    super(Text.translatable("custompaintings.group.title"), state);
  }

  @Override
  public void init() {
    // If we have filters set, then go back to the group select screen, we should clear the filters.
    this.state.getFilters().reset();

    this.layout.addHeader(this.title, this.textRenderer);

    GroupListWidget groupsListWidget = new GroupListWidget(this.client, this.layout, this::selectGroup);
    groupsListWidget.setGroups(this.state.getGroups());
    this.layout.addBody(groupsListWidget);

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.CANCEL, this::close).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    this.saveEmpty();
    super.close();
  }

  protected void close(ButtonWidget button) {
    this.close();
  }

  protected void selectGroup(String id) {
    this.state.setCurrentGroup(id);
    Objects.requireNonNull(this.client)
        .getSoundManager()
        .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
    Objects.requireNonNull(this.client).setScreen(new PaintingSelectScreen(this.state));
  }
}
