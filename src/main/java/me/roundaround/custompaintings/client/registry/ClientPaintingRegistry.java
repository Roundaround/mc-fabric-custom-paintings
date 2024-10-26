package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.texture.BasicTextureSprite;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.client.texture.VanillaIconSprite;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ClientPaintingRegistry extends CustomPaintingRegistry implements AutoCloseable {
  private static final Identifier PAINTING_BACK_ID = new Identifier(Identifier.DEFAULT_NAMESPACE, "back");
  private static final Identifier BACK_TEXTURE_ID = new Identifier(
      Identifier.DEFAULT_NAMESPACE, "textures/painting/back.png");
  private static final Identifier EARTH_TEXTURE_ID = new Identifier(
      Identifier.DEFAULT_NAMESPACE, "textures/painting/earth.png");

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<Identifier> spriteIds = new HashSet<>();
  private final HashSet<Identifier> neededImages = new HashSet<>();
  private final HashMap<Identifier, Image> cachedImages = new HashMap<>();
  private final HashMap<Identifier, ImageChunkBuilder> imageBuilders = new HashMap<>();
  private final LinkedHashMap<Identifier, CompletableFuture<PaintingData>> pendingDataRequests = new LinkedHashMap<>();

  private boolean packsReceived = false;
  private String pendingCombinedImagesHash = "";
  private long waitingForImagesTimer;
  private int imagesExpected;
  private int packetsExpected;
  private int bytesExpected;
  private int imagesReceived;
  private int packetsReceived;
  private int bytesReceived;
  private long lastDownloadUpdate = 0L;

  private ClientPaintingRegistry(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(new Identifier(CustomPaintingsMod.MOD_ID, "textures/atlas/paintings.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);
    this.buildSpriteAtlas();

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
    return this.atlas.getSprite(MissingSprite.getMissingSpriteId());
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
    if (data.isVanilla()) {
      return this.client.getPaintingManager().getPaintingSprite(Registries.PAINTING_VARIANT.get(data.id()));
    }
    return this.getSprite(data.id());
  }

  public Map<String, PaintingPack> getPacks() {
    return Map.copyOf(this.packsMap);
  }

  public void processSummary(List<PaintingPack> packsList, UUID serverId, String combinedImageHash) {
    boolean first = this.packsMap.isEmpty();

    HashMap<String, PaintingPack> packs = new HashMap<>(packsList.size());
    packsList.forEach((pack) -> packs.put(pack.id(), pack));
    this.setPacks(packs);

    if (combinedImageHash.equals(this.combinedImageHash)) {
      CustomPaintingsMod.LOGGER.info("Loaded painting hash matches, skipping server image download");
      return;
    }

    this.cachedImages.clear();
    this.images.clear();
    this.imageHashes.clear();

    if (first) {
      this.postCacheRead(this.readCache(serverId), combinedImageHash);
      return;
    }

    CompletableFuture.supplyAsync(() -> this.readCache(serverId), Util.getIoWorkerExecutor())
        .thenAcceptAsync((cacheRead) -> this.postCacheRead(cacheRead, combinedImageHash), this.client);
  }

  private CacheRead readCache(UUID serverId) {
    if (!this.usingCache()) {
      return null;
    }

    HashSet<String> packIds = new HashSet<>(this.packsMap.keySet());
    HashSet<Identifier> paintingIds = new HashSet<>(this.paintings.keySet());
    for (String packId : packIds) {
      paintingIds.add(PackIcons.identifier(packId));
    }
    packIds.add(PackIcons.ICON_NAMESPACE);

    return CacheManager.getInstance().loadFromFile(serverId, packIds, paintingIds);
  }

  private void postCacheRead(CacheRead cacheRead, String newCombinedImageHash) {
    if (cacheRead != null) {
      this.cachedImages.putAll(cacheRead.images());
      this.imageHashes.putAll(cacheRead.hashes());
      this.combinedImageHash = cacheRead.combinedHash();
    }

    if (Objects.equals(this.combinedImageHash, newCombinedImageHash) ||
        Objects.equals(this.pendingCombinedImagesHash, newCombinedImageHash)) {
      CustomPaintingsMod.LOGGER.info("Cached painting hash matches, skipping server image download");
    } else {
      CustomPaintingsMod.LOGGER.info("Requesting painting images from server");
      ClientNetworking.sendHashesPacket(this.imageHashes);
      this.pendingCombinedImagesHash = newCombinedImageHash;
    }

    this.images.putAll(this.cachedImages);
    this.onImagesChanged();
  }

  public void trackExpectedPackets(List<Identifier> ids, int imageCount, int packetCount, int byteCount) {
    this.cachedImages.entrySet().removeIf((entry) -> ids.contains(entry.getKey()));
    if (!this.cachedImages.isEmpty()) {
      CustomPaintingsMod.LOGGER.info("{} painting hashes match, using cached data", this.cachedImages.size());
      this.images.putAll(this.cachedImages);
    }

    CustomPaintingsMod.LOGGER.info("Expecting {} painting image(s) from server", ids.size());
    this.waitingForImagesTimer = Util.getMeasuringTimeMs();

    this.neededImages.clear();
    this.neededImages.addAll(ids);
    this.imagesExpected = imageCount;
    this.packetsExpected = packetCount;
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
    this.packetsReceived++;
    this.bytesReceived += image.getSize();
    this.setFull(id, image);
  }

  public void setPaintingHeader(Identifier id, int width, int height, int totalChunks) {
    this.packetsReceived++;
    this.setPart(id, (builder) -> builder.set(width, height, totalChunks));
  }

  public void setPaintingChunk(Identifier id, int index, byte[] bytes) {
    this.packetsReceived++;
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
  }

  @Override
  protected void onImagesChanged() {
    this.buildSpriteAtlas();

    if (this.usingCache()) {
      if (!"".equals(this.pendingCombinedImagesHash)) {
        this.combinedImageHash = this.pendingCombinedImagesHash;
        this.pendingCombinedImagesHash = "";
      }

      if (!this.neededImages.isEmpty()) {
        return;
      }

      Util.getIoWorkerExecutor().execute(() -> {
        try {
          CacheManager.getInstance().saveToFile(this.images, this.combinedImageHash);
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(e);
          CustomPaintingsMod.LOGGER.warn("Failed to write images and metadata to cache.");
        }
      });
    }
  }

  @Override
  public void close() {
    super.close();

    this.packsReceived = false;
    this.atlas.clear();
    this.spriteIds.clear();
    this.neededImages.clear();
    this.cachedImages.clear();
    this.imageBuilders.clear();
    this.pendingDataRequests.forEach((id, future) -> future.cancel(true));
    this.pendingDataRequests.clear();
    this.pendingCombinedImagesHash = "";
    this.imagesExpected = 0;
    this.packetsExpected = 0;
    this.bytesExpected = 0;
    this.imagesReceived = 0;
    this.packetsReceived = 0;
    this.bytesReceived = 0;
  }

  private void tick(MinecraftClient client) {
    if (client.isInSingleplayer()) {
      return;
    }

    if (this.imagesExpected == 0 || this.imagesReceived == this.imagesExpected) {
      this.imagesExpected = 0;
      this.packetsExpected = 0;
      this.bytesExpected = 0;
      this.imagesReceived = 0;
      this.packetsReceived = 0;
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
    this.close();
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
      throw new RuntimeException(e);
    }

    this.neededImages.remove(id);
    if (this.neededImages.isEmpty()) {
      CustomPaintingsMod.LOGGER.info("All painting images received from server. Refreshing sprite atlas...");
      this.onImagesChanged();
      String time = formatToTwoDecimals((Util.getMeasuringTimeMs() - this.waitingForImagesTimer) / 1000.0);
      CustomPaintingsMod.LOGGER.info("Painting images downloaded and sprite atlas refreshed in {}s", time);

      if (!this.client.isInSingleplayer()) {
        this.sendMessage(Text.translatable("custompaintings.download.done", this.imagesExpected, time));
      }

      this.imagesExpected = 0;
      this.packetsExpected = 0;
      this.bytesExpected = 0;
      this.imagesReceived = 0;
      this.packetsReceived = 0;
      this.bytesReceived = 0;
    }
  }

  void buildSpriteAtlas() {
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
