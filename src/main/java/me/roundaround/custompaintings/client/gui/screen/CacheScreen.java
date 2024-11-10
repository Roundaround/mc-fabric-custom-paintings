package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CacheScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.DEFAULT_WIDTH_SMALL;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  public CacheScreen(Screen parent) {
    // TODO: i18n
    super(Text.of("Custom Paintings Mod Cache"));
    this.parent = parent;

    CacheManager.getInstance().getStats().orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync((result, exception) -> {
      // TODO: Display stats!
    });
  }

  @Override
  protected void init() {
    assert this.client != null;

    // TODO: Loading indicator that gets replaced with stats when they finish loading

    // TODO: i18n
    this.layout.addFooter(
        ButtonWidget.builder(Text.of("Clear Cache"), (b) -> this.clearCache()).width(BUTTON_WIDTH).build());
    // TODO: i18n
    this.layout.addFooter(
        ButtonWidget.builder(Text.of("Configure"), (b) -> this.navigateConfig()).width(BUTTON_WIDTH).build());
    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (b) -> this.close()).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    assert this.client != null;
    this.client.setScreen(this.parent);
  }

  private void clearCache() {
    try {
      CacheManager.getInstance().clear();
    } catch (IOException e) {
      // TODO: Handle exception
      throw new RuntimeException(e);
    }
  }

  private void navigateConfig() {
    assert this.client != null;
    this.client.setScreen(new ConfigScreen(this, CustomPaintingsMod.MOD_ID, CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()
    ));
  }
}
