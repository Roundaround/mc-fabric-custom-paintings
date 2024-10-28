package me.roundaround.custompaintings.registry;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.resource.Image;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;

public abstract class CustomPaintingRegistry implements AutoCloseable {
  protected final LinkedHashMap<String, PackData> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PackData> packsList = new ArrayList<>();
  protected final HashMap<Identifier, PaintingData> paintings = new HashMap<>();
  protected final HashMap<Identifier, Image> images = new HashMap<>();
  protected final HashMap<Identifier, String> imageHashes = new HashMap<>();

  protected String combinedImageHash = "";

  public boolean contains(Identifier id) {
    PaintingData data = this.get(id);
    return data != null && !data.isEmpty();
  }

  public PaintingData get(Identifier id) {
    if (id == null) {
      return PaintingData.EMPTY;
    }
    if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
      return VanillaPaintingRegistry.getInstance().get(id);
    }
    return this.paintings.get(id);
  }

  public Image getImage(Identifier id) {
    return this.images.get(id);
  }

  @Override
  public void close() {
    this.packsMap.clear();
    this.packsList.clear();
    this.paintings.clear();
    this.images.clear();
    this.imageHashes.clear();
    this.combinedImageHash = "";
  }

  public void setPacks(HashMap<String, PackData> packsMap) {
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

  public void setImages(HashMap<Identifier, Image> images) {
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

  protected static HashResult hashImages(HashMap<Identifier, Image> images) throws IOException {
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
