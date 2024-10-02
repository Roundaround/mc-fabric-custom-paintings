package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ClientPaintingRegistry extends CustomPaintingRegistry implements AutoCloseable {
  private static final Gson GSON = new GsonBuilder().create();
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
  private final HashMap<Identifier, ImageChunkBuilder> cacheImageBuilders = new HashMap<>();

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
    writeNeededIdsToFile(ids);
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

  public void setPaintingHeader(Identifier id, int width, int height, int totalChunks) {
    this.setPart(id, (builder) -> builder.set(width, height, totalChunks));
  }

  public void setPaintingChunk(Identifier id, int index, byte[] bytes) {
    this.setPart(id, (builder) -> builder.set(index, bytes));
  }

  private void setPart(Identifier id, Function<ImageChunkBuilder, Boolean> setter) {
    ImageChunkBuilder builder = this.cacheImageBuilders.computeIfAbsent(id, (identifier) -> new ImageChunkBuilder());
    if (setter.apply(builder)) {
      this.setPaintingImage(id, builder.generate());
      this.cacheImageBuilders.remove(id);
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
    this.cacheImageBuilders.clear();
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

    for (String packId : this.packsMap.keySet()) {
      Identifier id = PackIcons.identifier(packId);
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
    this.images.entrySet().removeIf((entry) -> !this.isValidImageId(entry.getKey()));
    this.imageHashes.entrySet().removeIf((entry) -> !this.isValidImageId(entry.getKey()));

    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(MissingSprite.createSpriteContents());
    sprites.add(BasicTextureSprite.fetch(this.client, PAINTING_BACK_ID, BACK_TEXTURE_ID));
    sprites.add(VanillaIconSprite.create(this.client, PackIcons.MINECRAFT_ICON_ID, "vanilla"));
    sprites.add(BasicTextureSprite.fetch(this.client, PackIcons.MINECRAFT_HIDDEN_ICON_ID, EARTH_TEXTURE_ID));
    this.paintings.values().forEach((painting) -> sprites.add(this.getSpriteContents(painting)));
    // TODO: Stop using PaintingData for this
    this.packsMap.keySet().forEach((packId) -> sprites.add(this.getSpriteContents(PaintingData.packIcon(packId))));

    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));

    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).toList());
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isValidImageId(Identifier id) {
    return this.paintings.containsKey(id) ||
        (id.getNamespace().equals(PackIcons.ICON_NAMESPACE) && this.packsMap.containsKey(id.getPath()));
  }

  private SpriteContents getSpriteContents(PaintingData painting) {
    return getSpriteContents(
        painting.id(), this.images.get(painting.id()), painting.getScaledWidth(), painting.getScaledHeight());
  }

  private static SpriteContents getSpriteContents(Identifier id, Image image, int width, int height) {
    if (image == null || image.isEmpty()) {
      return LoadingSprite.generate(id, width, height);
    }
    NativeImage nativeImage = getNativeImage(image);
    return new SpriteContents(id, new SpriteDimensions(image.width(), image.height()), nativeImage,
        getResourceMetadata(image)
    );
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

  private static Path initCacheDir() {
    Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("data").resolve(CustomPaintingsMod.MOD_ID);
    if (Files.notExists(cacheDir)) {
      try {
        Files.createDirectories(cacheDir);
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn("Could not create cache directory: {}", cacheDir.toAbsolutePath());
        return null;
      }
    }
    return cacheDir;
  }

  private static void writeNeededIdsToFile(List<Identifier> ids) {
    if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
      return;
    }

    Path cacheDir = initCacheDir();
    if (cacheDir == null) {
      return;
    }

    JsonObject json = new JsonObject();
    JsonArray idsArray = new JsonArray();
    for (Identifier id : ids) {
      idsArray.add(id.toString());
    }
    json.add("ids", idsArray);
    String jsonString = GSON.toJson(json);

    Path file = cacheDir.resolve("needed_ids_dump.json");
    try {
      Files.writeString(file, jsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Could not write needed ids dump: {}", file.toAbsolutePath());
    }
  }

  private static void writeImagesToFile(Map<String, PaintingPack> packsMap, Map<Identifier, Image> images) {
    Path cacheDir = initCacheDir();
    if (cacheDir == null) {
      return;
    }

    Path iconsDir = cacheDir.resolve(PackIcons.ICON_NAMESPACE);
    AtomicBoolean cacheIcons = new AtomicBoolean(true);
    if (Files.notExists(iconsDir)) {
      try {
        Files.createDirectories(iconsDir);
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(
            "Could not create cache subdirectory, skipping icons: {}", iconsDir.toAbsolutePath());
        cacheIcons.set(false);
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

      if (cacheIcons.get()) {
        Image iconImage = images.get(PackIcons.identifier(id));
        if (iconImage != null && !iconImage.isEmpty()) {
          Path imagePath = iconsDir.resolve(String.format("%s.png", id));
          try {
            ImageIO.write(iconImage.toBufferedImage(), "png", imagePath.toFile());
          } catch (IOException e) {
            CustomPaintingsMod.LOGGER.warn(
                "Failed to write icon to file for {}, skipping: {}", id, imagePath.toAbsolutePath());
          }
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
