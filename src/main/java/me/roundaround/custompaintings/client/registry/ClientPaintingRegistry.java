package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.custompaintings.resource.PaintingPack;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class ClientPaintingRegistry {
  private static ClientPaintingRegistry instance = null;

  private final HashMap<String, PaintingPack> packs = new HashMap<>();
  private final HashMap<Identifier, PaintingImage> images = new HashMap<>();
  private final HashMap<Identifier, PaintingData> paintings = new HashMap<>();

  private ClientPaintingRegistry() {
  }

  public static ClientPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ClientPaintingRegistry();
    }
    return instance;
  }

  public void setPacks(HashMap<String, PaintingPack> packs) {
    this.packs.clear();
    this.paintings.clear();

    this.packs.putAll(packs);
    this.packs.values().forEach((pack) -> {
      pack.paintings().forEach((painting) -> {
        Identifier id = new Identifier(pack.id(), painting.id());
        this.paintings.put(
            id, new PaintingData(id, painting.width(), painting.height(), painting.name(), painting.artist()));
      });
    });
  }

  public void setImages(HashMap<Identifier, PaintingImage> images) {
    this.images.clear();
    this.images.putAll(images);
  }
}
