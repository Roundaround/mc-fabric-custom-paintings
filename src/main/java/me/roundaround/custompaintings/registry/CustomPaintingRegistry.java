package me.roundaround.custompaintings.registry;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.resource.PaintingImage;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;

public abstract class CustomPaintingRegistry implements AutoCloseable {
  protected final LinkedHashMap<String, PaintingPack> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PaintingPack> packsList = new ArrayList<>();
  protected final HashMap<Identifier, PaintingData> paintings = new HashMap<>();
  protected final HashMap<Identifier, PaintingImage> images = new HashMap<>();
  protected final HashMap<Identifier, String> imageHashes = new HashMap<>();

  protected String combinedImageHash = "";

  @Override
  public void close() {
    this.packsMap.clear();
    this.packsList.clear();
    this.paintings.clear();
    this.images.clear();
    this.imageHashes.clear();
    this.combinedImageHash = "";
  }

  public void setPacks(HashMap<String, PaintingPack> packsMap) {
    this.packsMap.clear();
    this.packsList.clear();
    this.paintings.clear();

    this.packsMap.putAll(packsMap);
    this.packsMap.forEach((id, pack) -> {
      this.packsList.add(pack);
      pack.paintings().forEach((painting) -> {
        this.paintings.put(painting.id(), painting);
      });
    });

    this.onPacksChanged();
  }

  protected void onPacksChanged() {
  }

  public void setImages(HashMap<Identifier, PaintingImage> images) {
    this.images.clear();
    this.imageHashes.clear();

    this.images.putAll(images);

    try {
      HashResult hashResult = hashImages(this.images);
      this.combinedImageHash = hashResult.combinedImageHash;
      this.imageHashes.putAll(hashResult.imageHashes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.onImagesChanged();
  }

  protected void onImagesChanged() {
  }

  protected static HashResult hashImages(HashMap<Identifier, PaintingImage> images) throws IOException {
    HashMap<Identifier, String> imageHashes = new HashMap<>();

    TreeSet<Identifier> imageIds = new TreeSet<>(images.keySet());
    LinkedHashMap<Identifier, ByteSource> byteSources = new LinkedHashMap<>();
    for (Identifier id : imageIds) {
      byteSources.putIfAbsent(id, images.get(id).getByteSource());
    }

    for (var entry : byteSources.entrySet()) {
      imageHashes.put(entry.getKey(), entry.getValue().hash(Hashing.sha256()).toString());
    }

    ByteSource combinedByteSource = ByteSource.concat(byteSources.values());
    String combinedImageHash = combinedByteSource.hash(Hashing.sha256()).toString();

    return new HashResult(combinedImageHash, imageHashes);
  }

  protected record HashResult(String combinedImageHash, HashMap<Identifier, String> imageHashes) {
  }
}
