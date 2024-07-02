package me.roundaround.custompaintings.registry;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.resource.PaintingImage;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public abstract class CustomPaintingRegistry {
  protected final HashMap<String, PaintingPack> packs = new HashMap<>();
  protected final HashMap<Identifier, PaintingImage> images = new HashMap<>();

  protected void onPacksChanged() {
  }

  protected void onImagesChanged() {
  }

  public void setPacks(HashMap<String, PaintingPack> packs) {
    this.packs.clear();
    this.packs.putAll(packs);

    this.onPacksChanged();
  }

  public void setImages(HashMap<Identifier, PaintingImage> images) {
    this.images.clear();
    this.images.putAll(images);

    this.onImagesChanged();
  }
}
