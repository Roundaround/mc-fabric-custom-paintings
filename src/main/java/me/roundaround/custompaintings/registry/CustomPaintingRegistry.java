package me.roundaround.custompaintings.registry;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.CustomId;
import me.roundaround.custompaintings.resource.HashResult;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.ResourceUtil;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CustomPaintingRegistry {
  protected final LinkedHashMap<String, PackData> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PackData> packsList = new ArrayList<>();
  protected final HashMap<CustomId, PaintingData> paintings = new HashMap<>();
  protected final HashMap<CustomId, MigrationData> migrations = new HashMap<>();
  protected final HashMap<CustomId, Image> images = new HashMap<>();
  protected final HashMap<CustomId, String> imageHashes = new HashMap<>();

  protected String combinedImageHash = "";

  public boolean contains(CustomId id) {
    PaintingData data = this.get(id);
    return data != null && !data.isEmpty();
  }

  public PaintingData get(CustomId id) {
    if (id == null) {
      return PaintingData.EMPTY;
    }
    if (id.pack().equals(Identifier.DEFAULT_NAMESPACE)) {
      return VanillaPaintingRegistry.getInstance().get(id);
    }
    return this.paintings.get(id);
  }

  public MigrationData getMigration(CustomId id) {
    if (id == null) {
      return null;
    }
    return this.migrations.get(id);
  }

  public Map<String, PackData> getPacks() {
    return Map.copyOf(this.packsMap);
  }

  public Map<CustomId, MigrationData> getMigrations() {
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

  public void setImages(HashMap<CustomId, Image> images) {
    this.images.clear();
    this.imageHashes.clear();
    this.combinedImageHash = "";

    this.images.putAll(images);
    try {
      HashResult hashResult = ResourceUtil.hashImages(this.images);
      this.imageHashes.putAll(hashResult.imageHashes());
      this.combinedImageHash = hashResult.combinedImageHash();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.onImagesChanged();
  }

  protected void onPacksChanged() {
  }

  protected void onImagesChanged() {
  }
}
