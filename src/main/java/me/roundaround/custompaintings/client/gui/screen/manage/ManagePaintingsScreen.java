package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

public class ManagePaintingsScreen extends Screen {
  private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  public ManagePaintingsScreen() {
    super(Text.translatable("custompaintings.manage.title"));
  }

  @Override
  public void init() {
    this.layout.addHeader(this.title, this.textRenderer);

    ButtonListWidget buttonList = new ButtonListWidget(this.client, this.layout);
    this.layout.addBody(buttonList);

    buttonList.addEntry(
        Text.translatable("custompaintings.manage.unknown"),
        (button) -> Objects.requireNonNull(this.client).setScreen(new UnknownPaintingsScreen(this))
    );
    buttonList.addEntry(
        Text.translatable("custompaintings.manage.mismatched"),
        (button) -> Objects.requireNonNull(this.client).setScreen(new MismatchedPaintingsScreen(this))
    );
    buttonList.addEntry(
        Text.translatable("custompaintings.manage.migrations"),
        (button) -> Objects.requireNonNull(this.client).setScreen(new MigrationsScreen(this))
    );

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.close()).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }
}
