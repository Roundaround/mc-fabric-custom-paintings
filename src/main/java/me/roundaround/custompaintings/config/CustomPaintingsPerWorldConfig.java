package me.roundaround.custompaintings.config;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.roundalib.config.ConfigPath;
import me.roundaround.roundalib.config.manage.ModConfigImpl;
import me.roundaround.roundalib.config.manage.store.WorldScopedFileStore;
import me.roundaround.roundalib.config.option.BooleanConfigOption;
import me.roundaround.roundalib.config.option.IntConfigOption;

public class CustomPaintingsPerWorldConfig extends ModConfigImpl implements WorldScopedFileStore {
  private static CustomPaintingsPerWorldConfig instance = null;

  public static CustomPaintingsPerWorldConfig getInstance() {
    if (instance == null) {
      instance = new CustomPaintingsPerWorldConfig();
    }
    return instance;
  }

  public BooleanConfigOption throttleImageDownloads = null;
  public IntConfigOption imagePacketsPerSecond = null;

  private CustomPaintingsPerWorldConfig() {
    super(CustomPaintingsMod.MOD_ID, "world");
  }

  @Override
  protected void registerOptions() {
    this.throttleImageDownloads = this.buildRegistration(
            BooleanConfigOption.yesNoBuilder(ConfigPath.of("throttleImageDownloads")).setDefaultValue(false).build())
        .serverOnly()
        .commit();

    this.imagePacketsPerSecond = this.buildRegistration(
            IntConfigOption.builder(ConfigPath.of("imagePacketsPerSecond")).setDefaultValue(5).setMinValue(1).build())
        .serverOnly()
        .commit();
  }
}
