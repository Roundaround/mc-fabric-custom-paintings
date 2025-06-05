package me.roundaround.custompaintings.resource.legacy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.texture.LoadingSprite;
import me.roundaround.custompaintings.resource.MigrationResource;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.PaintingResource;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.file.Pack;
import me.roundaround.custompaintings.resource.file.PackReader;
import me.roundaround.custompaintings.roundalib.util.PathAccessor;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class LegacyPackConverter {
  private static final String CUSTOMPAINTINGS_JSON = "custompaintings.json";
  private static final String ICON_PNG = "icon.png";

  private static LegacyPackConverter instance = null;

  private final HashSet<CustomId> spriteIds = new HashSet<>();
  private final Executor ioExecutor = Util.getIoWorkerExecutor();

  private SpriteAtlasTexture atlas = null;
  private Path globalOutDir = null;

  private LegacyPackConverter() {
  }

  public static LegacyPackConverter getInstance() {
    if (instance == null) {
      instance = new LegacyPackConverter();
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
    return this.getSprite(PackIcons.customId(packId));
  }

  public Sprite getSprite(CustomId id) {
    if (this.atlas == null) {
      return null;
    }
    if (!this.spriteIds.contains(id)) {
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id.toIdentifier());
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
          .resolve("converted");
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

  public CompletableFuture<Collection<Metadata>> checkForLegacyPacks(MinecraftClient client) {
    Path resourcePackDir = client.getResourcePackDir();
    return CompletableFuture.supplyAsync(() -> this.checkForLegacyPackMetadata(resourcePackDir), this.ioExecutor);
  }

  public CompletableFuture<LegacyPackCheckResult> checkForLegacyPacksAndConvertedIds(MinecraftClient client) {
    Path resourcePackDir = client.getResourcePackDir();
    boolean isSinglePlayer = client.isInSingleplayer();

    return CompletableFuture.supplyAsync(
        () -> this.loadAllDataFromFiles(resourcePackDir, isSinglePlayer), this.ioExecutor).thenApplyAsync((result) -> {
          this.uploadIconsSpriteAtlas(client, result.metas());
          return result;
        }, client);
  }

  private LegacyPackCheckResult loadAllDataFromFiles(Path resourcePackDir, boolean isSinglePlayer) {
    Collection<Metadata> metas = this.checkForLegacyPackMetadata(resourcePackDir);
    HashMap<String, Path> globalConvertedIds = this.lookUpConvertedPacks(this.getGlobalOutDir());
    HashMap<String, Path> worldConvertedIds = isSinglePlayer ? this.lookUpConvertedPacks(this.getWorldOutDir())
        : new HashMap<>();
    return new LegacyPackCheckResult(metas, globalConvertedIds, worldConvertedIds);
  }

  private ArrayList<Metadata> checkForLegacyPackMetadata(Path resourcePackDir) {
    ArrayList<Metadata> metas = new ArrayList<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resourcePackDir)) {
      directoryStream.forEach((path) -> {
        Metadata metadata = PackReader.readMetadata(path);
        if (metadata == null) {
          return;
        }
        metas.add(metadata);
      });
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn(
          "Error while checking for legacy packs in the resource pack directory...exiting early.");
    }

    return metas;
  }

  private HashMap<String, Path> lookUpConvertedPacks(Path directory) {
    HashMap<String, Path> map = new HashMap<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
      directoryStream.forEach((path) -> {
        try {
          BasicFileAttributes fileAttributes = Files.readAttributes(
              path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

          String packFileUid = null;
          if (fileAttributes.isDirectory()) {
            packFileUid = this.readPackFileUidFromDirectory(path);
          } else if (fileAttributes.isRegularFile()) {
            packFileUid = this.readPackFileUidFromZip(path);
          }

          if (packFileUid != null) {
            map.put(packFileUid, path);
          }
        } catch (IOException ignored) {
        }
      });
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error while looking up list of already-converted packs...exiting early.");
    }

    return map;
  }

  private String readPackFileUidFromDirectory(Path path) {
    SourceLegacyPackWrapper parsed;
    try {
      parsed = CustomPaintingsMod.GSON.fromJson(
          Files.newBufferedReader(path.resolve(CUSTOMPAINTINGS_JSON)), SourceLegacyPackWrapper.class);
    } catch (Exception e) {
      return null;
    }
    return parsed.sourceLegacyPack();
  }

  private String readPackFileUidFromZip(Path path) {
    try (ZipFile zip = new ZipFile(path.toFile())) {
      ZipEntry jsonEntry = zip.getEntry(CUSTOMPAINTINGS_JSON);
      if (jsonEntry == null) {
        return null;
      }

      SourceLegacyPackWrapper parsed;
      try (InputStream stream = zip.getInputStream(jsonEntry)) {
        parsed = CustomPaintingsMod.GSON.fromJson(new InputStreamReader(stream), SourceLegacyPackWrapper.class);
      } catch (Exception ignored) {
        return null;
      }

      return parsed.sourceLegacyPack();
    } catch (IOException e) {
      return null;
    }
  }

  private void uploadIconsSpriteAtlas(
      MinecraftClient client, Collection<Metadata> metas) {
    this.atlas = new SpriteAtlasTexture(
        Identifier.of(CustomPaintingsMod.MOD_ID, "textures/atlas/legacy_pack_icons.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    List<SpriteContents> spriteContents = new ArrayList<>();
    spriteContents.add(MissingSprite.createSpriteContents());
    metas.forEach((meta) -> {
      if (meta.icon() == null) {
        return;
      }
      CustomId id = PackIcons.customId(meta.pack().id());
      this.spriteIds.add(id);
      spriteContents.add(getIconSpriteContents(id.toIdentifier(), meta.icon()));
    });
    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(spriteContents, 0, Util.getMainWorkerExecutor()));
  }

  public CompletableFuture<Boolean> convertPack(Metadata meta, Path path) {
    return CompletableFuture.supplyAsync(() -> {
      Pack legacyPack = meta.pack();

      HashMap<CustomId, Image> images = new HashMap<>();
      if (meta.icon() != null) {
        images.put(PackIcons.customId(legacyPack.id()), meta.icon());
      }
      images.putAll(PackReader.readPaintingImages(meta));

      HashMap<String, PaintingResource> paintings = new HashMap<>();
      HashMap<String, MigrationResource> migrations = new HashMap<>();

      legacyPack.paintings().forEach((legacyPainting) -> {
        paintings.put(legacyPainting.id(),
            new PaintingResource(legacyPainting.id(), legacyPainting.name(), legacyPainting.artist(),
                legacyPainting.height(), legacyPainting.width()));
      });
      paintings.keySet().removeIf(paintingId -> !images.containsKey(new CustomId(legacyPack.id(), paintingId)));

      legacyPack.migrations().forEach((legacyMigration) -> {
        List<List<String>> pairs = legacyMigration.pairs().stream().map(List::copyOf).toList();
        migrations.put(
            legacyMigration.id(), new MigrationResource(legacyMigration.id(), legacyMigration.description(), pairs));
      });

      PackResource pack = new PackResource(1, legacyPack.id(), legacyPack.name(), legacyPack.description(),
          meta.fileUid().stringValue(), List.copyOf(paintings.values()), List.copyOf(migrations.values()));

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
          FileOutputStream fos = new FileOutputStream(path.toFile());
          ZipOutputStream zos = new ZipOutputStream(fos)) {
        writeCustomPaintingsJson(zos, pack);

        CustomId iconId = PackIcons.customId(pack.id());
        if (images.containsKey(iconId)) {
          writeImage(zos, ICON_PNG, images.get(iconId));
        }

        for (PaintingResource painting : pack.paintings()) {
          CustomId paintingId = new CustomId(pack.id(), painting.id());
          if (images.containsKey(paintingId)) {
            writeImage(zos, Paths.get("images", painting.id() + ".png").toString(), images.get(paintingId));
          }
        }
      } catch (IOException e) {
        CustomPaintingsMod.LOGGER.warn(
            "Failed to automatically convert legacy painting pack {}", meta.fileUid().filename());
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
        ResourceMetadata.NONE);
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

  private static void writeCustomPaintingsJson(ZipOutputStream zos, PackResource pack) throws IOException {
    ZipEntry entry = new ZipEntry(CUSTOMPAINTINGS_JSON);
    zos.putNextEntry(entry);

    String content = CustomPaintingsMod.GSON.toJson(pack);
    byte[] bytes = content.getBytes();
    zos.write(bytes, 0, bytes.length);
    zos.closeEntry();
  }

  private static void writeImage(ZipOutputStream zos, String path, Image image) throws IOException {
    ZipEntry entry = new ZipEntry(path);
    zos.putNextEntry(entry);
    ImageIO.write(image.toBufferedImage(), "png", zos);
  }

  private record SourceLegacyPackWrapper(String sourceLegacyPack) {
  }
}
