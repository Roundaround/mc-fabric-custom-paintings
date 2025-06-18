package me.roundaround.custompaintings.client.registry;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.texture.BasicTextureSprite;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.mixin.BakedModelManagerAccessor;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.util.PathAccessor;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.data.ItemModels;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedSimpleModel;
import net.minecraft.client.render.model.ErrorCollectingSpriteGetter;
import net.minecraft.client.render.model.MissingModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.model.ReferencedModelsCollector;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class ItemManager {
  private static final Image HOOK_IMAGE = generateItemHookImage();
  private static final Identifier VANILLA_SPRITE = Identifier.ofVanilla("textures/item/painting.png");

  private static ItemManager instance = null;

  // TODO: Create new sprite atlas dedicated to item sprites

  private final MinecraftClient client;

  private ItemManager(MinecraftClient client) {
    this.client = client;
  }

  public void generateSprites(
      Collection<PaintingData> paintings,
      Function<CustomId, Image> imageSupplier,
      Consumer<SpriteContents> addSprite) {
    if (!CustomPaintingsConfig.getInstance().renderArtworkOnItems.getPendingValue()) {
      return;
    }

    // TODO: paintings.forEach -> loadOrScheduleGeneration
    // TODO: loadResults.forEach -> if (loadedFromCache) -> set lastAccess
    // TODO: loadResults.forEach -> wait for all futures, then batch add sprites and rebuild sprite atlas/models

    if (!CustomPaintingsConfig.getInstance().cacheImages.getPendingValue()) {
      paintings.forEach((painting) -> {
        Image baseImage = imageSupplier.apply(painting.id());
        if (isEmpty(baseImage)) {
          addSprite.accept(this.getPlaceholderSprite(painting.id()));
          return;
        }
        Image image = this.generateImage(painting, baseImage);
        this.getSpriteContents(painting, image).ifPresent(addSprite);
      });
      return;
    }

    HashMap<CustomId, Image> images = new HashMap<>();
    paintings.forEach((painting) -> {
      Image image = imageSupplier.apply(painting.id());
      if (image != null) {
        images.put(painting.id(), image);
      }
    });

    CacheData data = this.loadCacheData();
    final long expired = Util.getEpochTimeMs() - getTtlMs();
    HashSet<String> loadedHashes = new HashSet<>();
    HashMap<String, Image> generatedImages = new HashMap<>();

    paintings.forEach((painting) -> {
      Image baseImage = images.get(painting.id());
      if (isEmpty(baseImage)) {
        addSprite.accept(this.getPlaceholderSprite(painting.id()));
        return;
      }

      String baseHash = baseImage.hash();
      Image image = null;
      CacheFile file = data.hashes().get(baseHash);

      if (file == null || file.isExpired(expired)) {
        image = this.generateImage(painting, baseImage);
        generatedImages.put(baseHash, image);
      } else {
        image = loadImage(getCacheDir(), file.hash());
        if (image.isEmpty()) {
          image = this.generateImage(painting, baseImage);
          generatedImages.put(baseHash, image);
        } else {
          loadedHashes.add(baseHash);
        }
      }

      this.getSpriteContents(painting, image).ifPresent(addSprite);
    });

    // TODO: Add some kind of mutex/lock
    CompletableFuture.runAsync(() -> {
      try {
        this.saveToFile(loadedHashes, generatedImages);
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Failed to save item image cache", e);
      }
    }, Util.getIoWorkerExecutor());
  }

  public CompletableFuture<Void> bakeModels(
      ErrorCollectingSpriteGetter spriteGetter,
      HashMap<CustomId, PaintingData> paintings,
      Executor executor) {
    HashMap<Identifier, UnbakedModel> unbakedModels = new HashMap<>();
    unbakedModels.put(Identifier.ofVanilla("item/generated"), getGeneratedItemModel());

    HashMap<Identifier, ItemAsset> itemAssets = new HashMap<>();
    paintings.values().forEach((painting) -> {
      Identifier id = getItemModelId(painting.id());

      JsonObject json = new JsonObject();
      json.addProperty("parent", "minecraft:item/generated");
      JsonObject textures = new JsonObject();
      textures.addProperty("layer0", id.toString());
      json.add("textures", textures);
      JsonUnbakedModel unbakedModel = JsonUnbakedModel.deserialize(new StringReader(json.toString()));

      unbakedModels.put(id, unbakedModel);
      itemAssets.put(id, new ItemAsset(ItemModels.basic(id), ItemAsset.Properties.DEFAULT));
    });

    ReferencedModelsCollector collector = new ReferencedModelsCollector(unbakedModels, MissingModel.create());
    collector.addSpecialModel(GeneratedItemModel.GENERATED, new GeneratedItemModel());
    itemAssets.forEach((id, asset) -> collector.resolve(asset.model()));
    BakedSimpleModel missingModel = collector.getMissingModel();
    Map<Identifier, BakedSimpleModel> simpleModels = collector.collectModels();

    ModelBaker baker = new ModelBaker(
        LoadedEntityModels.EMPTY,
        Map.of(),
        itemAssets,
        simpleModels,
        missingModel);

    return baker.bake(spriteGetter, executor).thenAccept((bakedModels) -> {
      BakedModelManagerAccessor accessor = (BakedModelManagerAccessor) this.client.getBakedModelManager();
      HashMap<Identifier, ItemModel> itemModels = new HashMap<>(accessor.getBakedItemModels());
      itemModels.putAll(bakedModels.itemStackModels());
      accessor.setBakedItemModels(itemModels);
    });
  }

  private LoadResult loadOrScheduleGeneration(PaintingData painting, Image baseImage) {
    if (!CustomPaintingsConfig.getInstance().cacheImages.getPendingValue()) {
      SpriteContents sprite = this.getPlaceholderSprite(painting.id());

      CompletableFuture<SpriteContents> generation = isEmpty(baseImage)
          ? null
          : CompletableFuture.supplyAsync(() -> {
            // TODO: Wire up image generation
            return sprite;
          }, Util.getIoWorkerExecutor());

      return new LoadResult(
          sprite,
          false,
          generation);
    }

    if (isEmpty(baseImage)) {
      return new LoadResult(
          this.getPlaceholderSprite(painting.id()),
          false,
          CompletableFuture.supplyAsync(() -> {
            // TODO: Wire up image generation
            // TODO: Save generated image back to cache
            return null;
          }, Util.getIoWorkerExecutor()));
    }

    CacheData data = this.loadCacheData();
    final long expired = Util.getEpochTimeMs() - getTtlMs();

    String baseHash = baseImage.hash();
    CacheFile file = data.hashes().get(baseHash);

    if (file == null || file.isExpired(expired)) {
      return new LoadResult(
          this.getPlaceholderSprite(painting.id()),
          false,
          CompletableFuture.supplyAsync(() -> {
            // TODO: Wire up image generation
            // TODO: Save generated image back to cache
            return null;
          }, Util.getIoWorkerExecutor()));
    }

    Image image = loadImage(getCacheDir(), file.hash());
    if (isEmpty(image)) {
      return new LoadResult(
          this.getPlaceholderSprite(painting.id()),
          false,
          CompletableFuture.supplyAsync(() -> {
            // TODO: Wire up image generation
            // TODO: Save generated image back to cache
            return null;
          }, Util.getIoWorkerExecutor()));
    }

    return new LoadResult(
        this.getSpriteContents(painting, image).orElse(null),
        true,
        null);
  }

  private Image generateImage(PaintingData painting, Image baseImage) {
    if (isEmpty(baseImage)) {
      return Image.empty();
    }

    int minWidth = 9;
    int maxWidth = 13;
    int minHeight = 8;
    int maxHeight = 12;
    int blockWidth = painting.width();
    int blockHeight = painting.height();

    float scale = Math.min(maxWidth / (float) blockWidth, maxHeight / (float) blockHeight);
    int width = Math.max(minWidth, MathHelper.floor(blockWidth * scale));
    int height = Math.max(minHeight, MathHelper.floor(blockHeight * scale));

    int tx = MathHelper.ceil((16 - width) / 2f);
    int ty = 2 + MathHelper.ceil((13 - height) / 2f);

    return baseImage.apply(
        Image.Operation.scale(width, height,
            Image.Resampler.combine(
                0.2f,
                Image.Resampler.BILINEAR,
                Image.Resampler.NEAREST_NEIGHBOR_FRAME_PRESERVING)),
        Image.Operation.resize(16, 16),
        Image.Operation.translate(tx, ty),
        Image.Operation.embed(HOOK_IMAGE, 6, ty - 3),
        new Image.Operation() {
          @Override
          public Text getName() {
            return Text.of("Shadow");
          }

          @Override
          public Image.Hashless apply(Image.Hashless source) {
            int minX = tx;
            int maxX = minX + width - 1;

            ArrayList<Image.Color> colors = new ArrayList<>();
            int sampleY = height - 1 + ty;
            for (int x = minX; x <= maxX; x++) {
              Image.Color color = source.getPixel(x, sampleY);
              if (color.getAlphaFloat() > 0.5f) {
                colors.add(color.removeAlpha());
              }
            }
            Image.Color shadow = Image.Color.average(colors).darken(0.15f);

            Image.Color[] pixels = source.copyPixels();
            for (int x = minX; x <= maxX; x++) {
              pixels[Image.getIndex(source.height(), x, sampleY + 1)] = shadow;
            }
            return new Image.Hashless(pixels, source.width(), source.height());
          }
        });
  }

  private SpriteContents getPlaceholderSprite(CustomId id) {
    return BasicTextureSprite.fetch(
        this.client,
        getItemModelId(id),
        VANILLA_SPRITE);
  }

  private Optional<SpriteContents> getSpriteContents(PaintingData painting, Image image) {
    return this.getSpriteContents(CustomId.from(getItemModelId(painting.id())), image);
  }

  private Optional<SpriteContents> getSpriteContents(CustomId id, Image image) {
    if (isEmpty(image)) {
      return Optional.empty();
    }
    NativeImage nativeImage = image.toNativeImage();
    return Optional.of(new SpriteContents(
        id.toIdentifier(),
        new SpriteDimensions(image.width(), image.height()),
        nativeImage,
        ResourceMetadata.NONE));
  }

  private CacheData loadCacheData() {
    Path dataFile = getDataFile(getCacheDir());

    if (Files.notExists(dataFile) || !Files.isRegularFile(dataFile)) {
      return CacheData.empty();
    }

    NbtCompound nbt;
    try {
      nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to load cache data");
      return CacheData.empty();
    }

    return CacheData.fromNbt(nbt);
  }

  private void saveToFile(Set<String> touched, Map<String, Image> generated) throws IOException {
    final long now = Util.getEpochTimeMs();

    Path cacheDir = getCacheDir();
    if (Files.notExists(cacheDir)) {
      Files.createDirectories(cacheDir);
    }

    Path dataFile = getDataFile(cacheDir);
    NbtCompound nbt;
    if (Files.notExists(dataFile)) {
      nbt = new NbtCompound();
    } else {
      try {
        nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Failed to read existing item image cache data before writing");
        nbt = new NbtCompound();
      }
    }

    CacheData data = CacheData.fromNbt(nbt);
    touched.forEach((baseHash) -> {
      CacheFile file = data.hashes().get(baseHash);
      if (file == null) {
        return;
      }
      file.setLastAccess(now);
    });
    generated.forEach((baseHash, image) -> {
      String hash = image.hash();
      data.hashes().put(baseHash, new CacheFile(hash, now));

      try {
        ImageIO.write(image.toBufferedImage(), "png", cacheDir.resolve(hash + ".png").toFile());
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Failed to save item image to cache: {}", hash, e);
      }
    });

    NbtIo.writeCompressed(data.toNbt(), dataFile);

    CompletableFuture.runAsync(() -> trimExpired(data), Util.getIoWorkerExecutor());
  }

  public static ItemManager getInstance() {
    if (instance == null) {
      instance = new ItemManager(MinecraftClient.getInstance());
    }
    return instance;
  }

  public static Identifier getItemModelId(CustomId id) {
    return Identifier.of(Constants.MOD_ID, "item/" + id.pack() + "/" + id.resource());
  }

  public static void runBackgroundClean() {
    CompletableFuture.runAsync(
        () -> {
          try {
            Path dataFile = getDataFile(getCacheDir());

            if (Files.notExists(dataFile) || !Files.isRegularFile(dataFile)) {
              return;
            }

            NbtCompound nbt = NbtIo.readCompressed(dataFile, NbtSizeTracker.ofUnlimitedBytes());
            CacheData data = CacheData.fromNbt(nbt);

            trimExpired(data);
          } catch (Exception ignored) {
            // TODO: Handle exception
          }
        }, Util.getIoWorkerExecutor());
  }

  private static Image generateItemHookImage() {
    Image.Color[] pixels = new Image.Color[5 * 3];
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = Image.Color.transparent();
    }

    pixels[Image.getIndex(3, 2, 0)] = new Image.Color(99, 99, 99, 255);
    pixels[Image.getIndex(3, 1, 1)] = new Image.Color(99, 99, 99, 255);
    pixels[Image.getIndex(3, 2, 1)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 3, 1)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 0, 2)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 1, 2)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 3, 2)] = new Image.Color(38, 38, 38, 255);
    pixels[Image.getIndex(3, 4, 2)] = new Image.Color(53, 53, 53, 255);
    return Image.fromPixels(pixels, 5, 3);
  }

  private static UnbakedModel getGeneratedItemModel() {
    JsonObject json = new JsonObject();
    json.addProperty("parent", "builtin/generated");
    json.addProperty("gui_light", "front");
    JsonObject display = new JsonObject();
    display.add("ground", genDisplayJsonObject(
        0, 0, 0,
        0, 2, 0,
        0.5f, 0.5f, 0.5f));
    display.add("head", genDisplayJsonObject(
        0, 180, 0,
        0, 13, 7,
        1, 1, 1));
    display.add("thirdperson_righthand", genDisplayJsonObject(
        0, 0, 0,
        0, 3, 1,
        0.55f, 0.55f, 0.55f));
    display.add("firstperson_righthand", genDisplayJsonObject(
        0, -90, 25,
        1.13f, 3.2f, 1.13f,
        0.68f, 0.68f, 0.68f));
    display.add("fixed", genDisplayJsonObject(
        0, 180, 0,
        0, 0, 0,
        1, 1, 1));
    json.add("display", display);
    return JsonUnbakedModel.deserialize(new StringReader(json.toString()));
  }

  private static JsonObject genDisplayJsonObject(float... values) {
    if (values.length != 9) {
      throw new IllegalArgumentException("Values must contain 9 values");
    }

    JsonArray rotation = new JsonArray();
    rotation.add(values[0]);
    rotation.add(values[1]);
    rotation.add(values[2]);

    JsonArray translation = new JsonArray();
    translation.add(values[3]);
    translation.add(values[4]);
    translation.add(values[5]);

    JsonArray scale = new JsonArray();
    scale.add(values[6]);
    scale.add(values[7]);
    scale.add(values[8]);

    JsonObject display = new JsonObject();
    display.add("rotation", rotation);
    display.add("translation", translation);
    display.add("scale", scale);

    return display;
  }

  private static Path getCacheDir() {
    return PathAccessor.getInstance().getGameDir()
        .resolve("data")
        .resolve(Constants.MOD_ID)
        .resolve("cache")
        .resolve("items");
  }

  private static Path getDataFile(Path cacheDir) {
    return cacheDir.resolve("data.dat");
  }

  private static void deleteImage(Path cacheDir, String hash) throws IOException {
    Path path = cacheDir.resolve(hash + ".png");
    if (Files.notExists(path) || !Files.isRegularFile(path)) {
      return;
    }
    Files.delete(path);
  }

  private static Image loadImage(Path cacheDir, String hash) {
    Path path = cacheDir.resolve(hash + ".png");
    if (Files.notExists(path) || !Files.isRegularFile(path)) {
      return Image.empty();
    }

    try {
      return Image.read(Files.newInputStream(path));
    } catch (IOException e) {
      return Image.empty();
    }
  }

  private static long getTtlMs() {
    return 1000L * 60 * 60 * 24 * CustomPaintingsConfig.getInstance().cacheTtl.getValue();
  }

  private static void trimExpired(CacheData data) {
    Path cacheDir = getCacheDir();
    final long now = Util.getEpochTimeMs();
    final long ttl = getTtlMs();
    final long expired = now - ttl;

    if (Files.notExists(cacheDir)) {
      return;
    }

    Path dataFile = getDataFile(cacheDir);

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(cacheDir)) {
      directoryStream.forEach((path) -> {
        if (path.equals(dataFile)) {
          return;
        }

        String filename = path.getFileName().toString();
        if (!filename.toLowerCase().endsWith(".png")) {
          return;
        }

        String hash = filename.substring(0, filename.length() - 4);

        CacheFile file = data.hashes().get(hash);
        if (file == null || file.lastAccess() < expired) {
          try {
            deleteImage(cacheDir, hash);
            data.hashes().remove(hash);
          } catch (IOException e) {
            CustomPaintingsMod.LOGGER.warn(String.format("Failed to delete stale cached item image %s.png", hash), e);
          }
          return;
        }
      });
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to access item image cache directory for cleaning", e);
      return;
    }

    try {
      NbtIo.writeCompressed(data.toNbt(), dataFile);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to write trimmed item iamge cache data file", e);
    }
  }

  private static boolean isEmpty(Image image) {
    return image == null || image.isEmpty();
  }

  private record CacheData(
      int version,
      HashMap<String, CacheFile> hashes) {
    public static final String NBT_VERSION = "Version";
    public static final String NBT_HASHES = "Hashes";
    public static final Codec<CacheData> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        Codec.INT.fieldOf(NBT_VERSION).forGetter(CacheData::version),
        Codec.unboundedMap(Codec.STRING, CacheFile.CODEC)
            .xmap(HashMap::new, Function.identity())
            .fieldOf(NBT_HASHES)
            .forGetter(CacheData::hashes))
        .apply(instance, CacheData::new));

    public static CacheData empty() {
      return new CacheData(1, new HashMap<>());
    }

    public static CacheData fromNbt(NbtCompound nbt) {
      try {
        return CODEC.parse(NbtOps.INSTANCE, nbt).getPartialOrThrow();
      } catch (Exception e) {
        return CacheData.empty();
      }
    }

    public NbtCompound toNbt() {
      return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }
  }

  private static final class CacheFile {
    public static final String NBT_HASH = "Hash";
    public static final String NBT_LAST_ACCESS = "LastAccess";
    public static final Codec<CacheFile> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        Codec.STRING.fieldOf(NBT_HASH).forGetter(CacheFile::hash),
        Codec.LONG.fieldOf(NBT_LAST_ACCESS).forGetter(CacheFile::lastAccess))
        .apply(instance, CacheFile::new));

    private final String hash;
    private long lastAccess;

    private CacheFile(String hash, long lastAccess) {
      this.hash = hash;
      this.lastAccess = lastAccess;
    }

    public String hash() {
      return this.hash;
    }

    public long lastAccess() {
      return this.lastAccess;
    }

    public void setLastAccess(long lastAccess) {
      this.lastAccess = lastAccess;
    }

    public boolean isExpired(long expired) {
      return this.lastAccess <= expired;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (CacheFile) obj;
      return Objects.equals(this.hash, that.hash) && this.lastAccess == that.lastAccess;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.hash, this.lastAccess);
    }
  }

  private record LoadResult(
      SpriteContents sprite,
      boolean loadedFromCache,
      CompletableFuture<SpriteContents> generation) {
  }
}
