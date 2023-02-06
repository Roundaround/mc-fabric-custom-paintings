package me.roundaround.custompaintings.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.manage.PaintingPacksTracker;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.Migration;
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
import net.minecraft.resource.ZipResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
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
  private final HashMap<Identifier, PaintingData> data = new HashMap<>();
  private final HashSet<Identifier> spriteIds = new HashSet<>();

  public CustomPaintingManager(TextureManager manager) {
    super(manager, new Identifier("textures/atlas/custompaintings.png"), "painting");
  }

  @Override
  protected SpriteAtlasTexture.Data prepare(ResourceManager resourceManager, Profiler profiler) {
    packs.clear();
    data.clear();
    spriteIds.clear();

    resourceManager.streamResourcePacks().filter((resource) -> {
      // Can we find a way to include non-zip packs without throwing everytime
      // on the fabric pack?
      return resource instanceof ZipResourcePack;
    }).filter((resource) -> {
      return resource.getNamespaces(ResourceType.CLIENT_RESOURCES)
          .stream()
          .filter((namespace) -> {
            return !Identifier.DEFAULT_NAMESPACE.equals(namespace)
                && !Identifier.REALMS_NAMESPACE.equals(namespace);
          })
          .count() > 0;
    }).forEach((resource) -> {
      try (InputStream stream = resource.openRoot("custompaintings.json")) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
          Pack pack = readPack(reader, resource.getName());

          if (packs.containsKey(pack.id())) {
            throw new ParseException("Multiple packs detected with id '" + pack.id() + "'! Pack id must be unique.");
          }
          packs.put(pack.id(), pack);

          pack.paintings().forEach((painting) -> {
            Identifier id = new Identifier(pack.id(), painting.id());
            data.put(id, new PaintingData(
                id,
                painting.index(),
                painting.width().orElse(1),
                painting.height().orElse(1),
                painting.name().orElse(""),
                painting.artist().orElse("")));
          });
        }
      } catch (Exception e) {
        CustomPaintingsMod.LOGGER.error("Error reading custom painting pack, skipping...", e);
        return;
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
          .filter((id) -> data.containsKey(id))
          .forEach(spriteIds::add);
    });

    if (MINECRAFT.player != null) {
      sendKnownPaintingsToServer();
    }

    if (MINECRAFT.currentScreen instanceof PaintingPacksTracker) {
      ((PaintingPacksTracker) MINECRAFT.currentScreen).onResourcesReloaded();
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
      ClientNetworking.sendDeclareKnownPaintingsPacket(getEntries());
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
    return data.containsKey(id);
  }

  public List<PaintingData> getEntries() {
    return data.entrySet()
        .stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  public List<Pack> getPacks() {
    return new ArrayList<>(packs.values());
  }

  public List<Migration> getMigrations() {
    return packs.values()
        .stream()
        .flatMap((pack) -> pack.migrations().stream())
        .collect(Collectors.toList());
  }

  public Sprite getPaintingSprite(PaintingData paintingData) {
    if (paintingData == null || paintingData.isEmpty()) {
      return this.getBackSprite();
    }
    if (paintingData.isVanilla()) {
      return MINECRAFT.getPaintingManager()
          .getPaintingSprite(Registry.PAINTING_VARIANT.get(paintingData.id()));
    }
    return this.getSprite(paintingData.id());
  }

  public Pair<Integer, Integer> getPaintingDimensions(Identifier id) {
    PaintingData paintingData = data.get(id);
    if (paintingData == null) {
      return new Pair<>(1, 1);
    }
    return new Pair<>(paintingData.width(), paintingData.height());
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
    List<Migration> migrations = new ArrayList<>();

    while (reader.hasNext()) {
      final String key = reader.nextName();
      switch (key) {
        case "id":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Pack id must be a string.");
          }

          id = Optional.of(reader.nextString());
          break;
        case "name":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Pack name must be a string.");
          }

          name = Optional.of(reader.nextString());
          break;
        case "paintings":
          if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new ParseException("Paintings must be an array.");
          }

          reader.beginArray();

          while (reader.hasNext()) {
            paintings.add(readPainting(reader, paintings.size()));
          }

          reader.endArray();
          break;
        case "migrations":
          if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new ParseException("Migrations must be an array.");
          }

          reader.beginArray();

          while (reader.hasNext()) {
            migrations.add(readMigration(reader, migrations.size()));
          }

          reader.endArray();
          break;
        default:
          // Unknown key, skip it
          reader.skipValue();
      }
    }

    reader.endObject();

    // Pack ID is required
    if (id.isEmpty()) {
      throw new ParseException("Pack ID is required.");
    }

    // Pack ID must be a valid Identifier namespace
    try {
      new Identifier(id.get(), "dummy");
    } catch (InvalidIdentifierException e) {
      throw new ParseException("Non [a-z0-9_.-] character in pack ID (" + id.get() + ")");
    }

    String packId = id.get();
    migrations = migrations.stream()
        .map((migration) -> new Migration(
            migration.id(),
            migration.description(),
            packId,
            migration.index(),
            migration.pairs()))
        .collect(Collectors.toList());

    return new Pack(
        packId,
        name.orElse(filename),
        paintings,
        migrations);
  }

  private Painting readPainting(JsonReader reader, int index) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Each painting must be an object.");
    }

    // Required
    Optional<String> paintingId = Optional.empty();

    // Optional
    Optional<String> name = Optional.empty();
    Optional<String> artist = Optional.empty();
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

          paintingId = Optional.of(reader.nextString());
          break;
        case "name":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Painting name must be a string.");
          }

          name = Optional.of(reader.nextString());
          break;
        case "artist":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Painting artist must be a string.");
          }

          artist = Optional.of(reader.nextString());
          break;
        case "height":
          if (reader.peek() != JsonToken.NUMBER) {
            throw new ParseException("Painting height must be a number.");
          }

          height = Optional.of(reader.nextInt());
          break;
        case "width":
          if (reader.peek() != JsonToken.NUMBER) {
            throw new ParseException("Paintings width must be a number.");
          }

          width = Optional.of(reader.nextInt());
          break;
        default:
          // Unknown key, skip it
          reader.skipValue();
      }
    }

    reader.endObject();

    // Painting ID is required
    if (paintingId.isEmpty()) {
      throw new ParseException("Painting ID is required.");
    }

    // Painting ID must be a valid Identifier path
    try {
      new Identifier("dummy", paintingId.get());
    } catch (InvalidIdentifierException e) {
      throw new ParseException("Non [a-z0-9/._-] character in painting ID (" + paintingId.get() + ").");
    }

    // Height must be between 1 and 32
    if (height.get() < 1 || height.get() > 32) {
      throw new ParseException("Painting height must be between 1 and 32. (" + paintingId.get() + ").");
    }

    // Width must be between 1 and 32
    if (width.get() < 1 || width.get() > 32) {
      throw new ParseException("Painting width must be between 1 and 32. (" + paintingId.get() + ").");
    }

    return new Painting(paintingId.get(), index, name, artist, height, width);
  }

  private Migration readMigration(JsonReader reader, int index) throws IOException, ParseException {
    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
      throw new ParseException("Each migration must be an object.");
    }

    // Required
    Optional<String> id = Optional.empty();

    // Optional
    Optional<String> description = Optional.empty();
    ArrayList<Pair<String, String>> pairs = new ArrayList<>();

    reader.beginObject();

    while (reader.hasNext()) {
      final String key = reader.nextName();
      switch (key) {
        case "id":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Migration id must be a string.");
          }

          id = Optional.of(reader.nextString());
          break;
        case "description":
          if (reader.peek() != JsonToken.STRING) {
            throw new ParseException("Migration description must be a string.");
          }

          description = Optional.of(reader.nextString());
          break;
        case "pairs":
          if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new ParseException("Migration pairs must be an array.");
          }

          reader.beginArray();

          while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_ARRAY) {
              throw new ParseException("Each migration pair must be an array.");
            }

            reader.beginArray();

            if (reader.peek() != JsonToken.STRING) {
              throw new ParseException("Each migration pair must be an array of two strings.");
            }

            final String from = reader.nextString();

            if (reader.peek() != JsonToken.STRING) {
              throw new ParseException("Each migration pair must be an array of two strings.");
            }

            final String to = reader.nextString();

            reader.endArray();

            pairs.add(new Pair<>(from, to));
          }

          reader.endArray();
          break;
        default:
          // Unknown key, skip it
          reader.skipValue();
      }
    }

    reader.endObject();

    // Migration ID is required
    if (id.isEmpty()) {
      throw new ParseException("Migration ID is required.");
    }

    // Pack ID to be filled later
    return new Migration(id.get(), description.orElse(""), null, index, List.copyOf(pairs));
  }

  public class ParseException extends Exception {
    private ParseException(String message) {
      super(message);
    }
  }

  public record Pack(
      String id,
      String name,
      List<Painting> paintings,
      List<Migration> migrations) {
  }

  public record Painting(
      String id,
      int index,
      Optional<String> name,
      Optional<String> artist,
      Optional<Integer> height,
      Optional<Integer> width) {
  }
}
