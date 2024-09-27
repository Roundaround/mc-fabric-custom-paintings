package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.texture.BackSprite;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;

public class ClientPaintingRegistry extends CustomPaintingRegistry implements AutoCloseable {
  private static final Identifier PAINTING_BACK_ID = new Identifier(Identifier.DEFAULT_NAMESPACE, "back");
  private static final Identifier BACK_TEXTURE_ID = new Identifier(
      Identifier.DEFAULT_NAMESPACE, "textures/painting/back.png");

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<Identifier> spriteIds = new HashSet<>();
  private final HashSet<Identifier> neededImages = new HashSet<>();
  private final HashMap<Identifier, Image> cachedImages = new HashMap<>();

  private boolean packsReceived = false;
  private String pendingCombinedImagesHash = "";
  private long waitingForImagesTimer;

  private ClientPaintingRegistry(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(new Identifier(CustomPaintingsMod.MOD_ID, "textures/atlas/paintings.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);
    this.buildSpriteAtlas();

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

  public boolean hasReceivedPacks() {
    return this.packsReceived;
  }

  @Override
  public void setPacks(HashMap<String, PaintingPack> packsMap) {
    this.packsReceived = true;
    super.setPacks(packsMap);
  }

  public Map<String, PaintingPack> getPacks() {
    return Map.copyOf(this.packsMap);
  }

  public void checkCombinedImageHash(String combinedImageHash) {
    if (!CustomPaintingsConfig.getInstance().cacheImages.getValue()) {
      CustomPaintingsMod.LOGGER.info("Caching disabled; requesting all painting images from server");
      ClientNetworking.sendHashesPacket(new HashMap<>(0));
      return;
    }

    if (!Objects.equals(this.combinedImageHash, combinedImageHash) &&
        !Objects.equals(this.pendingCombinedImagesHash, combinedImageHash)) {
      CustomPaintingsMod.LOGGER.info("Requesting painting images from server");
      ClientNetworking.sendHashesPacket(this.imageHashes);
      this.pendingCombinedImagesHash = combinedImageHash;
      return;
    }

    CustomPaintingsMod.LOGGER.info("Combined painting hash matches, skipping server image download");
    this.images.clear();
    this.images.putAll(this.cachedImages);
    this.onImagesChanged();
  }

  public void trackNeededImages(List<Identifier> ids) {
    this.cachedImages.entrySet().removeIf((entry) -> ids.contains(entry.getKey()));
    if (!this.cachedImages.isEmpty()) {
      CustomPaintingsMod.LOGGER.info("{} painting hashes match, using cached data", this.cachedImages.size());
      this.images.putAll(this.cachedImages);
    }

    CustomPaintingsMod.LOGGER.info("Expecting {} painting image(s) from server", ids.size());
    this.waitingForImagesTimer = Util.getMeasuringTimeMs();

    this.neededImages.clear();
    this.neededImages.addAll(ids);

    this.buildSpriteAtlas();
  }

  public void setPaintingImage(Identifier id, Image image) {
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
      DecimalFormat format = new DecimalFormat("0.##");
      CustomPaintingsMod.LOGGER.info("Painting images downloaded and sprite atlas refreshed in {}s",
          format.format((Util.getMeasuringTimeMs() - this.waitingForImagesTimer) / 1000.0)
      );
    }
  }

  protected void close(MinecraftClient client) {
    this.close();
  }

  @Override
  public void close() {
    super.close();

    this.atlas.clear();
    this.spriteIds.clear();
    this.neededImages.clear();
    this.cachedImages.clear();
    this.pendingCombinedImagesHash = "";
  }

  @Override
  protected void onPacksChanged() {
    CustomPaintingsMod.LOGGER.info("{} painting metadata entries loaded", this.paintings.size());

    if (!CustomPaintingsConfig.getInstance().cacheImages.getValue()) {
      this.cachedImages.clear();
      this.imageHashes.clear();
      this.combinedImageHash = "";

      CustomPaintingsMod.LOGGER.info("Painting image caching disabled, skipping hash generation and cache loading");
      return;
    }

    this.cachedImages.clear();

    Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("data").resolve(CustomPaintingsMod.MOD_ID);
    if (Files.notExists(cacheDir)) {
      CustomPaintingsMod.LOGGER.info(
          "Painting image cache directory does not exist, skipping hash generation and cache loading");
      return;
    }

    for (Identifier id : this.paintings.keySet()) {
      Path path = cacheDir.resolve(id.getNamespace()).resolve(id.getPath() + ".png");
      if (Files.notExists(path) || !Files.isRegularFile(path)) {
        this.cachedImages.put(id, Image.empty());
        continue;
      }

      try {
        Image image = Image.read(Files.newInputStream(path));
        this.cachedImages.put(id, image);
      } catch (IOException e) {
        this.cachedImages.put(id, Image.empty());
      }
    }

    this.combinedImageHash = "";
    this.imageHashes.clear();

    try {
      HashResult hashResult = hashImages(this.cachedImages);
      this.combinedImageHash = hashResult.combinedImageHash();
      this.imageHashes.putAll(hashResult.imageHashes());
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Painting image cache failed to hash. Ignoring.");
    }
  }

  @Override
  protected void onImagesChanged() {
    this.buildSpriteAtlas();

    if (CustomPaintingsConfig.getInstance().cacheImages.getValue()) {
      if (!"".equals(this.pendingCombinedImagesHash)) {
        this.combinedImageHash = this.pendingCombinedImagesHash;
        this.pendingCombinedImagesHash = "";
      }

      Util.getIoWorkerExecutor().execute(() -> {
        writeImagesToFile(Map.copyOf(this.packsMap), Map.copyOf(this.images));
      });
    }
  }

  protected void buildSpriteAtlas() {
    this.images.entrySet().removeIf((entry) -> !this.paintings.containsKey(entry.getKey()));
    this.imageHashes.entrySet().removeIf((entry) -> !this.paintings.containsKey(entry.getKey()));

    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BackSprite.fetch(this.client, PAINTING_BACK_ID, BACK_TEXTURE_ID));
    this.paintings.values().forEach((painting) -> sprites.add(this.getSpriteContents(painting)));

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));

    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).toList());
  }

  private SpriteContents getSpriteContents(PaintingData painting) {
    Image image = this.images.get(painting.id());
    if (image != null) {
      return getSpriteContents(painting.id(), image);
    }
    return LoadingSprite.generate(painting.id(), painting.getScaledWidth(), painting.getScaledHeight());
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

  private static SpriteContents getSpriteContents(Identifier id, Image image) {
    NativeImage nativeImage = getNativeImage(image);
    return new SpriteContents(id, new SpriteDimensions(image.width(), image.height()), nativeImage,
        getResourceMetadata(image)
    );
  }

  private static ResourceMetadata getResourceMetadata(Image image) {
    return new ResourceMetadata.Builder().add(AnimationResourceMetadata.READER,
            new AnimationResourceMetadata(ImmutableList.of(new AnimationFrameResourceMetadata(0, -1)), image.width(),
                image.height(), 1, false
            )
        )
        .build();
  }

  private static void writeImagesToFile(Map<String, PaintingPack> packsMap, Map<Identifier, Image> images) {
    Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("data").resolve(CustomPaintingsMod.MOD_ID);
    if (Files.notExists(cacheDir)) {
      try {
        Files.createDirectories(cacheDir);
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Could not create cache directory: {}", cacheDir.toAbsolutePath());
        return;
      }
    }

    packsMap.forEach((id, pack) -> {
      Path packDir = cacheDir.resolve(id);
      if (Files.notExists(packDir)) {
        try {
          Files.createDirectories(packDir);
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(
              "Could not create cache subdirectory, skipping pack: {}", packDir.toAbsolutePath());
          return;
        }
      }

      for (PaintingData painting : pack.paintings()) {
        Image image = images.get(painting.id());
        if (image == null || image.isEmpty()) {
          continue;
        }

        String paintingId = painting.id().getPath();
        Path imagePath = packDir.resolve(paintingId + ".png");
        try {
          ImageIO.write(image.toBufferedImage(), "png", imagePath.toFile());
        } catch (IOException e) {
          CustomPaintingsMod.LOGGER.warn(
              "Failed to write {}:{} to file, skipping: {}", id, paintingId, imagePath.toAbsolutePath());
        }
      }
    });
  }
}
