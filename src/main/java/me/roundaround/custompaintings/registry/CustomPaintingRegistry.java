package me.roundaround.custompaintings.registry;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;

import java.util.*;

public abstract class CustomPaintingRegistry {
  protected final LinkedHashMap<String, PackData> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PackData> packsList = new ArrayList<>();
  protected final HashMap<CustomId, PaintingData> paintings = new HashMap<>();
  protected final HashMap<CustomId, MigrationData> migrations = new HashMap<>();
  protected final ImageStore images = new ImageStore();

  protected String combinedImageHash = "";

  protected abstract DynamicRegistryManager getRegistryManager();

  public boolean contains(CustomId id) {
    PaintingData data = this.get(id);
    return data != null && !data.isEmpty();
  }

  public PaintingData get(CustomId id) {
    if (id == null) {
      return PaintingData.EMPTY;
    }
    if (id.pack().equals(Identifier.DEFAULT_NAMESPACE)) {
      PaintingVariant variant = this.getVanillaVariant(id);
      if (variant == null) {
        return PaintingData.EMPTY;
      }
      return new PaintingData(variant);
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

  public List<PackData> getAllPacks() {
    return List.copyOf(this.packsList);
  }

  public List<PackData> getActivePacks() {
    return this.packsList.stream().filter((pack) -> !pack.disabled()).toList();
  }

  public List<PackData> getInactivePacks() {
    return this.packsList.stream().filter(PackData::disabled).toList();
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
  }

  public List<PaintingData> getAllVanilla() {
    DynamicRegistryManager registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return List.of();
    }

    return registryManager.get(RegistryKeys.PAINTING_VARIANT)
        .streamEntries()
        .filter((entry) -> entry.isIn(PaintingVariantTags.PLACEABLE))
        .map(RegistryEntry.Reference::value)
        .map(PaintingData::new)
        .toList();
  }

  public List<PaintingData> getAllVanillaUnplaceable() {
    DynamicRegistryManager registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return List.of();
    }

    return registryManager.get(RegistryKeys.PAINTING_VARIANT)
        .streamEntries()
        .filter((entry) -> !entry.isIn(PaintingVariantTags.PLACEABLE))
        .map(RegistryEntry.Reference::value)
        .map(PaintingData::new)
        .toList();
  }

  protected PaintingVariant getVanillaVariant(CustomId id) {
    DynamicRegistryManager registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return null;
    }
    return registryManager.get(RegistryKeys.PAINTING_VARIANT).get(id.toIdentifier());
  }
}
