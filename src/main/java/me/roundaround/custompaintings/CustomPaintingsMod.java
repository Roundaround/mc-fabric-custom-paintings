package me.roundaround.custompaintings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    PaintingManager.init();
  }
}
