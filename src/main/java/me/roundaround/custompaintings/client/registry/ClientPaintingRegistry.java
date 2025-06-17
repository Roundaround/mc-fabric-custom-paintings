package me.roundaround.custompaintings.client.registry;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.PacksLoadedListener;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.texture.BasicTextureSprite;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.client.texture.VanillaIconSprite;
import me.roundaround.custompaintings.client.toast.CustomSystemToasts;
import me.roundaround.custompaintings.client.toast.DownloadProgressToast;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.mixin.BakedModelManagerAccessor;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.resource.file.FileUid;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.legacy.LegacyPackConverter;
import me.roundaround.custompaintings.roundalib.event.MinecraftClientEvents;
import me.roundaround.custompaintings.roundalib.util.PathAccessor;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.StringUtil;
import net.fabricmc.loader.api.FabricLoader;
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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class ClientPaintingRegistry extends CustomPaintingRegistry {
  public static final Identifier CUSTOM_PAINTING_TEXTURE_ID = Identifier.of(CustomPaintingsMod.MOD_ID,
      "textures/atlas/paintings.png");

  private static final Identifier PAINTING_BACK_ID = Identifier.ofVanilla("back");
  private static final Identifier BACK_TEXTURE_ID = Identifier.ofVanilla("textures/painting/back.png");
  private static final Identifier EARTH_TEXTURE_ID = Identifier.ofVanilla("textures/painting/earth.png");
  private static final Image HOOK_IMAGE = generateItemHookImage();

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<CustomId> spriteIds = new HashSet<>();
  private final HashMap<CustomId, Image> cachedImages = new HashMap<>();
  private final HashSet<CustomId> neededImages = new HashSet<>();
  private final HashMap<CustomId, ImageChunkBuilder> imageBuilders = new HashMap<>();
  private final LinkedHashMap<CustomId, CompletableFuture<PaintingData>> pendingDataRequests = new LinkedHashMap<>();
  private final HashMap<CustomId, Boolean> finishedMigrations = new HashMap<>();
  private final SpriteGetter spriteGetter = new SpriteGetter();

  private boolean atlasInitialized = false;
  private boolean packsReceived = false;
  private boolean cacheDirty = false;
  private long waitingForImagesTimer;
  private int imagesExpected;
  private int bytesExpected;
  private int imagesReceived;
  private int bytesReceived;

  private ClientPaintingRegistry(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(CUSTOM_PAINTING_TEXTURE_ID);
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    MinecraftClientEvents.CLOSE.register(this::close);
  }

  public static ClientPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ClientPaintingRegistry(MinecraftClient.getInstance());
    }
    return instance;
  }

  @Override
  protected DynamicRegistryManager getRegistryManager() {
    return this.client.world == null ? null : this.client.world.getRegistryManager();
  }

  @Override
  public void setPacks(HashMap<String, PackData> packsMap) {
    super.setPacks(packsMap);

    CustomPaintingsMod.LOGGER.info("{} painting metadata entries loaded", this.paintings.size());
    this.packsReceived = true;

    this.pendingDataRequests.forEach((id, future) -> future.complete(this.get(id)));
    this.pendingDataRequests.clear();

    this.clearUnknownMigrations();

    if (this.client != null && this.client.currentScreen instanceof PacksLoadedListener screen) {
      screen.onPacksLoaded();
    }
  }

  @Override
  public void clear() {
    super.clear();

    this.packsReceived = false;
    this.cacheDirty = false;
    this.atlas.clear();
    this.spriteIds.clear();
    this.neededImages.clear();
    this.cachedImages.clear();
    this.imageBuilders.clear();
    this.pendingDataRequests.forEach((id, future) -> future.cancel(true));
    this.pendingDataRequests.clear();
    this.finishedMigrations.clear();
    this.imagesExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.bytesReceived = 0;
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
        this.buildSpriteAtlas();
        return this.getMissingSprite();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Sprite getBackSprite() {
    return this.atlas.getSprite(PAINTING_BACK_ID);
  }

  public Sprite getSprite(CustomId id) {
    if (!this.spriteIds.contains(id)) {
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id.toIdentifier());
  }

  public Sprite getSprite(PaintingData data) {
    if (data.isEmpty()) {
      return this.getBackSprite();
    }
    if (data.vanilla()) {
      ClientWorld world = this.client.world;
      if (world == null) {
        return this.getMissingSprite();
      }

      PaintingVariant variant = world.getRegistryManager()
          .getOrThrow(RegistryKeys.PAINTING_VARIANT)
          .get(data.id().toIdentifier());
      return this.client.getPaintingManager().getPaintingSprite(variant);
    }
    return this.getSprite(data.id());
  }

  public Map<CustomId, Boolean> getFinishedMigrations() {
    return Map.copyOf(this.finishedMigrations);
  }

  public void markMigrationFinished(CustomId id, boolean succeeded) {
    this.finishedMigrations.put(id, succeeded);
  }

  public void setFinishedMigrations(Map<CustomId, Boolean> finishedMigrations) {
    this.finishedMigrations.clear();
    this.finishedMigrations.putAll(finishedMigrations);
  }

  public void clearUnknownMigrations() {
    this.finishedMigrations.keySet().removeIf((id) -> !this.migrations.containsKey(id));
  }

  public void processSummary(
      List<PackData> packs,
      UUID serverId,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations) {
    boolean initialLoad = this.packsMap.isEmpty();
    if (initialLoad) {
      this.checkAndPromptForLegacyPacks();
    }

    this.setPacks(packs);
    this.setFinishedMigrations(finishedMigrations);
    this.initCacheAndSpriteAtlas(initialLoad, serverId, combinedImageHash);
  }

  private void setPacks(List<PackData> packsList) {
    HashMap<String, PackData> packs = new HashMap<>(packsList.size());
    packsList.forEach((pack) -> packs.put(pack.id(), pack));
    this.setPacks(packs);
  }

  private void checkAndPromptForLegacyPacks() {
    if (!this.client.isInSingleplayer() || CustomPaintingsConfig.getInstance().silenceAllConvertPrompts.getValue() ||
        CustomPaintingsPerWorldConfig.getInstance().silenceConvertPrompt.getValue()) {
      return;
    }

    LegacyPackConverter.getInstance().checkForLegacyPacks(this.client).orTimeout(30, TimeUnit.SECONDS).thenAcceptAsync(
        (metas) -> {
          if (this.client.player == null) {
            return;
          }

          HashSet<String> alreadyLoaded = this.packsMap.values()
              .stream()
              .map(PackData::sourceLegacyPack)
              .filter((id) -> id.isPresent() && !id.get().isBlank())
              .map(Optional::get)
              .collect(Collectors.toCollection(HashSet::new));
          HashSet<String> legacyPacks = metas.stream()
              .map(Metadata::fileUid)
              .map(FileUid::stringValue)
              .collect(Collectors.toCollection(HashSet::new));
          legacyPacks.removeAll(alreadyLoaded);

          if (!legacyPacks.isEmpty()) {
            CustomSystemToasts.addLegacyPacksFound(this.client, legacyPacks.size());
          }
        }, this.client);
  }

  private void initCacheAndSpriteAtlas(boolean initialLoad, UUID serverId, String serverCombinedImageHash) {
    if (this.isHashCorrectAndAllImagesPresent(serverCombinedImageHash, this.images::containsKey)) {
      CustomPaintingsMod.LOGGER.info("All image info still valid, skipping re-fetching images");
      this.buildSpriteAtlas();
      return;
    }

    // TODO: Do I need to clear all these here?
    this.images.clear();
    this.cachedImages.clear();
    this.cacheDirty = false;

    this.combinedImageHash = serverCombinedImageHash;

    if (!this.usingCache()) {
      CustomPaintingsMod.LOGGER.info("Not using cache, requesting all images from server");
      ClientNetworking.sendHashesPacket(new HashMap<>(0));
      this.buildSpriteAtlas();
      return;
    }

    if (initialLoad) {
      CacheRead cacheRead = this.readCache(serverId);
      this.postCacheRead(cacheRead);
      return;
    }

    CompletableFuture.supplyAsync(() -> this.readCache(serverId), Util.getIoWorkerExecutor())
        .thenAcceptAsync(this::postCacheRead, this.client);
  }

  private CacheRead readCache(UUID serverId) {
    if (!this.usingCache()) {
      return null;
    }

    return CacheManager.getInstance()
        .loadFromFile(serverId, ResourceUtil.getAllImageIds(this.packsMap.keySet(), this.paintings.keySet()));
  }

  private void postCacheRead(CacheRead cacheRead) {
    if (cacheRead == null) {
      CustomPaintingsMod.LOGGER.info("Cache was empty; requesting all images from server");
      this.cacheDirty = true;
      ClientNetworking.sendHashesPacket(Map.of());
    } else if (this.isHashCorrectAndAllImagesPresent(cacheRead.combinedHash(), cacheRead.images()::containsKey)) {
      CustomPaintingsMod.LOGGER.info("All images successfully pulled from cache; skipping server image download");
      this.images.clear();
      this.images.putAll(cacheRead.images());
    } else {
      CustomPaintingsMod.LOGGER.info("Requesting images from server");
      this.cacheDirty = true;
      this.cachedImages.putAll(cacheRead.images());
      ClientNetworking.sendHashesPacket(this.cachedImages.entrySet()
          .stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              (entry) -> entry.getValue().hash())));
    }

    this.buildSpriteAtlas();
  }

  private boolean isHashCorrectAndAllImagesPresent(String newCombinedHash, Predicate<CustomId> imageCheck) {
    if (!Objects.equals(newCombinedHash, this.combinedImageHash)) {
      return false;
    }

    HashSet<CustomId> neededIds = ResourceUtil.getAllImageIds(this.packsMap.keySet(), this.paintings.keySet());
    return neededIds.stream().allMatch(imageCheck);
  }

  public void trackExpectedPackets(List<CustomId> expectedIds, int imageCount, int byteCount) {
    if (expectedIds.isEmpty() || byteCount == 0) {
      CustomPaintingsMod.LOGGER.info(
          "Combined image hash is invalid, but all the individual images are fine. Rebuilding cache.");
      this.cacheDirty = true;
      this.cacheNewImages();
      this.buildSpriteAtlas();
      return;
    }

    CustomPaintingsMod.LOGGER.info("Expecting {} painting image(s) from server", expectedIds.size());
    this.waitingForImagesTimer = Util.getMeasuringTimeMs();

    this.neededImages.clear();
    this.neededImages.addAll(expectedIds);
    this.imagesExpected = imageCount;
    this.bytesExpected = byteCount;

    if (this.client.player != null && !this.client.isInSingleplayer()) {
      DownloadProgressToast.add(this.client.getToastManager(), this.imagesExpected, this.bytesExpected);
    }

    this.copyInCachedImageData(Set.copyOf(expectedIds));
    this.buildSpriteAtlas();
  }

  public void setPaintingImage(CustomId id, Image image) {
    this.imagesReceived++;
    this.bytesReceived += image.getSize();

    DownloadProgressToast toast = DownloadProgressToast.get(this.client.getToastManager());
    if (toast != null) {
      toast.setReceived(this.imagesReceived, this.bytesReceived);
    }

    this.setFull(id, image);
  }

  public void setPaintingHeader(CustomId id, int width, int height, int totalChunks) {
    this.setPart(id, (builder) -> builder.set(width, height, totalChunks));
  }

  public void setPaintingChunk(CustomId id, int index, byte[] bytes) {
    this.bytesReceived += bytes.length;

    DownloadProgressToast toast = DownloadProgressToast.get(this.client.getToastManager());
    if (toast != null) {
      toast.setReceived(this.imagesReceived, this.bytesReceived);
    }

    this.setPart(id, (builder) -> builder.set(index, bytes));
  }

  public CompletableFuture<PaintingData> safeGet(CustomId id) {
    if (this.packsReceived) {
      return CompletableFuture.completedFuture(this.get(id));
    }

    CompletableFuture<PaintingData> future = new CompletableFuture<>();
    this.pendingDataRequests.put(id, future);
    return future;
  }

  private void close(MinecraftClient client) {
    this.clear();
  }

  private void setPart(CustomId id, Function<ImageChunkBuilder, Boolean> setter) {
    ImageChunkBuilder builder = this.imageBuilders.computeIfAbsent(id, (identifier) -> new ImageChunkBuilder());
    if (setter.apply(builder)) {
      this.imagesReceived++;

      DownloadProgressToast toast = DownloadProgressToast.get(this.client.getToastManager());
      if (toast != null) {
        toast.setReceived(this.imagesReceived, this.bytesReceived);
      }

      this.setFull(id, builder.generate());
      this.imageBuilders.remove(id);
    }
  }

  private void setFull(CustomId id, Image image) {
    this.images.put(id, image);

    this.cacheDirty = true;
    this.neededImages.remove(id);

    if (!this.neededImages.isEmpty()) {
      return;
    }

    // TODO: Recreate the combined hash to validate all the images.

    CustomPaintingsMod.LOGGER.info("All painting images received from server. Refreshing sprite atlas...");
    this.buildSpriteAtlas();
    this.cacheNewImages();
    CustomPaintingsMod.LOGGER.info(
        "Painting images downloaded and sprite atlas refreshed in {}s",
        StringUtil.formatDuration(Util.getMeasuringTimeMs() - this.waitingForImagesTimer));

    this.imagesExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.bytesReceived = 0;
  }

  private void buildSpriteAtlas() {
    this.images.keySet().removeIf((id) -> !this.isValidImageId(id));

    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BasicTextureSprite.fetch(this.client, PAINTING_BACK_ID, BACK_TEXTURE_ID));
    sprites.add(VanillaIconSprite.create(this.client, PackIcons.MINECRAFT_ICON_ID.toIdentifier(), "vanilla"));
    sprites.add(BasicTextureSprite.fetch(
        this.client,
        PackIcons.MINECRAFT_HIDDEN_ICON_ID.toIdentifier(),
        EARTH_TEXTURE_ID));
    this.paintings.values().forEach((painting) -> this.getSpriteContents(painting).ifPresent(sprites::add));
    this.packsMap.keySet().forEach((packId) -> this.getSpriteContents(packId).ifPresent(sprites::add));

    HashMap<Identifier, UnbakedModel> unbakedModels = new HashMap<>();
    unbakedModels.put(Identifier.ofVanilla("item/generated"), getGeneratedItemModel());

    HashMap<Identifier, ItemAsset> itemAssets = new HashMap<>();
    this.paintings.values().forEach((painting) -> this.generateItemSprite(painting).ifPresent((sprite) -> {
      sprites.add(sprite);

      Identifier id = getItemModelId(painting.id());

      JsonObject json = new JsonObject();
      json.addProperty("parent", "minecraft:item/generated");
      JsonObject textures = new JsonObject();
      textures.addProperty("layer0", id.toString());
      json.add("textures", textures);
      JsonUnbakedModel unbakedModel = JsonUnbakedModel.deserialize(new StringReader(json.toString()));

      unbakedModels.put(id, unbakedModel);
      itemAssets.put(id, new ItemAsset(ItemModels.basic(id), ItemAsset.Properties.DEFAULT));
    }));

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));
    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).map(CustomId::from).toList());

    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
      try {
        this.atlas.save(CUSTOM_PAINTING_TEXTURE_ID, PathAccessor.getInstance().getGameDir());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

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
    });

    this.atlasInitialized = true;

    if (this.client.currentScreen instanceof PacksLoadedListener screen) {
      screen.onPackTexturesInitialized();
    }
  }

  private void copyInCachedImageData(Set<CustomId> invalidatedIds) {
    HashSet<CustomId> allIds = new HashSet<>();
    allIds.addAll(this.packsMap.keySet().stream().map(PackIcons::customId).toList());
    allIds.addAll(this.paintings.keySet());
    allIds.forEach((id) -> {
      if (invalidatedIds.contains(id)) {
        this.cachedImages.remove(id);
        return;
      }
      Image cachedImage = this.cachedImages.get(id);
      if (cachedImage == null) {
        return;
      }
      this.images.put(id, cachedImage);
    });
  }

  private void cacheNewImages() {
    if (!this.usingCache() || !this.neededImages.isEmpty() || !this.cacheDirty) {
      return;
    }

    final HashMap<CustomId, Image> images = new HashMap<>(this.images);
    final String combinedImageHash = this.combinedImageHash;
    CompletableFuture.supplyAsync(
        () -> {
          try {
            CacheManager.getInstance().saveToFile(images, combinedImageHash);
            return true;
          } catch (IOException e) {
            CustomPaintingsMod.LOGGER.warn(e);
            CustomPaintingsMod.LOGGER.warn("Failed to write images and metadata to cache.");
            return false;
          }
        }, Util.getIoWorkerExecutor()).thenAcceptAsync(
            (succeeded) -> {
              if (succeeded) {
                this.cacheDirty = false;
              }
            }, this.client);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isValidImageId(CustomId id) {
    return this.paintings.containsKey(id) ||
        (id.pack().equals(PackIcons.ICON_NAMESPACE) && this.packsMap.containsKey(id.resource()));
  }

  private Optional<SpriteContents> getSpriteContents(PaintingData painting) {
    return this.getSpriteContents(
        painting.id(),
        this.images.get(painting.id()),
        painting.getScaledWidth(),
        painting.getScaledHeight());
  }

  private Optional<SpriteContents> getSpriteContents(String packId) {
    CustomId id = PackIcons.customId(packId);
    return this.getSpriteContents(id, this.images.get(id), 16, 16);
  }

  private Optional<SpriteContents> getSpriteContents(CustomId id, Image image, int width, int height) {
    if (image == null || image.isEmpty()) {
      if (this.neededImages.contains(id)) {
        return Optional.of(LoadingSprite.generate(id.toIdentifier(), width, height));
      }
      return Optional.empty();
    }
    NativeImage nativeImage = getNativeImage(image);
    return Optional.of(new SpriteContents(
        id.toIdentifier(),
        new SpriteDimensions(image.width(), image.height()),
        nativeImage,
        ResourceMetadata.NONE));
  }

  private Optional<SpriteContents> generateItemSprite(PaintingData painting) {
    Image image = this.images.get(painting.id());
    if (image == null || image.isEmpty()) {
      return Optional.empty();
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

    Image itemSprite = image.apply(
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
            Image.Color shadow = Image.Color.average(colors).darken(0.1f);

            Image.Color[] pixels = source.copyPixels();
            for (int x = minX; x <= maxX; x++) {
              pixels[Image.getIndex(source.height(), x, sampleY + 1)] = shadow;
            }
            return new Image.Hashless(pixels, source.width(), source.height());
          }
        });

    return this.getSpriteContents(
        CustomId.from(getItemModelId(painting.id())),
        itemSprite,
        width,
        height);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean usingCache() {
    return CustomPaintingsConfig.getInstance().cacheImages.getValue() && !this.client.isInSingleplayer();
  }

  private static NativeImage getNativeImage(Image image) {
    NativeImage nativeImage = new NativeImage(image.width(), image.height(), false);
    for (int x = 0; x < image.width(); x++) {
      for (int y = 0; y < image.height(); y++) {
        nativeImage.setColorArgb(x, y, image.getARGB(x, y));
      }
    }
    return nativeImage;
  }

  public static Identifier getItemModelId(CustomId id) {
    return Identifier.of(Constants.MOD_ID, "item/" + id.pack() + "/" + id.resource());
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

  private class SpriteGetter implements ErrorCollectingSpriteGetter {
    @Override
    public Sprite get(SpriteIdentifier id, SimpleModel model) {
      return ClientPaintingRegistry.this.getSprite(CustomId.from(id.getTextureId()));
    }

    @Override
    public Sprite getMissing(String name, SimpleModel model) {
      return ClientPaintingRegistry.this.getMissingSprite();
    }
  }
}
