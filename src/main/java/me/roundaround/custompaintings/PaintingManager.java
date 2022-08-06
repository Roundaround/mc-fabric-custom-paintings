package me.roundaround.custompaintings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Lifecycle;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.impl.lib.gson.JsonReader;
import net.fabricmc.loader.impl.lib.gson.JsonToken;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

public class PaintingManager {
  private static PaintingManager INSTANCE;

  private final HashMap<String, Pack> packs = new HashMap<>();
  private RegistryKey<Registry<PaintingVariant>> CUSTOM_PAINTING_VARIANT_KEY = RegistryKey.ofRegistry(new Identifier(CustomPaintingsMod.MOD_ID, "painting_variant"));
  private Registry<PaintingVariant> CUSTOM_PAINTING_VARIANT = new SimpleRegistry<>(CUSTOM_PAINTING_VARIANT_KEY, Lifecycle.experimental(), null);

  private PaintingManager() {
    ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
        .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
          @Override
          public Identifier getFabricId() {
            return new Identifier(CustomPaintingsMod.MOD_ID, "painting_packs");
          }

          @Override
          public void reload(ResourceManager manager) {
            manager.streamResourcePacks().forEach((resource) -> {
              CustomPaintingsMod.LOGGER.info(resource.getName());
              try (InputStream stream = resource.openRoot("custompaintings.json")) {
                try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                  Pack pack = readPack(reader);
                  packs.put(pack.id(), pack);
                }
              } catch (Exception e) {
                CustomPaintingsMod.LOGGER.info("Unable to access custompaintings.json");
              }
            });

            CustomPaintingsMod.LOGGER.info("Parsed all custom painting packs!");

            for (Pack pack : getAllPacks()) {
              for (Painting painting : pack.paintings()) {
                Registry.register(
                    CUSTOM_PAINTING_VARIANT,
                    new Identifier(pack.id(), painting.id()),
                    new PaintingVariant(16 * painting.width().orElse(1), 16 * painting.height().orElse(1)));
              }
            }
          }
        });
  }

  public static PaintingManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PaintingManager();
    }
    return INSTANCE;
  }

  public static void init() {
    if (INSTANCE == null) {
      INSTANCE = new PaintingManager();
    }
  }

  public static Optional<Pack> getPack(String id) {
    return getInstance().packs.containsKey(id)
        ? Optional.of(getInstance().packs.get(id))
        : Optional.empty();
  }

  public static List<Pack> getAllPacks() {
    return List.copyOf(getInstance().packs.values());
  }

  public static List<Painting> getPaintings(Pack pack) {
    return getPaintings(pack.id());
  }

  public static List<Painting> getPaintings(String id) {
    if (!getInstance().packs.containsKey(id)) {
      return List.of();
    }
    return List.copyOf(getInstance().packs.get(id).paintings());
  }

  public static Iterable<RegistryEntry<PaintingVariant>> getRegistryEntries(TagKey<PaintingVariant> tag) {
    return getInstance().CUSTOM_PAINTING_VARIANT.iterateEntries(tag);
  }

  private Pack readPack(JsonReader reader) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Root of \"custonpaintings.json\" must be an object");
    }

    reader.beginObject();

    // Required
    Optional<String> id = Optional.empty();

    // Optional
    Optional<String> name = Optional.empty();
    ArrayList<Painting> paintings = new ArrayList<>();

    while (reader.hasNext()) {
      final String key = reader.nextName();
      switch (key) {
        case "id":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Pack id must be a string.");
          }

          // TODO: Additional validation
          id = Optional.of(reader.nextString());
          break;
        case "name":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Pack name must be a string.");
          }

          // TODO: Additional validation
          name = Optional.of(reader.nextString());
          break;
        case "paintings":
          if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new ParseException("Paintings must be an array.");
          }

          reader.beginArray();

          while (reader.hasNext()) {
            paintings.add(readPainting(reader));
          }

          reader.endArray();
          break;
      }
    }

    reader.endObject();

    if (id.isEmpty()) {
      throw new ParseException("Pack ID is required.");
    }

    return new Pack(id.get(), name, List.copyOf(paintings));
  }

  private Painting readPainting(JsonReader reader) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Each painting must be an object.");
    }

    // Required
    Optional<String> paintingId = Optional.empty();

    // Optional
    Optional<String> paintingName = Optional.empty();
    Optional<Integer> height = Optional.empty();
    Optional<Integer> width = Optional.empty();

    reader.beginObject();

    while (reader.hasNext()) {
      final String key = reader.nextName();
      switch (key) {
        case "id":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Painting id must be a string.");
          }

          // TODO: Additional validation
          paintingId = Optional.of(reader.nextString());
          break;
        case "name":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Painting name must be a string.");
          }

          // TODO: Additional validation
          paintingName = Optional.of(reader.nextString());
          break;
        case "height":
          if (reader.peek() != JsonToken.NUMBER) {
            throw new ParseException("Paintings must be an array.");
          }

          // TODO: Additional validation
          height = Optional.of(reader.nextInt());
          break;
        case "width":
          if (reader.peek() != JsonToken.NUMBER) {
            throw new ParseException("Paintings must be an array.");
          }

          // TODO: Additional validation
          width = Optional.of(reader.nextInt());
          break;
      }
    }

    reader.endObject();

    if (paintingId.isEmpty()) {
      throw new ParseException("Painting ID is required.");
    }

    return new Painting(paintingId.get(), paintingName, height, width);
  }

  public class ParseException extends Exception {
    private ParseException(String message) {
      super(message);
    }
  }

  public record Pack(String id, Optional<String> name, List<Painting> paintings) {
  }

  public record Painting(String id, Optional<String> name, Optional<Integer> height, Optional<Integer> width) {
  }
}
