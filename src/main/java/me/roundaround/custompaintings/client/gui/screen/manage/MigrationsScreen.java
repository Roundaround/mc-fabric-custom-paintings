package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashMap;

import me.roundaround.custompaintings.client.gui.widget.MigrationListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.util.Migration;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class MigrationsScreen extends Screen implements PaintingPacksTracker {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

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
    this.list = new MigrationListWidget(
        this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
    this.list.setMigrations(getMigrations());
    addSelectableChild(this.list);

    this.confirmButton = new ButtonWidget(
        (this.width - PADDING) / 2 - BUTTON_WIDTH,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.migrations.confirm"),
        (button) -> this.confirmSelection());
    this.confirmButton.active = false;
    addDrawableChild(this.confirmButton);

    addDrawableChild(new ButtonWidget(
        (this.width + PADDING) / 2,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.CANCEL,
        (button) -> {
          this.close();
        }));
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.list.render(matrixStack, mouseX, mouseY, partialTicks);

    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, PADDING, 0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
