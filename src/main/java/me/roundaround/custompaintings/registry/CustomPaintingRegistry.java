package me.roundaround.custompaintings.registry;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.resource.Image;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.*;

public abstract class CustomPaintingRegistry {
  protected final LinkedHashMap<String, PackData> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PackData> packsList = new ArrayList<>();
  protected final HashMap<Identifier, PaintingData> paintings = new HashMap<>();
  protected final HashMap<Identifier, MigrationData> migrations = new HashMap<>();
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

  public MigrationData getMigration(Identifier id) {
    if (id == null) {
      return null;
    }
    return this.migrations.get(id);
  }

  public Map<String, PackData> getPacks() {
    return Map.copyOf(this.packsMap);
  }

  public Map<Identifier, MigrationData> getMigrations() {
    return Map.copyOf(this.migrations);
  }

  public void clear() {
    this.packsMap.clear();
    this.packsList.clear();
    this.paintings.clear();
    this.migrations.clear();
    this.images.clear();
    this.imageHashes.clear();
    this.combinedImageHash = "";
  }

  public void setPacks(HashMap<String, PackData> packsMap) {
    this.packsMap.clear();
    this.packsList.clear();
    this.paintings.clear();
    this.migrations.clear();

    this.packsMap.putAll(packsMap);
    this.packsMap.forEach((id, pack) -> {
      this.packsList.add(pack);
      if (pack.disabled()) {
        return;
      }
      pack.paintings().forEach((painting) -> {
        this.paintings.put(painting.id(), painting);
      });
      pack.migrations().forEach((migration) -> {
        this.migrations.put(migration.id(), migration);
      });
    });

    this.onPacksChanged();
  }

  public void setImages(HashMap<Identifier, Image> images) {
    this.images.clear();
    this.imageHashes.clear();
    this.combinedImageHash = "";

    this.images.putAll(images);
    try {
      HashResult hashResult = hashImages(this.images);
      this.imageHashes.putAll(hashResult.imageHashes);
      this.combinedImageHash = hashResult.combinedImageHash;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.onImagesChanged();
  }

  protected void onPacksChanged() {
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
