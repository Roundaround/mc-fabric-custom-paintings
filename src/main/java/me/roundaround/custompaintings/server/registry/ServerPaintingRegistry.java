package me.roundaround.custompaintings.server.registry;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.PaintingImage;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;

public class ServerPaintingRegistry extends CustomPaintingRegistry {
  private static ServerPaintingRegistry instance = null;

  private final HashMap<Identifier, String> checksums = new HashMap<>();

  private String combinedChecksum;

  private ServerPaintingRegistry() {
  }

  public static ServerPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ServerPaintingRegistry();
    }
    return instance;
  }

  @Override
  protected void onImagesChanged() {
    this.checksums.clear();

    try {
      TreeSet<Identifier> imageIds = new TreeSet<>(images.keySet());
      LinkedHashMap<Identifier, ByteSource> byteSources = new LinkedHashMap<>();
      for (Identifier id : imageIds) {
        byteSources.putIfAbsent(id, ByteSource.wrap(images.get(id).toBytes()));
      }

      for (var entry : byteSources.entrySet()) {
        this.checksums.put(entry.getKey(), entry.getValue().hash(Hashing.sha256()).toString());
      }

      ByteSource combinedByteSource = ByteSource.concat(byteSources.values());
      this.combinedChecksum = combinedByteSource.hash(Hashing.sha256()).toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void update(HashMap<String, PaintingPack> packs, HashMap<Identifier, PaintingImage> images) {
    this.setPacks(packs);
    this.setImages(images);

    // TODO: Notify client of changes.
  }
}
