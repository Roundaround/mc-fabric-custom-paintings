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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

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
import me.roundaround.custompaintings.roundalib.event.MinecraftClientEvents;
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
import net.minecraft.client.render.model.SimpleModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.util.SpriteIdentifier;
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
  public static final Identifier PAINTING_ITEM_TEXTURE_ID = Identifier.of(Constants.MOD_ID,
      "textures/atlas/items.png");

  private static final Image HOOK_IMAGE = generateItemHookImage();
  private static final Identifier VANILLA_TEXTURE = Identifier.ofVanilla("textures/item/painting.png");
  private static final Identifier VANILLA_ID = Identifier.of(Constants.MOD_ID, "item/painting");

  private static ItemManager instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<CustomId> spriteIds = new HashSet<>();
  private final ArrayList<CompletableFuture<GeneratedImage>> generateFutures = new ArrayList<>();
  private final HashMap<CustomId, Image> generated = new HashMap<>();
  private final HashSet<CustomId> usesVanilla = new HashSet<>();
  private final ArrayList<PaintingData> paintings = new ArrayList<>();
  private final SpriteGetter spriteGetter = new SpriteGetter();

  private boolean atlasInitialized = false;
  private Function<CustomId, Image> baseImageSupplier = ItemManager::emptyBaseImageSupplier;
  private AtomicReference<CompletableFuture<Void>> buildFuture = new AtomicReference<>();

  private ItemManager(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(PAINTING_ITEM_TEXTURE_ID);
    this.client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    MinecraftClientEvents.CLOSE.register((c) -> {
      if (c == this.client) {
        this.close();
      }
    });
  }

  public Identifier getAtlasId() {
    return this.atlas.getId();
  }

  public Sprite getMissingSprite() {
    try {
      return this.atlas.getSprite(MissingSprite.getMissingSpriteId());
    } catch (IllegalStateException e) {
      // In single player we will (usually) initialize the atlas before the first
      // render. In multiplayer and potentially in single player on slower PCs
      // however, first render could come before we receive the summary packet and
      // initialize the atlas. In those cases, atlas.getSprite throws an exception. If
      // it does, simply build the sprite atlas and try again.
      if (!this.atlasInitialized) {
        this.build();
        return this.getMissingSprite();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Sprite getVanillaSprite() {
    try {
      return this.atlas.getSprite(VANILLA_ID);
    } catch (IllegalStateException e) {
      // See comment in getMissingSprite about atlas initialization
      if (!this.atlasInitialized) {
        this.build();
        return this.getVanillaSprite();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Sprite getSprite(PaintingData painting) {
    return this.getSprite(painting.id());
  }

  public Sprite getSprite(CustomId paintingId) {
    return this.getItemSprite(getItemId(paintingId));
  }

  public Sprite getSprite(Identifier textureId) {
    return this.getItemSprite(CustomId.from(textureId));
  }

  public void build(
      Collection<PaintingData> paintings,
      Function<CustomId, Image> imageSupplier) {
    this.paintings.clear();
    this.paintings.addAll(paintings);
    this.baseImageSupplier = imageSupplier;
    this.build();
  }

  public void close() {
    CompletableFuture<Void> future = this.buildFuture.getAndSet(null);
    if (future != null) {
      future.cancel(false);
    }

    this.atlas.clear();
    this.spriteIds.clear();
    this.generateFutures.clear();
    this.generated.clear();
    this.paintings.clear();
    this.atlasInitialized = false;
    this.baseImageSupplier = ItemManager::emptyBaseImageSupplier;
  }

  private Sprite getItemSprite(CustomId id) {
    if (this.usesVanilla.contains(id)) {
      return this.getVanillaSprite();
    }
    if (!this.spriteIds.contains(id)) {
      CustomPaintingsMod.LOGGER.warn("Sprite not found for {}", id);
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id.toIdentifier());
  }

  private void build() {
    ArrayList<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BasicTextureSprite.fetch(this.client, VANILLA_ID, VANILLA_TEXTURE));

    HashSet<String> loadededHashes = new HashSet<>();
    HashMap<CustomId, CompletableFuture<GeneratedImage>> generateFutures = new HashMap<>();

    CacheData data = this.loadCacheData();
    final long now = Util.getEpochTimeMs();
    final long expired = now - getTtlMs();

    this.paintings.forEach((painting) -> {
      CustomId paintingId = painting.id();
      CustomId itemId = CustomId.from(getItemModelId(paintingId));
      Image generatedImage = this.generated.get(itemId);
      if (generatedImage != null) {
        this.getSpriteContents(painting, generatedImage).ifPresent(sprites::add);
        return;
      }

      Image baseImage = this.baseImageSupplier.apply(paintingId);
      if (isEmpty(baseImage)) {
        this.usesVanilla.add(itemId);
        return;
      }

      LoadResult result = this.loadOrScheduleGeneration(data, expired, painting, baseImage);
      if (result.sprite != null) {
        sprites.add(result.sprite);
      } else {
        this.usesVanilla.add(itemId);
      }
      if (result.generateFuture != null) {
        generateFutures.put(itemId, result.generateFuture);
      }
    });

    for (String hash : loadededHashes) {
      CacheFile file = data.hashes().get(hash);
      if (file == null || file.isExpired(expired)) {
        continue;
      }
      file.setLastAccess(now);
    }

    this.saveCacheData(data);

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));
    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).map(CustomId::from).toList());

    this.bakeModels();

    this.atlasInitialized = true;

    if (generateFutures.isEmpty()) {
      return;
    }

    // TODO: Sprites/models are not being updated after image generation completes
    this.buildFuture.updateAndGet((previousFuture) -> {
      if (previousFuture != null) {
        previousFuture.cancel(false);
      }

      return CompletableFuture.allOf(generateFutures.values().toArray(CompletableFuture[]::new))
          .thenRunAsync(() -> {
            HashMap<CustomId, GeneratedImage> generated = new HashMap<>();
            generateFutures.forEach((id, future) -> {
              generated.put(id, future.join());
            });

            this.onGenerationCompleted(generated);
          }, this.client);
    });
  }

  private LoadResult loadOrScheduleGeneration(
      CacheData data,
      long expired,
      PaintingData painting,
      Image baseImage) {
    Supplier<LoadResult> generate = () -> new LoadResult(
        null,
        false,
        CompletableFuture.supplyAsync(
            () -> this.generateImage(painting, baseImage),
            Util.getMainWorkerExecutor()));

    if (!CustomPaintingsConfig.getInstance().cacheImages.getPendingValue()) {
      return generate.get();
    }

    String baseHash = baseImage.hash();
    CacheFile file = data.hashes().get(baseHash);

    if (file == null || file.isExpired(expired)) {
      return generate.get();
    }

    Image image = loadImage(getCacheDir(), file.hash());
    if (isEmpty(image)) {
      return generate.get();
    }

    return new LoadResult(
        this.getSpriteContents(painting, image).orElse(null),
        true,
        null);
  }

  private void bakeModels() {
    HashMap<Identifier, UnbakedModel> unbakedModels = new HashMap<>();
    unbakedModels.put(Identifier.ofVanilla("item/generated"), getGeneratedItemModel());

    HashMap<Identifier, ItemAsset> itemAssets = new HashMap<>();
    this.paintings.forEach((painting) -> {
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

    baker.bake(this.spriteGetter, Util.getMainWorkerExecutor()).thenAccept((bakedModels) -> {
      BakedModelManagerAccessor accessor = (BakedModelManagerAccessor) this.client.getBakedModelManager();
      HashMap<Identifier, ItemModel> itemModels = new HashMap<>(accessor.getBakedItemModels());
      itemModels.putAll(bakedModels.itemStackModels());
      accessor.setBakedItemModels(itemModels);
    }).join();
  }

  private void onGenerationCompleted(Map<CustomId, GeneratedImage> generated) {
    CacheData data = this.loadCacheData();
    final long now = Util.getEpochTimeMs();
    Path cacheDir = getCacheDir();

    try {
      if (Files.notExists(cacheDir)) {
        Files.createDirectories(cacheDir);
      }
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to create cache directory", e);

      generated.forEach((id, generatedImage) -> {
        String baseHash = generatedImage.baseHash();
        Image image = generatedImage.image();
        if (baseHash == null || isEmpty(image)) {
          return;
        }
        this.generated.put(id, image);
      });

      this.build();
      this.generated.clear();
      return;
    }

    HashSet<CustomId> noLongerVanilla = new HashSet<>();
    generated.forEach((id, generatedImage) -> {
      String baseHash = generatedImage.baseHash();
      Image image = generatedImage.image();
      if (baseHash == null || isEmpty(image)) {
        return;
      }

      String hash = image.hash();
      this.generated.put(id, image);
      data.hashes().put(baseHash, new CacheFile(hash, now));

      try {
        ImageIO.write(image.toBufferedImage(), "png", cacheDir.resolve(hash + ".png").toFile());
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Failed to save item image to cache: {}", hash, e);
      }

      noLongerVanilla.add(id);
    });

    this.saveCacheData(data);
    this.build();

    this.usesVanilla.removeAll(noLongerVanilla);
  }

  private GeneratedImage generateImage(PaintingData painting, Image baseImage) {
    if (isEmpty(baseImage)) {
      return new GeneratedImage(painting.id(), null, Image.empty());
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

    Image image = baseImage.apply(
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

    return new GeneratedImage(painting.id(), baseImage.hash(), image);
  }

  private Optional<SpriteContents> getSpriteContents(PaintingData painting, Image image) {
    return this.getSpriteContents(getItemId(painting.id()), image);
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
    // TODO: Some kind of mutex/lock

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

  private void saveCacheData(CacheData data) {
    // TODO: Some kind of mutex/lock

    try {
      Path cacheDir = getCacheDir();
      if (Files.notExists(cacheDir)) {
        Files.createDirectories(cacheDir);
      }

      Path dataFile = getDataFile(cacheDir);
      NbtIo.writeCompressed(data.toNbt(), dataFile);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Failed to save item image cache data", e);
    }
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

  private static CustomId getItemId(CustomId paintingId) {
    return CustomId.from(getItemModelId(paintingId));
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

  private static Image emptyBaseImageSupplier(CustomId id) {
    return Image.empty();
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
      @Nullable SpriteContents sprite,
      boolean loadedFromCache,
      @Nullable CompletableFuture<GeneratedImage> generateFuture) {
  }

  private record GeneratedImage(
      CustomId paintingId,
      String baseHash,
      Image image) {
  }

  private class SpriteGetter implements ErrorCollectingSpriteGetter {
    @Override
    public Sprite get(SpriteIdentifier id, SimpleModel model) {
      return ItemManager.this.getSprite(id.getTextureId());
    }

    @Override
    public Sprite getMissing(String name, SimpleModel model) {
      return ItemManager.this.getMissingSprite();
    }
  }
}
