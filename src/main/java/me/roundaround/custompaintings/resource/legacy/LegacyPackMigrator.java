package me.roundaround.custompaintings.resource.legacy;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.PaintingResource;
import me.roundaround.roundalib.util.PathAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class LegacyPackMigrator {
  private static final String CUSTOM_PAINTINGS_JSON = "custompaintings.json";
  private static final String PACK_MCMETA = "pack.mcmeta";
  private static final String PACK_PNG = "pack.png";
  private static final String ICON_PNG = "icon.png";
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static LegacyPackMigrator instance = null;

  private final HashSet<Identifier> spriteIds = new HashSet<>();
  private final Executor ioExecutor = Util.getIoWorkerExecutor();

  private SpriteAtlasTexture atlas = null;
  private Path globalOutDir = null;

  private LegacyPackMigrator() {
  }

  public static LegacyPackMigrator getInstance() {
    if (instance == null) {
      instance = new LegacyPackMigrator();
    }
    return instance;
  }

  public Identifier getAtlasId() {
    if (this.atlas == null) {
      return null;
    }
    return this.atlas.getId();
  }

  public Sprite getMissingSprite() {
    if (this.atlas == null) {
      return null;
    }
    return this.atlas.getSprite(MissingSprite.getMissingSpriteId());
  }

  public Sprite getSprite(String packId) {
    return this.getSprite(PackIcons.identifier(packId));
  }

  public Sprite getSprite(Identifier id) {
    if (this.atlas == null) {
      return null;
    }
    if (!this.spriteIds.contains(id)) {
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id);
  }

  public Path getWorldOutDir() {
    try {
      Path path = PathAccessor.getInstance().getPerWorldModDir(CustomPaintingsMod.MOD_ID);
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      }
      return path;
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Failed to create output directory for legacy pack conversion.");
    }

    return null;
  }

  public Path getGlobalOutDir() {
    if (this.globalOutDir != null) {
      return this.globalOutDir;
    }

    try {
      Path path = FabricLoader.getInstance()
          .getGameDir()
          .resolve("data")
          .resolve(CustomPaintingsMod.MOD_ID)
          .resolve("legacy_converted");
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      }
      this.globalOutDir = path;
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Failed to create output directory for legacy pack conversion.");
    }

    return this.globalOutDir;
  }

  public CompletableFuture<Collection<PackMetadata>> checkForLegacyPacks(MinecraftClient client) {
    Path resourcePackDir = client.getResourcePackDir();
    return CompletableFuture.supplyAsync(() -> this.checkForLegacyPackMetadata(resourcePackDir), this.ioExecutor)
        .thenApplyAsync((metas) -> this.uploadIconsSpriteAtlas(client, metas), client);
  }

  private ArrayList<PackMetadata> checkForLegacyPackMetadata(Path resourcePackDir) {
    ArrayList<PackMetadata> metas = new ArrayList<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resourcePackDir)) {
      directoryStream.forEach((path) -> {
        PackMetadata metadata = readPackMetadata(path);
        if (metadata == null) {
          return;
        }
        metas.add(metadata);
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return metas;
  }

  private Collection<PackMetadata> uploadIconsSpriteAtlas(
      MinecraftClient client, Collection<PackMetadata> metas
  ) {
    this.atlas = new SpriteAtlasTexture(
        new Identifier(CustomPaintingsMod.MOD_ID, "textures/atlas/legacy_pack_icons.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    List<SpriteContents> spriteContents = new ArrayList<>();
    spriteContents.add(MissingSprite.createSpriteContents());
    metas.forEach((meta) -> {
      if (meta.icon() == null) {
        return;
      }
      Identifier id = PackIcons.identifier(meta.pack().packId());
      this.spriteIds.add(id);
      spriteContents.add(getIconSpriteContents(id, meta.icon()));
    });
    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(spriteContents, 0, Util.getMainWorkerExecutor()));

    return metas;
  }

  public CompletableFuture<Boolean> convertPack(LegacyPackResource legacyPack, Path path) {
    return CompletableFuture.supplyAsync(() -> {
      HashMap<Identifier, Image> images = readPaintingImages(legacyPack);
      HashMap<String, PaintingResource> paintings = new HashMap<>();

      legacyPack.paintings().forEach((legacyPainting) -> {
        paintings.put(legacyPainting.id(),
            new PaintingResource(legacyPainting.id(), legacyPainting.name(), legacyPainting.artist(),
                legacyPainting.height(), legacyPainting.width()
            )
        );
      });
      paintings.keySet().removeIf(paintingId -> !images.containsKey(new Identifier(legacyPack.packId(), paintingId)));

      PackResource pack = new PackResource(
          1, legacyPack.packId(), legacyPack.name(), legacyPack.description(), new ArrayList<>(paintings.values()));

      try {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
          Path backup = path.resolveSibling(path.getFileName().toString() + "_old");
          if (Files.exists(backup)) {
            Files.delete(backup);
          }
          Files.move(path, backup);
        }
      } catch (IOException e) {
        return false;
      }

      try (
          FileOutputStream fos = new FileOutputStream(path.toFile()); ZipOutputStream zos = new ZipOutputStream(fos)
      ) {
        writeCustomPaintingsJson(zos, pack);

        Identifier iconId = PackIcons.identifier(pack.id());
        if (images.containsKey(iconId)) {
          writeImage(zos, ICON_PNG, images.get(iconId));
        }

        for (PaintingResource painting : pack.paintings()) {
          Identifier paintingId = new Identifier(pack.id(), painting.id());
          if (images.containsKey(paintingId)) {
            writeImage(zos, Paths.get("images", painting.id() + ".png").toString(), images.get(paintingId));
          }
        }
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(
            "Failed to automatically convert legacy painting pack {}", legacyPack.path().getFileName());
        return false;
      }

      return true;
    }, this.ioExecutor);
  }

  private static SpriteContents getIconSpriteContents(Identifier id, Image image) {
    if (image == null || image.isEmpty()) {
      return LoadingSprite.generate(id, 16, 16);
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

  private static PackMetadata readPackMetadata(Path path) {
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readPackMetadataFromDirectory(path);
      }

      if (fileAttributes.isRegularFile()) {
        return readPackMetadataFromZip(path);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
    }

    return null;
  }

  private static PackMetadata readPackMetadataFromDirectory(Path path) {
    if (!Files.isRegularFile(path.resolve(CUSTOM_PAINTINGS_JSON), LinkOption.NOFOLLOW_LINKS)) {
      return null;
    }

    CustomPaintingsJson json = readCustomPaintingsJson(path.resolve(CUSTOM_PAINTINGS_JSON));
    if (json == null) {
      return null;
    }

    String dirname = path.getFileName().toString();
    long lastModified = 0;
    try {
      lastModified = Files.getLastModifiedTime(path).toMillis();
    } catch (IOException ignored) {
    }

    // TODO: Store in output and use to match up previously migrated packs.
    LegacyPackId instanceId = new LegacyPackId(false, dirname, lastModified);

    PackMcmeta meta = readPackMcmeta(path.resolve(PACK_MCMETA));

    String packId = json.id();
    String name = json.name();
    List<LegacyPaintingResource> paintings = json.paintings();
    List<LegacyMigrationResource> migrations = json.migrations();
    String description = meta == null ? "" : meta.pack().description();

    LegacyPackResource pack = new LegacyPackResource(path, packId, name, description, paintings, migrations);
    Image packIcon = readImage(path.resolve(PACK_PNG));

    return new PackMetadata(pack, packIcon);
  }

  private static PackMetadata readPackMetadataFromZip(Path path) {
    String filename = path.getFileName().toString();
    if (!filename.endsWith(".zip")) {
      return null;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      return null;
    }

    long lastModified = 0;
    try {
      lastModified = Files.getLastModifiedTime(path).toMillis();
    } catch (IOException ignored) {
    }

    try (ZipFile zip = new ZipFile(path.toFile())) {
      String folderPrefix = getFolderPrefix(zip);

      CustomPaintingsJson json = readCustomPaintingsJson(zip, folderPrefix + CUSTOM_PAINTINGS_JSON);
      if (json == null) {
        return null;
      }

      // TODO: Store in output and use to match up previously migrated packs.
      LegacyPackId instanceId = new LegacyPackId(true, filename, lastModified);

      PackMcmeta meta = readPackMcmeta(zip, folderPrefix + PACK_MCMETA);

      String packId = json.id();
      String name = json.name();
      List<LegacyPaintingResource> paintings = json.paintings();
      List<LegacyMigrationResource> migrations = json.migrations();
      String description = meta == null ? "" : meta.pack().description();

      LegacyPackResource pack = new LegacyPackResource(path, packId, name, description, paintings, migrations);
      Image packIcon = readImage(zip, folderPrefix + PACK_PNG);

      return new PackMetadata(pack, packIcon);
    } catch (IOException e) {
      return null;
    }
  }

  private static HashMap<Identifier, Image> readPaintingImages(LegacyPackResource pack) {
    try {
      BasicFileAttributes fileAttributes = Files.readAttributes(
          pack.path(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readPaintingImagesFromDirectory(pack);
      }

      if (fileAttributes.isRegularFile()) {
        return readPaintingImagesFromZip(pack);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn(
          "Error reading Custom Paintings pack \"{}\", skipping...", pack.path().getFileName());
    }

    return new HashMap<>();
  }

  private static HashMap<Identifier, Image> readPaintingImagesFromDirectory(LegacyPackResource pack) {
    HashMap<Identifier, Image> images = new HashMap<>();

    Path path = pack.path();
    if (Files.notExists(path)) {
      return images;
    }

    String packId = pack.packId();
    List<LegacyPaintingResource> paintings = pack.paintings();

    Image packIcon = readImage(path.resolve(PACK_PNG));
    if (packIcon != null) {
      images.put(PackIcons.identifier(packId), packIcon);
    }

    for (LegacyPaintingResource painting : paintings) {
      Image image = readImage(path.resolve(getPaintingPath(packId, painting.id())));
      if (image != null) {
        images.put(new Identifier(packId, painting.id()), image);
      }
    }

    return images;
  }

  private static HashMap<Identifier, Image> readPaintingImagesFromZip(LegacyPackResource pack) {
    HashMap<Identifier, Image> images = new HashMap<>();

    Path path = pack.path();
    String filename = path.getFileName().toString();
    if (!filename.endsWith(".zip")) {
      return images;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      return images;
    }

    try (ZipFile zip = new ZipFile(path.toFile())) {
      String folderPrefix = getFolderPrefix(zip);
      String packId = pack.packId();
      List<LegacyPaintingResource> paintings = pack.paintings();

      Image packIcon = readImage(zip, folderPrefix + PACK_PNG);
      if (packIcon != null) {
        images.put(PackIcons.identifier(packId), packIcon);
      }

      for (LegacyPaintingResource painting : paintings) {
        Image image = readImage(zip, folderPrefix + getPaintingPath(packId, painting.id()));
        if (image != null) {
          images.put(new Identifier(packId, painting.id()), image);
        }
      }
    } catch (IOException ignored) {
    }

    return images;
  }

  private static String getFolderPrefix(ZipFile zip) {
    Enumeration<? extends ZipEntry> entries = zip.entries();
    if (!entries.hasMoreElements()) {
      return "";
    }

    ZipEntry firstEntry = entries.nextElement();
    if (!firstEntry.isDirectory()) {
      return "";
    }

    String folderPrefix = firstEntry.getName();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (!entry.getName().startsWith(folderPrefix)) {
        return "";
      }
    }

    return folderPrefix;
  }

  private static CustomPaintingsJson readCustomPaintingsJson(Path path) {
    CustomPaintingsJson json;
    try {
      json = GSON.fromJson(Files.newBufferedReader(path), CustomPaintingsJson.class);
    } catch (Exception e) {
      return null;
    }

    if (json.paintings().isEmpty()) {
      return null;
    }

    return json;
  }

  private static CustomPaintingsJson readCustomPaintingsJson(ZipFile zip, String path) {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null) {
      return null;
    }

    CustomPaintingsJson json;
    try (InputStream stream = zip.getInputStream(entry)) {
      json = GSON.fromJson(new InputStreamReader(stream), CustomPaintingsJson.class);
    } catch (Exception e) {
      return null;
    }

    if (json.paintings().isEmpty()) {
      return null;
    }

    return json;
  }

  private static PackMcmeta readPackMcmeta(Path path) {
    try {
      return GSON.fromJson(Files.newBufferedReader(path), PackMcmeta.class);
    } catch (Exception e) {
      return null;
    }
  }

  private static PackMcmeta readPackMcmeta(ZipFile zip, String path) {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null) {
      return null;
    }

    try (InputStream stream = zip.getInputStream(entry)) {
      return GSON.fromJson(new InputStreamReader(stream), PackMcmeta.class);
    } catch (Exception e) {
      return null;
    }
  }

  private static Image readImage(Path path) {
    try {
      BufferedImage image = ImageIO.read(Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS));
      if (image == null) {
        return null;
      }

      return Image.read(image);
    } catch (IOException e) {
      return null;
    }
  }

  private static Image readImage(ZipFile zip, String path) {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null) {
      return null;
    }

    try (InputStream stream = zip.getInputStream(entry)) {
      BufferedImage image = ImageIO.read(stream);
      if (image == null) {
        return null;
      }

      return Image.read(image);
    } catch (IOException e) {
      return null;
    }
  }

  private static Path getPaintingPath(String packId, String paintingId) {
    return Paths.get("assets", packId, "textures", "painting", paintingId + ".png");
  }

  private static void writeCustomPaintingsJson(ZipOutputStream zos, PackResource pack) throws IOException {
    ZipEntry entry = new ZipEntry(CUSTOM_PAINTINGS_JSON);
    zos.putNextEntry(entry);

    String content = GSON.toJson(pack);
    byte[] bytes = content.getBytes();
    zos.write(bytes, 0, bytes.length);
    zos.closeEntry();
  }

  private static void writeImage(ZipOutputStream zos, String path, Image image) throws IOException {
    ZipEntry entry = new ZipEntry(path);
    zos.putNextEntry(entry);
    ImageIO.write(image.toBufferedImage(), "png", zos);
  }
}
