package me.roundaround.custompaintings.config;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.roundalib.config.ConfigPath;
import me.roundaround.custompaintings.roundalib.config.manage.ModConfigImpl;
import me.roundaround.custompaintings.roundalib.config.manage.store.GameScopedFileStore;
import me.roundaround.custompaintings.roundalib.config.option.BooleanConfigOption;
import me.roundaround.custompaintings.roundalib.config.option.IntConfigOption;

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
  public IntConfigOption cacheTtl;
  public BooleanConfigOption silenceAllConvertPrompts;
  public BooleanConfigOption renderArtworkOnItems;

  private CustomPaintingsConfig() {
    super(CustomPaintingsMod.MOD_ID, "game");
  }

  @Override
  protected void registerOptions() {
    this.overrideRenderDistance = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of(
        "overrideRenderDistance")).setComment("Override vanilla render distance").setDefaultValue(false).build())
        .clientOnly()
        .commit();

    this.renderDistanceScale = this.buildRegistration(IntConfigOption.sliderBuilder(ConfigPath.of(
        "renderDistanceScale"))
        .setComment("Render distance scale")
        .setDefaultValue(16)
        .setMinValue(1)
        .setMaxValue(64)
        .setStep(4)
        .build()).clientOnly().commit();

    this.cacheImages = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of("cacheImages"))
        .setComment("Cache images from the server locally")
        .setDefaultValue(true)
        .build()).clientOnly().commit();

    this.cacheTtl = this.buildRegistration(IntConfigOption.builder(ConfigPath.of("cacheTtl"))
        .setComment("Number of days to retain cached images")
        .setDefaultValue(14)
        .setMinValue(1)
        .setMaxValue(10000)
        .onUpdate((option) -> option.setDisabled(!this.cacheImages.getPendingValue()))
        .build()).clientOnly().commit();

    this.silenceAllConvertPrompts = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of(
        "silenceAllConvertPrompts"))
        .setComment("Silence all legacy pack conversion prompts")
        .setDefaultValue(false)
        .build()).clientOnly().commit();

    this.renderArtworkOnItems = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of(
        "renderArtworkOnItems"))
        .setComment("Render the artwork on custom painting items")
        .setDefaultValue(true)
        .build()).clientOnly().commit();
  }
}
