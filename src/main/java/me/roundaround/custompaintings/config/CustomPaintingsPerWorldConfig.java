package me.roundaround.custompaintings.config;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.roundalib.config.ConfigPath;
import me.roundaround.custompaintings.roundalib.config.manage.ModConfigImpl;
import me.roundaround.custompaintings.roundalib.config.manage.store.WorldScopedFileStore;
import me.roundaround.custompaintings.roundalib.config.option.BooleanConfigOption;
import me.roundaround.custompaintings.roundalib.config.option.IntConfigOption;

public class CustomPaintingsPerWorldConfig extends ModConfigImpl implements WorldScopedFileStore {
  private static CustomPaintingsPerWorldConfig instance = null;

  public static CustomPaintingsPerWorldConfig getInstance() {
    if (instance == null) {
      instance = new CustomPaintingsPerWorldConfig();
    }
    return instance;
  }

  public BooleanConfigOption throttleImageDownloads;
  public IntConfigOption maxImagePacketsPerSecond;
  public IntConfigOption maxPerClientImagePacketsPerSecond;
  public IntConfigOption maxImagePacketSize;
  public BooleanConfigOption silenceConvertPrompt;

  private CustomPaintingsPerWorldConfig() {
    super(CustomPaintingsMod.MOD_ID, "world");
  }

  @Override
  protected void registerOptions() {
    this.throttleImageDownloads = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of(
            "throttleImageDownloads")).setComment("Throttle image transfers to clients").setDefaultValue(true).build())
        .serverOnly()
        .commit();

    this.maxImagePacketsPerSecond = this.buildRegistration(IntConfigOption.builder(ConfigPath.of(
            "maxImagePacketsPerSecond"))
        .setComment("Maximum number of image packets per second")
        .setDefaultValue(40)
        .setMinValue(0)
        .build()).serverOnly().commit();

    this.maxPerClientImagePacketsPerSecond = this.buildRegistration(IntConfigOption.builder(ConfigPath.of(
            "maxPerClientImagePacketsPerSecond"))
        .setComment("Maximum number of image packets per second per client")
        .setDefaultValue(10)
        .setMinValue(0)
        .build()).serverOnly().commit();

    this.maxImagePacketSize = this.buildRegistration(IntConfigOption.builder(ConfigPath.of("maxImagePacketSize"))
        .setComment("Maximum size for each image packet in KB")
        .setDefaultValue(256)
        .setStep(64)
        .setMinValue(0)
        .build()).serverOnly().commit();

    this.silenceConvertPrompt = this.buildRegistration(BooleanConfigOption.yesNoBuilder(ConfigPath.of(
            "silenceConvertPrompt"))
        .setComment("Silence legacy pack conversion prompts for this world")
        .setDefaultValue(false)
        .build()).singlePlayerOnly().commit();
  }
}
