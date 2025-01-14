package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
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
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.registry.ImageStore;
import me.roundaround.custompaintings.resource.*;
import me.roundaround.custompaintings.resource.legacy.LegacyPackConverter;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.StringUtil;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClientPaintingRegistry extends CustomPaintingRegistry {
  private static final Identifier PAINTING_BACK_ID = Identifier.ofVanilla("back");
  private static final Identifier BACK_TEXTURE_ID = Identifier.ofVanilla("textures/painting/back.png");
  private static final Identifier EARTH_TEXTURE_ID = Identifier.ofVanilla("textures/painting/earth.png");

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<CustomId> spriteIds = new HashSet<>();
  private final ImageStore cachedImages = new ImageStore();
  private final HashSet<CustomId> neededImages = new HashSet<>();
  private final HashMap<CustomId, ImageChunkBuilder> imageBuilders = new HashMap<>();
  private final LinkedHashMap<CustomId, CompletableFuture<PaintingData>> pendingDataRequests = new LinkedHashMap<>();
  private final HashMap<CustomId, Boolean> finishedMigrations = new HashMap<>();

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
    this.atlas = new SpriteAtlasTexture(Identifier.of(CustomPaintingsMod.MOD_ID, "textures/atlas/paintings.png"));
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
      // In single player we will (usually) initialize the atlas before the first render. In multiplayer and potentially
      // in single player on slower PCs however, first render could come before we receive the summary packet and
      // initialize the atlas. In those cases, atlas.getSprite throws an exception. If it does, simply build the sprite
      // atlas and try again.
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
      List<PackData> packs, UUID serverId, String combinedImageHash, Map<CustomId, Boolean> finishedMigrations
  ) {
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

    LegacyPackConverter.getInstance()
        .checkForLegacyPacks(this.client)
        .orTimeout(30, TimeUnit.SECONDS)
        .thenAcceptAsync((metas) -> {
          if (this.client.player == null) {
            return;
          }

          HashSet<String> alreadyLoaded = this.packsMap.values()
              .stream()
              .map(PackData::sourceLegacyPack)
              .filter((id) -> id != null && !id.isBlank())
              .collect(Collectors.toCollection(HashSet::new));
          HashSet<String> legacyPacks = metas.stream()
              .map(PackMetadata::packFileUid)
              .map(PackFileUid::stringValue)
              .collect(Collectors.toCollection(HashSet::new));
          legacyPacks.removeAll(alreadyLoaded);

          if (!legacyPacks.isEmpty()) {
            CustomSystemToasts.addLegacyPacksFound(this.client, legacyPacks.size());
          }
        }, this.client);
  }

  private void initCacheAndSpriteAtlas(boolean initialLoad, UUID serverId, String serverCombinedImageHash) {
    if (this.isHashCorrectAndAllImagesPresent(serverCombinedImageHash, this.images::contains)) {
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
      this.images.setAll(cacheRead.images(), cacheRead.hashes());
    } else {
      CustomPaintingsMod.LOGGER.info("Requesting images from server");
      this.cacheDirty = true;
      this.cachedImages.putAll(cacheRead.images(), cacheRead.hashes());
      ClientNetworking.sendHashesPacket(this.cachedImages.getHashes());
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
    CustomPaintingsMod.LOGGER.info("Painting images downloaded and sprite atlas refreshed in {}s",
        StringUtil.formatDuration(Util.getMeasuringTimeMs() - this.waitingForImagesTimer)
    );

    this.imagesExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.bytesReceived = 0;
  }

  private void buildSpriteAtlas() {
    this.images.removeIf((id) -> !this.isValidImageId(id));

    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BasicTextureSprite.fetch(this.client, PAINTING_BACK_ID, BACK_TEXTURE_ID));
    sprites.add(VanillaIconSprite.create(this.client, PackIcons.MINECRAFT_ICON_ID.toIdentifier(), "vanilla"));
    sprites.add(
        BasicTextureSprite.fetch(this.client, PackIcons.MINECRAFT_HIDDEN_ICON_ID.toIdentifier(), EARTH_TEXTURE_ID));
    this.paintings.values().forEach((painting) -> this.getSpriteContents(painting).ifPresent(sprites::add));
    this.packsMap.keySet().forEach((packId) -> this.getSpriteContents(packId).ifPresent(sprites::add));

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));

    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).map(CustomId::from).toList());

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
      ImageStore.StoredImage cachedImage = this.cachedImages.get(id);
      if (cachedImage == null) {
        return;
      }
      this.images.put(id, cachedImage.image(), cachedImage.hash());
    });
  }

  private void cacheNewImages() {
    if (!this.usingCache() || !this.neededImages.isEmpty() || !this.cacheDirty) {
      return;
    }

    final ImageStore images = this.images.copy();
    final String combinedImageHash = this.combinedImageHash;
    CompletableFuture.supplyAsync(() -> {
      try {
        CacheManager.getInstance().saveToFile(images, combinedImageHash);
        return true;
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(e);
        CustomPaintingsMod.LOGGER.warn("Failed to write images and metadata to cache.");
        return false;
      }
    }, Util.getIoWorkerExecutor()).thenAcceptAsync((succeeded) -> {
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
        painting.id(), this.images.getImage(painting.id()), painting.getScaledWidth(), painting.getScaledHeight());
  }

  private Optional<SpriteContents> getSpriteContents(String packId) {
    CustomId id = PackIcons.customId(packId);
    return this.getSpriteContents(id, this.images.getImage(id), 16, 16);
  }

  private Optional<SpriteContents> getSpriteContents(CustomId id, Image image, int width, int height) {
    if (image == null || image.isEmpty()) {
      if (this.neededImages.contains(id)) {
        return Optional.of(LoadingSprite.generate(id.toIdentifier(), width, height));
      }
      return Optional.empty();
    }
    NativeImage nativeImage = getNativeImage(image);
    return Optional.of(
        new SpriteContents(id.toIdentifier(), new SpriteDimensions(image.width(), image.height()), nativeImage,
            getResourceMetadata(image)
        ));
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

  private static ResourceMetadata getResourceMetadata(Image image) {
    return new ResourceMetadata.Builder().add(AnimationResourceMetadata.READER,
            new AnimationResourceMetadata(ImmutableList.of(new AnimationFrameResourceMetadata(0, -1)), image.width(),
                image.height(), 1, false
            )
        )
        .build();
  }
}
