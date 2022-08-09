package me.roundaround.custompaintings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    CustomPaintingManager.init();

    // TODO: Resoruce reload listener, prepare all paintings together into a
    // sprite atlas, Mixin into PaintingManager to call containsId on the
    // painting registry. If not found, check our own dynamic/custom registry,
    // load from our custom atlas. If we don't have it either, fall back to
    // vanilla default.
  }
}
