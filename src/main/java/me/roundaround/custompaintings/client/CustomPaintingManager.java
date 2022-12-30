package me.roundaround.custompaintings.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

@Environment(value = EnvType.CLIENT)
public class CustomPaintingManager
    extends SpriteAtlasHolder
    implements IdentifiableResourceReloadListener {
  private static final MinecraftClient MINECRAFT = MinecraftClient.getInstance();
  private static final Pattern PATTERN = Pattern.compile("(?:\\w*/)*?(\\w+)\\.png");
  private static final Identifier PAINTING_BACK_ID = new Identifier(Identifier.DEFAULT_NAMESPACE, "back");

  private final HashMap<String, Pack> packs = new HashMap<>();
  private final HashMap<Identifier, Pair<Integer, Integer>> dimensions = new HashMap<>();
  private final HashSet<Identifier> spriteIds = new HashSet<>();

  public CustomPaintingManager(TextureManager manager) {
    super(manager, new Identifier("textures/atlas/custompaintings.png"), "painting");
  }

  @Override
  protected SpriteAtlasTexture.Data prepare(ResourceManager resourceManager, Profiler profiler) {
    packs.clear();
    dimensions.clear();
    spriteIds.clear();

    resourceManager.streamResourcePacks().forEach((resource) -> {
      try (InputStream stream = resource.openRoot("custompaintings.json")) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
          Pack pack = readPack(reader, resource.getName());

          if (packs.containsKey(pack.id())) {
            throw new ParseException("Multiple packs detected with id '" + pack.id() + "'! Pack id must be unique.");
          }
          packs.put(pack.id(), pack);

          pack.paintings().forEach((painting) -> {
            dimensions.put(new Identifier(pack.id(), painting.id()),
                new Pair<>(painting.width().orElse(1), painting.height().orElse(1)));
          });
        }
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.error("Error reading custom painting pack, skipping...", e);
      }

      spriteIds.add(PAINTING_BACK_ID);

      resource.getNamespaces(ResourceType.CLIENT_RESOURCES)
          .stream()
          .filter((namespace) -> {
            return !Identifier.DEFAULT_NAMESPACE.equals(namespace)
                && !Identifier.REALMS_NAMESPACE.equals(namespace);
          })
          .flatMap((namespace) -> {
            return resource.findResources(
                ResourceType.CLIENT_RESOURCES,
                namespace,
                "textures/painting",
                (id) -> id.getPath().endsWith(".png"))
                .stream();
          })
          .map((id) -> {
            String namespace = id.getNamespace();
            String path = id.getPath();

            Matcher matcher = PATTERN.matcher(path);
            if (matcher.find()) {
              path = matcher.group(1);
            }

            return new Identifier(namespace, path);
          })
          .peek((id) -> {
            if (!dimensions.containsKey(id)) {
              dimensions.put(id, new Pair<>(1, 1));
            }
          })
          .forEach(spriteIds::add);
    });

    if (MINECRAFT.player != null) {
      sendKnownPaintingsToServer();
    }

    return super.prepare(resourceManager, profiler);
  }

  @Override
  public Identifier getFabricId() {
    return new Identifier(CustomPaintingsMod.MOD_ID, "custom_paintings");
  }

  @Override
  protected Stream<Identifier> getSprites() {
    return spriteIds.stream();
  }

  public void sendKnownPaintingsToServer() {
    try {
      List<PaintingData> entries = getEntries();
      List<Identifier> ids = entries.stream().map(PaintingData::id).collect(Collectors.toList());
      ClientNetworking.sendDeclareKnownPaintingsPacket(ids);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.error("Error sending known paintings to server", e);
    }
  }

  public Optional<Pack> getPack(String id) {
    return Optional.ofNullable(packs.get(id));
  }

  public Identifier getAtlasId() {
    return getBackSprite().getAtlas().getId();
  }

  public boolean exists(Identifier id) {
    return dimensions.containsKey(id);
  }

  public List<PaintingData> getEntries() {
    return dimensions.entrySet()
        .stream()
        .map((entry) -> new PaintingData(entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight()))
        .collect(Collectors.toList());
  }

  public List<Pack> getPacks() {
    return new ArrayList<>(packs.values());
  }

  public Optional<Sprite> getPaintingSprite(PaintingData paintingData) {
    if (paintingData.isVanilla()) {
      Sprite vanillaSprite = MINECRAFT.getPaintingManager()
          .getPaintingSprite(Registry.PAINTING_VARIANT.get(paintingData.id()));
      return Optional.of(vanillaSprite);
    }
    return getPaintingSprite(paintingData.id());
  }

  public Optional<Sprite> getPaintingSprite(Identifier id) {
    if (!spriteIds.contains(id)) {
      return Optional.empty();
    }
    return Optional.ofNullable(getSprite(id));
  }

  public Pair<Integer, Integer> getPaintingDimensions(Identifier id) {
    return dimensions.getOrDefault(id, new Pair<>(1, 1));
  }

  public Sprite getBackSprite() {
    return this.getSprite(PAINTING_BACK_ID);
  }

  private Pack readPack(JsonReader reader, String filename) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Root of \"custompaintings.json\" must be an object");
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
        default:
          // Unknown key, skip it
          reader.skipValue();
      }
    }

    reader.endObject();

    if (id.isEmpty()) {
      throw new ParseException("Pack ID is required.");
    }

    return new Pack(id.get(), name.orElse(filename), List.copyOf(paintings));
  }

  private Painting readPainting(JsonReader reader) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Each painting must be an object.");
    }

    // Required
    Optional<String> paintingId = Optional.empty();

    // Optional
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
        default:
          // Unknown key, skip it
          reader.skipValue();
      }
    }

    reader.endObject();

    if (paintingId.isEmpty()) {
      throw new ParseException("Painting ID is required.");
    }

    return new Painting(paintingId.get(), height, width);
  }

  public class ParseException extends Exception {
    private ParseException(String message) {
      super(message);
    }
  }

  public record Pack(String id, String name, List<Painting> paintings) {
  }

  public record Painting(String id, Optional<Integer> height, Optional<Integer> width) {
  }
}
