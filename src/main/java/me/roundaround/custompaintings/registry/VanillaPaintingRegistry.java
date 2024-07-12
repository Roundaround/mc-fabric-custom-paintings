package me.roundaround.custompaintings.registry;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

public class VanillaPaintingRegistry {
  private static VanillaPaintingRegistry instance = null;

  private final LinkedHashMap<Identifier, PaintingData> placeable = new LinkedHashMap<>();
  private final LinkedHashMap<Identifier, PaintingData> nonPlaceable = new LinkedHashMap<>();
  private final LinkedHashMap<Identifier, PaintingData> all = new LinkedHashMap<>();

  private VanillaPaintingRegistry() {
    Registries.PAINTING_VARIANT.stream().forEach((vanillaVariant) -> {
      Identifier id = Registries.PAINTING_VARIANT.getId(vanillaVariant);
      PaintingData paintingData = new PaintingData(vanillaVariant);

      RegistryKey<PaintingVariant> registryKey = RegistryKey.of(Registries.PAINTING_VARIANT.getKey(), id);
      boolean isPlaceable = Registries.PAINTING_VARIANT.getEntry(registryKey)
          .map((entry) -> entry.isIn(PaintingVariantTags.PLACEABLE))
          .orElse(false);

      var destination = isPlaceable ? this.placeable : this.nonPlaceable;
      destination.put(id, paintingData);
    });

    this.all.putAll(this.placeable);
    this.all.putAll(this.nonPlaceable);
  }

  public static void init() {
    // No op, other than forcing instance creation.
    getInstance();
  }

  public static VanillaPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new VanillaPaintingRegistry();
    }
    return instance;
  }

  public PaintingData get(Identifier identifier) {
    return this.all.get(identifier);
  }

  public PaintingData get(PaintingVariant variant) {
    return this.all.get(Registries.PAINTING_VARIANT.getId(variant));
  }

  public List<PaintingData> getAll() {
    return this.getAll(Placeable.ANY);
  }

  public List<PaintingData> getAll(Placeable placeable) {
    return List.copyOf(this.mapFor(placeable).values());
  }

  public void forEach(Consumer<PaintingData> consumer) {
    this.forEach(consumer, Placeable.ANY);
  }

  public void forEach(Consumer<PaintingData> consumer, Placeable placeable) {
    this.mapFor(placeable).forEach(((identifier, paintingData) -> consumer.accept(paintingData)));
  }

  public int size() {
    return this.size(Placeable.ANY);
  }

  public int size(Placeable placeable) {
    return this.mapFor(placeable).size();
  }

  private LinkedHashMap<Identifier, PaintingData> mapFor(Placeable placeable) {
    return switch (placeable) {
      case YES -> this.placeable;
      case NO -> this.nonPlaceable;
      case ANY -> this.all;
    };
  }

  public enum Placeable {
    YES, NO, ANY
  }
}
