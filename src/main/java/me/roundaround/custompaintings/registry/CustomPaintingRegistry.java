package me.roundaround.custompaintings.registry;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;

public class CustomPaintingRegistry {
  private static CustomPaintingRegistry instance = null;

  private final LinkedHashMap<Identifier, PaintingData> store = new LinkedHashMap<>();

  private CustomPaintingRegistry() {}

  public static void init() {
    // No op, other than forcing instance creation.
    getInstance();
  }

  public static CustomPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new CustomPaintingRegistry();
    }
    return instance;
  }
}
