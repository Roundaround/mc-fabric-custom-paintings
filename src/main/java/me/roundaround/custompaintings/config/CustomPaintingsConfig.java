package me.roundaround.custompaintings.config;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.roundalib.config.ModConfig;
import me.roundaround.roundalib.config.option.BooleanConfigOption;
import me.roundaround.roundalib.config.option.IntConfigOption;

public class CustomPaintingsConfig extends ModConfig {
  // Client only
  public final BooleanConfigOption overrideRenderDistance = null;
  public final IntConfigOption renderDistanceScale = null;
  public final BooleanConfigOption cacheImages = null;

  // Server only
  public final BooleanConfigOption enableHiddenVanillaPaintings = null;
  public final BooleanConfigOption throttleImageDownloads = null;
  public final IntConfigOption imagePacketsPerSecond = null;

  public CustomPaintingsConfig() {
    super(CustomPaintingsMod.MOD_ID);
  }
}
