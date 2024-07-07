package me.roundaround.custompaintings.config;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.roundalib.config.ConfigPath;
import me.roundaround.roundalib.config.manage.ModConfigImpl;
import me.roundaround.roundalib.config.manage.store.GameScopedFileStore;
import me.roundaround.roundalib.config.option.BooleanConfigOption;
import me.roundaround.roundalib.config.option.IntConfigOption;

public class CustomPaintingsConfig extends ModConfigImpl implements GameScopedFileStore {
  private static CustomPaintingsConfig instance = null;

  public static CustomPaintingsConfig getInstance() {
    if (instance == null) {
      instance = new CustomPaintingsConfig();
    }
    return instance;
  }

  public BooleanConfigOption overrideRenderDistance;
  public IntConfigOption renderDistanceScale;
  public BooleanConfigOption cacheImages;

  private CustomPaintingsConfig() {
    super(CustomPaintingsMod.MOD_ID, "game");
  }

  @Override
  protected void registerOptions() {
    this.overrideRenderDistance = this.buildRegistration(
            BooleanConfigOption.yesNoBuilder(ConfigPath.of("overrideRenderDistance")).setDefaultValue(false).build())
        .clientOnly()
        .commit();

    this.renderDistanceScale = this.buildRegistration(
        IntConfigOption.sliderBuilder(ConfigPath.of("renderDistanceScale"))
            .setDefaultValue(16)
            .setMinValue(1)
            .setMaxValue(64)
            .setStep(4)
            .build()).clientOnly().commit();

    this.cacheImages = this.buildRegistration(
            BooleanConfigOption.yesNoBuilder(ConfigPath.of("cacheImages")).setDefaultValue(true).build())
        .clientOnly()
        .commit();
  }
}