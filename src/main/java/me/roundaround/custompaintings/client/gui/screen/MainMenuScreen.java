package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.resource.PaintingPackLoader;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  public MainMenuScreen(Screen parent) {
    // TODO: i18n
    super(Text.of("Custom Paintings"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.textRenderer, this.title);

    // TODO: i18n
    this.layout.addBody(ButtonWidget.builder(Text.of("Configuration"), this::navigateConfig).build());
    this.layout.addBody(ButtonWidget.builder(Text.of("Convert Legacy Packs"), this::navigateConvert).build());
    this.layout.addBody(ButtonWidget.builder(Text.of("Reload Packs"), this::reloadPacks).build());

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    if (this.client == null) {
      return;
    }
    this.client.setScreen(this.parent);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private void navigateConfig(ButtonWidget button) {
    assert this.client != null;
    this.client.setScreen(new ConfigScreen(this, CustomPaintingsMod.MOD_ID, CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()
    ));
  }

  private void navigateConvert(ButtonWidget button) {
    assert this.client != null;
    this.client.setScreen(new ConvertPromptScreen(this.client, this));
  }

  private void reloadPacks(ButtonWidget button) {
    assert this.client != null;
    // TODO: Send packet to server to reload packs
  }
}
