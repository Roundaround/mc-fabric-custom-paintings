package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.PacksLoadedListener;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.texture.BasicTextureSprite;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.client.texture.VanillaIconSprite;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.resource.legacy.LegacyPackConverter;
import me.roundaround.custompaintings.resource.legacy.PackMetadata;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientPaintingRegistry extends CustomPaintingRegistry {
  private static final Identifier PAINTING_BACK_ID = new Identifier(Identifier.DEFAULT_NAMESPACE, "back");
  private static final Identifier BACK_TEXTURE_ID = new Identifier(
      Identifier.DEFAULT_NAMESPACE, "textures/painting/back.png");
  private static final Identifier EARTH_TEXTURE_ID = new Identifier(
      Identifier.DEFAULT_NAMESPACE, "textures/painting/earth.png");

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<Identifier> spriteIds = new HashSet<>();
  private final HashMap<Identifier, Image> cachedImages = new HashMap<>();
  private final HashMap<Identifier, String> cachedImageHashes = new HashMap<>();
  private final HashSet<Identifier> neededImages = new HashSet<>();
  private final HashMap<Identifier, ImageChunkBuilder> imageBuilders = new HashMap<>();
  private final LinkedHashMap<Identifier, CompletableFuture<PaintingData>> pendingDataRequests = new LinkedHashMap<>();
  private final HashMap<Identifier, Boolean> finishedMigrations = new HashMap<>();

  private boolean atlasInitialized = false;
  private boolean packsReceived = false;
  private boolean cacheDirty = false;
  private long waitingForImagesTimer;
  private int imagesExpected;
  private int bytesExpected;
  private int imagesReceived;
  private int bytesReceived;
  private long lastDownloadUpdate = 0L;

  private ClientPaintingRegistry(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(new Identifier(CustomPaintingsMod.MOD_ID, "textures/atlas/paintings.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    ClientTickEvents.START_CLIENT_TICK.register(this::tick);
    MinecraftClientEvents.CLOSE.register(this::close);
  }

  public static ClientPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ClientPaintingRegistry(MinecraftClient.getInstance());
    }
    return instance;
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

  public Sprite getSprite(Identifier id) {
    if (!this.spriteIds.contains(id)) {
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id);
  }

  public Sprite getSprite(PaintingData data) {
    if (data.isEmpty()) {
      return this.getBackSprite();
    }
    if (data.vanilla()) {
      return this.client.getPaintingManager().getPaintingSprite(Registries.PAINTING_VARIANT.get(data.id()));
    }
    return this.getSprite(data.id());
  }

  public List<PackData> getActivePacks() {
    return this.packsList.stream().filter((pack) -> !pack.disabled()).toList();
  }

  public List<PackData> getInactivePacks() {
    return this.packsList.stream().filter(PackData::disabled).toList();
  }

  public Map<Identifier, Boolean> getFinishedMigrations() {
    return Map.copyOf(this.finishedMigrations);
  }

  public void markMigrationFinished(Identifier id, boolean succeeded) {
    this.finishedMigrations.put(id, succeeded);
  }

  public void setFinishedMigrations(Map<Identifier, Boolean> finishedMigrations) {
    this.finishedMigrations.clear();
    this.finishedMigrations.putAll(finishedMigrations);
  }

  public void clearUnknownMigrations() {
    this.finishedMigrations.keySet().removeIf((id) -> !this.migrations.containsKey(id));
  }

  public void processSummary(
      List<PackData> packs, UUID serverId, String combinedImageHash, Map<Identifier, Boolean> finishedMigrations
  ) {
    boolean initialLoad = this.packsMap.isEmpty();

    this.setPacks(packs);
    this.setFinishedMigrations(finishedMigrations);
    this.checkAndPromptForLegacyPacks();
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
              .collect(Collectors.toCollection(HashSet::new));
          legacyPacks.removeAll(alreadyLoaded);

          if (!legacyPacks.isEmpty()) {
            Text ignoreLink = Text.translatable("custompaintings.legacy.prompt.ignore")
                .styled((style) -> style.withColor(Formatting.BLUE)
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, CustomPaintingsMod.MSG_CMD_IGNORE)));
            Text openScreenLink = Text.translatable("custompaintings.legacy.prompt.openConvertScreen")
                .styled((style) -> style.withColor(Formatting.BLUE)
                    .withUnderline(true)
                    .withClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, CustomPaintingsMod.MSG_CMD_OPEN_CONVERT_SCREEN)));

            this.client.player.sendMessage(
                Text.translatable("custompaintings.legacy.prompt", legacyPacks.size(), ignoreLink, openScreenLink));
          }
        }, this.client);
  }

  private void initCacheAndSpriteAtlas(boolean initialLoad, UUID serverId, String serverCombinedImageHash) {
    if (this.hasAllImages(serverCombinedImageHash, this.images)) {
      CustomPaintingsMod.LOGGER.info("All image info still valid, skipping re-fetching images");
      this.buildSpriteAtlas();
      return;
    }

    // TODO: Do I need to clear all these here?
    this.images.clear();
    this.imageHashes.clear();
    this.cachedImages.clear();
    this.cachedImageHashes.clear();
    this.cacheDirty = false;

    this.combinedImageHash = serverCombinedImageHash;

    if (!this.usingCache()) {
      CustomPaintingsMod.LOGGER.info("Not using cache, requesting all images from server");
      ClientNetworking.sendHashesPacket(this.imageHashes);
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
    } else if (this.hasAllImages(cacheRead.combinedHash(), cacheRead.images())) {
      CustomPaintingsMod.LOGGER.info("All images successfully pulled from cache; skipping server image download");
      this.images.putAll(cacheRead.images());
      this.imageHashes.putAll(cacheRead.hashes());
    } else {
      CustomPaintingsMod.LOGGER.info("Requesting images from server");
      this.cacheDirty = true;
      this.cachedImages.putAll(cacheRead.images());
      this.cachedImageHashes.putAll(cacheRead.hashes());
      this.cachedImageHashes.entrySet().removeIf((entry) -> !this.cachedImages.containsKey(entry.getKey()));
      ClientNetworking.sendHashesPacket(this.cachedImageHashes);
    }

    this.buildSpriteAtlas();
  }

  private boolean hasAllImages(String newCombinedHash, Map<Identifier, Image> images) {
    if (!Objects.equals(newCombinedHash, this.combinedImageHash)) {
      return false;
    }

    HashSet<Identifier> neededIds = ResourceUtil.getAllImageIds(this.packsMap.keySet(), this.paintings.keySet());
    return neededIds.stream().allMatch(images::containsKey);
  }

  public void trackExpectedPackets(List<Identifier> ids, int imageCount, int byteCount) {
    HashSet<Identifier> neededIds = new HashSet<>();
    neededIds.addAll(this.packsMap.keySet().stream().map(PackIcons::identifier).toList());
    neededIds.addAll(this.paintings.keySet());
    neededIds.forEach((id) -> {
      if (ids.contains(id)) {
        this.cachedImages.remove(id);
        this.cachedImageHashes.remove(id);
        return;
      }
      Image cachedImage = this.cachedImages.get(id);
      if (cachedImage == null) {
        return;
      }
      this.images.put(id, cachedImage);
      this.imageHashes.put(id, this.cachedImageHashes.get(id));
    });

    CustomPaintingsMod.LOGGER.info("Expecting {} painting image(s) from server", ids.size());
    this.waitingForImagesTimer = Util.getMeasuringTimeMs();

    this.neededImages.clear();
    this.neededImages.addAll(ids);
    this.imagesExpected = imageCount;
    this.bytesExpected = byteCount;

    if (this.client.player != null && !this.client.isInSingleplayer()) {
      this.lastDownloadUpdate = System.currentTimeMillis();
      this.sendMessage(
          Text.translatable("custompaintings.download.start", this.imagesExpected, formatBytes(this.bytesExpected)));
    }

    this.buildSpriteAtlas();
  }

  public void setPaintingImage(Identifier id, Image image) {
    this.imagesReceived++;
    this.bytesReceived += image.getSize();
    this.setFull(id, image);
  }

  public void setPaintingHeader(Identifier id, int width, int height, int totalChunks) {
    this.setPart(id, (builder) -> builder.set(width, height, totalChunks));
  }

  public void setPaintingChunk(Identifier id, int index, byte[] bytes) {
    this.bytesReceived += bytes.length;
    this.setPart(id, (builder) -> builder.set(index, bytes));
  }

  public Progress getByteProgress() {
    return new Progress(this.bytesReceived, this.bytesExpected);
  }

  public CompletableFuture<PaintingData> safeGet(Identifier id) {
    if (this.packsReceived) {
      return CompletableFuture.completedFuture(this.get(id));
    }

    CompletableFuture<PaintingData> future = new CompletableFuture<>();
    this.pendingDataRequests.put(id, future);
    return future;
  }

  @Override
  protected void onPacksChanged() {
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
  public void setImages(HashMap<Identifier, Image> images) {
    // TODO: Should I decouple the two registries?
    CustomPaintingsMod.LOGGER.warn("Unexpected client-side setImages call. Was this intentional?");
    super.setImages(images);
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
    this.cachedImageHashes.clear();
    this.imageBuilders.clear();
    this.pendingDataRequests.forEach((id, future) -> future.cancel(true));
    this.pendingDataRequests.clear();
    this.finishedMigrations.clear();
    this.imagesExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.bytesReceived = 0;
  }

  private void tick(MinecraftClient client) {
    if (client.isInSingleplayer()) {
      return;
    }

    if (this.imagesExpected == 0 || this.imagesReceived == this.imagesExpected) {
      this.imagesExpected = 0;
      this.bytesExpected = 0;
      this.imagesReceived = 0;
      this.bytesReceived = 0;
      return;
    }

    long timestamp = System.currentTimeMillis();
    if (timestamp - this.lastDownloadUpdate > 4000) {
      this.lastDownloadUpdate = timestamp;
      this.sendMessage(client,
          Text.translatable("custompaintings.download.progress", this.imagesReceived, this.imagesExpected,
              this.getByteProgress().percent()
          )
      );
    }
  }

  private void close(MinecraftClient client) {
    this.clear();
  }

  private void setPart(Identifier id, Function<ImageChunkBuilder, Boolean> setter) {
    ImageChunkBuilder builder = this.imageBuilders.computeIfAbsent(id, (identifier) -> new ImageChunkBuilder());
    if (setter.apply(builder)) {
      this.imagesReceived++;
      this.setFull(id, builder.generate());
      this.imageBuilders.remove(id);
    }
  }

  private void setFull(Identifier id, Image image) {
    try {
      this.images.put(id, image);
      this.imageHashes.put(id, image.getHash());
    } catch (IOException e) {
      // TODO: Handle exception
      throw new RuntimeException(e);
    }

    this.cacheDirty = true;
    this.neededImages.remove(id);

    if (!this.neededImages.isEmpty()) {
      return;
    }

    // TODO: Rereate the combined hash to validate all the images.

    CustomPaintingsMod.LOGGER.info("All painting images received from server. Refreshing sprite atlas...");
    this.buildSpriteAtlas();
    this.saveBackToCache();
    String time = formatToTwoDecimals((Util.getMeasuringTimeMs() - this.waitingForImagesTimer) / 1000.0);
    CustomPaintingsMod.LOGGER.info("Painting images downloaded and sprite atlas refreshed in {}s", time);

    if (!this.client.isInSingleplayer()) {
      this.sendMessage(Text.translatable("custompaintings.download.done", this.imagesExpected, time));
    }

    this.imagesExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.bytesReceived = 0;
  }

  private void buildSpriteAtlas() {
    this.images.entrySet().removeIf((entry) -> !this.isValidImageId(entry.getKey()));
    this.imageHashes.entrySet().removeIf((entry) -> !this.isValidImageId(entry.getKey()));

    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BasicTextureSprite.fetch(this.client, PAINTING_BACK_ID, BACK_TEXTURE_ID));
    sprites.add(VanillaIconSprite.create(this.client, PackIcons.MINECRAFT_ICON_ID, "vanilla"));
    sprites.add(BasicTextureSprite.fetch(this.client, PackIcons.MINECRAFT_HIDDEN_ICON_ID, EARTH_TEXTURE_ID));
    this.paintings.values().forEach((painting) -> this.getSpriteContents(painting).ifPresent(sprites::add));
    this.packsMap.keySet().forEach((packId) -> this.getSpriteContents(packId).ifPresent(sprites::add));

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));

    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).toList());

    this.atlasInitialized = true;
  }

  private void saveBackToCache() {
    if (!this.usingCache() || !this.neededImages.isEmpty() || !this.cacheDirty) {
      return;
    }

    final ImmutableMap<Identifier, Image> images = ImmutableMap.copyOf(this.images);
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
  private boolean isValidImageId(Identifier id) {
    return this.paintings.containsKey(id) ||
           (id.getNamespace().equals(PackIcons.ICON_NAMESPACE) && this.packsMap.containsKey(id.getPath()));
  }

  private Optional<SpriteContents> getSpriteContents(PaintingData painting) {
    return this.getSpriteContents(
        painting.id(), this.images.get(painting.id()), painting.getScaledWidth(), painting.getScaledHeight());
  }

  private Optional<SpriteContents> getSpriteContents(String packId) {
    Identifier id = PackIcons.identifier(packId);
    return this.getSpriteContents(id, this.images.get(id), 16, 16);
  }

  private Optional<SpriteContents> getSpriteContents(Identifier id, Image image, int width, int height) {
    if (image == null || image.isEmpty()) {
      if (this.neededImages.contains(id)) {
        return Optional.of(LoadingSprite.generate(id, width, height));
      }
      return Optional.empty();
    }
    NativeImage nativeImage = getNativeImage(image);
    return Optional.of(new SpriteContents(id, new SpriteDimensions(image.width(), image.height()), nativeImage,
        getResourceMetadata(image)
    ));
  }

  private void sendMessage(Text text) {
    this.sendMessage(this.client, text);
  }

  private void sendMessage(MinecraftClient client, Text text) {
    if (client.player == null) {
      return;
    }
    client.player.sendMessage(text);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean usingCache() {
    return CustomPaintingsConfig.getInstance().cacheImages.getValue();
  }

  private static NativeImage getNativeImage(Image image) {
    NativeImage nativeImage = new NativeImage(image.width(), image.height(), false);
    for (int x = 0; x < image.width(); x++) {
      for (int y = 0; y < image.height(); y++) {
        nativeImage.setColor(x, y, image.getABGR(x, y));
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

  private static String formatBytes(int bytes) {
    if (bytes >= 512000) {
      return String.format("%s MB", formatToTwoDecimals(bytes / 1024.0 / 1024.0));
    } else if (bytes >= 512) {
      return String.format("%s KB", formatToTwoDecimals(bytes / 1024.0));
    }
    return formatToTwoDecimals(bytes / 1024.0 / 1024.0) + " B";
  }

  private static String formatToTwoDecimals(double value) {
    return new DecimalFormat("0.##").format(value);
  }

  public record Progress(int received, int expected, int percent) {
    public Progress(int received, int expected) {
      this(received, expected, Math.clamp(Math.round(100f * (float) received / expected), 0, 100));
    }
  }
}
