package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

public class ManagePaintingsScreen extends BaseScreen {
  private ButtonListWidget buttonList;

  public ManagePaintingsScreen() {
    super(Text.translatable("custompaintings.manage.title"));
  }

  @Override
  public void init() {
    this.buttonList = new ButtonListWidget(this.client,
        this.width,
        this.height - this.getHeaderHeight() - this.getFooterHeight(),
        this.getHeaderHeight());

    this.buttonList.addEntry(Text.translatable("custompaintings.manage.unknown"), (button) -> {
      Objects.requireNonNull(this.client).setScreen(new UnknownPaintingsScreen(this));
    });

    this.buttonList.addEntry(Text.translatable("custompaintings.manage.mismatched"), (button) -> {
      Objects.requireNonNull(this.client).setScreen(new MismatchedPaintingsScreen(this));
    });

    this.buttonList.addEntry(Text.translatable("custompaintings.manage.migrations"), (button) -> {
      Objects.requireNonNull(this.client).setScreen(new MigrationsScreen(this));
    });

    addSelectableChild(this.buttonList);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
          this.close();
        })
        .position((this.width - ONE_COL_BUTTON_WIDTH) / 2,
            this.height - BUTTON_HEIGHT - HEADER_FOOTER_PADDING)
        .size(ONE_COL_BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  @Override
  public void renderBackground(
      DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    renderBasicListBackground(drawContext, mouseX, mouseY, partialTicks, this.buttonList);
  }
}
