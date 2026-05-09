package me.roundaround.custompaintings.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.resources.Identifier;

public abstract class CustomPaintingRegistry {
  protected final LinkedHashMap<String, PackData> packsMap = new LinkedHashMap<>();
  protected final ArrayList<PackData> packsList = new ArrayList<>();
  protected final HashMap<CustomId, PaintingData> paintings = new HashMap<>();
  protected final HashMap<CustomId, MigrationData> migrations = new HashMap<>();
  protected final HashMap<CustomId, Image> images = new HashMap<>();

  protected String combinedImageHash = CustomPaintingsMod.EMPTY_HASH;

  protected abstract RegistryAccess getRegistryManager();

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
    this.combinedImageHash = CustomPaintingsMod.EMPTY_HASH;
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
    RegistryAccess registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return List.of();
    }

    return registryManager.lookupOrThrow(Registries.PAINTING_VARIANT)
        .listElements()
        .filter((entry) -> entry.is(PaintingVariantTags.PLACEABLE))
        .map(Holder.Reference::value)
        .map(PaintingData::new)
        .toList();
  }

  public List<PaintingData> getAllVanillaUnplaceable() {
    RegistryAccess registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return List.of();
    }

    return registryManager.lookupOrThrow(Registries.PAINTING_VARIANT)
        .listElements()
        .filter((entry) -> !entry.is(PaintingVariantTags.PLACEABLE))
        .map(Holder.Reference::value)
        .map(PaintingData::new)
        .toList();
  }

  protected PaintingVariant getVanillaVariant(CustomId id) {
    RegistryAccess registryManager = this.getRegistryManager();
    if (registryManager == null) {
      return null;
    }
    return registryManager.lookupOrThrow(Registries.PAINTING_VARIANT).getValue(id.toIdentifier());
  }
}
