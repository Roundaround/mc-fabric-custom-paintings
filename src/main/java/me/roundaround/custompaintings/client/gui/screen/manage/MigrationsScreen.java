package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.client.gui.widget.MigrationListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.util.Migration;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashMap;

public class MigrationsScreen extends BaseScreen implements PaintingPacksTracker {
  private final Screen parent;

  private MigrationListWidget list;
  private ButtonWidget confirmButton;
  private Migration selectedMigration;

  public MigrationsScreen(Screen parent) {
    super(Text.translatable("custompaintings.migrations.title"));
    this.parent = parent;
  }

  public void setSelectedMigration(Migration migration) {
    this.selectedMigration = migration;
    if (this.confirmButton != null) {
      this.confirmButton.active = migration != null;
    }
  }

  public void confirmSelection() {
    if (this.selectedMigration == null) {
      return;
    }
    ClientNetworking.sendApplyMigrationPacket(selectedMigration);
    this.client.setScreen(null);
  }

  @Override
  public void onMigrationsChanged(HashMap<String, MigrationGroup> migrations) {
    this.list.setMigrations(migrations);
  }

  @Override
  public void init() {
    this.list = new MigrationListWidget(this,
        this.client,
        this.width,
        this.height - this.getHeaderHeight() - this.getFooterHeight(),
        this.getHeaderHeight());
    this.list.setMigrations(getMigrations());
    addSelectableChild(this.list);

    this.confirmButton =
        ButtonWidget.builder(Text.translatable("custompaintings.migrations.confirm"),
                (button) -> this.confirmSelection())
            .position((this.width - BUTTON_PADDING) / 2 - TWO_COL_BUTTON_WIDTH,
                this.height - BUTTON_HEIGHT - HEADER_FOOTER_PADDING)
            .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
    this.confirmButton.active = false;
    addDrawableChild(this.confirmButton);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          this.close();
        })
        .position((this.width + BUTTON_PADDING) / 2,
            this.height - BUTTON_HEIGHT - HEADER_FOOTER_PADDING)
        .size(TWO_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
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
