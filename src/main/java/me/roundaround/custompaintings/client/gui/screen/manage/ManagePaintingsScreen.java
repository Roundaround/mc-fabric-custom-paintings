package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ManagePaintingsScreen extends Screen {
  private static final int BUTTON_WIDTH = 204;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private ButtonListWidget buttonList;

  public ManagePaintingsScreen() {
    super(Text.translatable("custompaintings.manage.title"));
  }

  @Override
  public void init() {
    this.buttonList = new ButtonListWidget(
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);

    this.buttonList.addEntry(
        Text.translatable("custompaintings.manage.unknown"),
        (button) -> {
          this.client.setScreen(new UnknownPaintingsScreen(this));
        });

    this.buttonList.addEntry(
        Text.translatable("custompaintings.manage.mismatched"),
        (button) -> {
          this.client.setScreen(new MismatchedPaintingsScreen(this));
        });

    this.buttonList.addEntry(
        Text.translatable("custompaintings.manage.migrations"),
        (button) -> {
          this.client.setScreen(new MigrationsScreen(this));
        });
    
    addSelectableChild(this.buttonList);

    addDrawableChild(new ButtonWidget(
        (this.width - BUTTON_WIDTH) / 2,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          this.close();
        }));
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackground(matrixStack);
    this.buttonList.render(matrixStack, mouseX, mouseY, partialTicks);
    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
