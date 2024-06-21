package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.MigrationListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Objects;

public class MigrationsScreen extends Screen implements PaintingPacksTracker {
  protected final Screen parent;
  protected final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private MigrationListWidget list;
  private ButtonWidget confirmButton;
  private Migration selectedMigration;

  public MigrationsScreen(Screen parent) {
    super(Text.translatable("custompaintings.migrations.title"));
    this.parent = parent;
  }

  @Override
  public void onMigrationsChanged(HashMap<String, MigrationGroup> migrations) {
    this.list.setMigrations(migrations);
  }

  @Override
  public void init() {
    this.layout.addHeader(this.title, this.textRenderer);

    this.list = new MigrationListWidget(
        this.client, this.layout, this::setSelectedMigration, this::setAndConfirmMigration);
    this.list.setMigrations(this.getMigrations());
    this.layout.addBody(this.list);

    DirectionalLayoutWidget footer = this.layout.addFooter(
        DirectionalLayoutWidget.horizontal().spacing(GuiUtil.PADDING));

    this.confirmButton = footer.add(
        ButtonWidget.builder(Text.translatable("custompaintings.migrations.confirm"), this::onConfirm).build());
    this.confirmButton.active = false;

    footer.add(ButtonWidget.builder(ScreenTexts.CANCEL, this::onCancel).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  private void setSelectedMigration(Migration migration) {
    this.selectedMigration = migration;
    if (this.confirmButton != null) {
      this.confirmButton.active = migration != null;
    }
  }

  private void setAndConfirmMigration(Migration migration) {
    this.setSelectedMigration(migration);
    this.confirmSelection();
  }

  private void confirmSelection() {
    if (this.selectedMigration == null) {
      return;
    }
    ClientNetworking.sendApplyMigrationPacket(this.selectedMigration);
    Objects.requireNonNull(this.client).setScreen(null);
  }

  private void onCancel(ButtonWidget button) {
    this.close();
  }

  private void onConfirm(ButtonWidget button) {
    this.confirmSelection();
  }
}
