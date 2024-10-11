package me.roundaround.custompaintings.resource.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.ConvertPromptScreen;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.PaintingResource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;

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
import java.util.stream.Collectors;
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

  private LegacyPackMigrator() {
  }

  public static LegacyPackMigrator getInstance() {
    if (instance == null) {
      instance = new LegacyPackMigrator();
    }
    return instance;
  }

  public void checkForLegacyPacks(MinecraftClient client) {
    CompletableFuture<ConvertPromptData> future = CompletableFuture.supplyAsync(() -> {
      // TODO: Actually use ignoredPacks
      // TODO: Consider doing a metadata-only read first, and fully load only after user confirms to convert

      Path ignoredPacksDatFile = FabricLoader.getInstance()
          .getGameDir()
          .resolve("data")
          .resolve(CustomPaintingsMod.MOD_ID)
          .resolve("legacy_ignored.dat");
      HashSet<String> ignoredPacks = readIgnoredPacks(ignoredPacksDatFile);

      HashMap<String, LegacyPackResource> packs = new HashMap<>();
      HashMap<Identifier, Image> images = new HashMap<>();

      Path resourcePackDir = client.getResourcePackDir();
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resourcePackDir)) {
        directoryStream.forEach((path) -> {
          SinglePackResult result = readAsPack(path);
          if (result != null) {
            packs.put(result.pack.packId(), result.pack);
            images.putAll(result.images);
          }
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (packs.isEmpty()) {
        return new ConvertPromptData();
      }

      return new ConvertPromptData(packs, images);
    });
    client.setScreen(new ConvertPromptScreen(client.currentScreen, future));
  }

  public void convertPack(LegacyPackResource legacyPack, HashMap<Identifier, Image> images, Path path) {
    ArrayList<PaintingResource> paintings = new ArrayList<>();
    legacyPack.paintings().forEach((legacyPainting) -> {
      paintings.add(new PaintingResource(legacyPainting.id(), legacyPainting.name(), legacyPainting.artist(),
          legacyPainting.height(), legacyPainting.width()
      ));
    });
    paintings.removeIf((painting) -> !images.containsKey(new Identifier(legacyPack.packId(), painting.id())));
    PackResource pack = new PackResource(
        1, legacyPack.packId(), legacyPack.name(), legacyPack.description(), paintings);

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
    }
  }

  private static HashSet<String> readIgnoredPacks(Path ignoredPacksDatFile) {
    if (Files.notExists(ignoredPacksDatFile)) {
      return new HashSet<>(0);
    }

    try {
      NbtCompound data = NbtIo.readCompressed(ignoredPacksDatFile, NbtSizeTracker.ofUnlimitedBytes());
      NbtList values = data.getList("values", NbtElement.STRING_TYPE);
      return values.stream().map(NbtElement::asString).collect(Collectors.toCollection(HashSet::new));
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Could not read {}", ignoredPacksDatFile.toAbsolutePath(), e);
    }

    return new HashSet<>(0);
  }

  private static SinglePackResult readAsPack(Path path) {
    BasicFileAttributes fileAttributes;
    try {
      fileAttributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      if (fileAttributes.isDirectory()) {
        return readDirectoryAsPack(path);
      }

      if (fileAttributes.isRegularFile()) {
        return readZipAsPack(path);
      }
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn(e);
      CustomPaintingsMod.LOGGER.warn("Error reading Custom Paintings pack \"{}\", skipping...", path.getFileName());
      return null;
    }

    return null;
  }

  private static SinglePackResult readZipAsPack(Path path) {
    String filename = path.getFileName().toString();

    if (!filename.endsWith(".zip")) {
      return null;
    }

    if (path.getFileSystem() != FileSystems.getDefault()) {
      return null;
    }

    try (ZipFile zip = new ZipFile(path.toFile())) {
      String folderPrefix = getFolderPrefix(zip);

      CustomPaintingsJson json = readCustomPaintingsJson(zip, folderPrefix + CUSTOM_PAINTINGS_JSON);
      if (json == null) {
        return null;
      }

      PackMcmeta meta = readPackMcmeta(zip, folderPrefix + PACK_MCMETA);

      String packId = json.id();
      String name = json.name();
      List<LegacyPaintingResource> paintings = json.paintings();
      List<LegacyMigrationResource> migrations = json.migrations();
      String description = meta == null ? "" : meta.pack().description();

      HashMap<Identifier, Image> images = new HashMap<>();

      Image packIcon = readImage(zip, folderPrefix + PACK_PNG);
      if (packIcon != null && !packIcon.isEmpty()) {
        images.put(PackIcons.identifier(packId), packIcon);
      }

      paintings.forEach((painting) -> {
        Image image = readImage(zip, getPaintingPath(packId, painting.id()).toString());
        if (image != null && !image.isEmpty()) {
          images.put(new Identifier(packId, painting.id()), image);
        }
      });

      // TODO: Check for other images without metadata and add them.

      return new SinglePackResult(
          new LegacyPackResource(filename, packId, name, description, paintings, migrations), images);
    } catch (IOException e) {
      return null;
    }
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

  private static SinglePackResult readDirectoryAsPack(Path path) {
    String dirname = path.getFileName().toString();

    if (!Files.isRegularFile(path.resolve(CUSTOM_PAINTINGS_JSON), LinkOption.NOFOLLOW_LINKS)) {
      return null;
    }

    CustomPaintingsJson json = readCustomPaintingsJson(path.resolve(CUSTOM_PAINTINGS_JSON));
    if (json == null) {
      return null;
    }

    PackMcmeta meta = readPackMcmeta(path.resolve(PACK_MCMETA));

    String packId = json.id();
    String name = json.name();
    List<LegacyPaintingResource> paintings = json.paintings();
    List<LegacyMigrationResource> migrations = json.migrations();
    String description = meta == null ? "" : meta.pack().description();

    HashMap<Identifier, Image> images = new HashMap<>();

    Image packIcon = readImage(path.resolve(PACK_PNG));
    if (packIcon != null && !packIcon.isEmpty()) {
      images.put(PackIcons.identifier(packId), packIcon);
    }

    paintings.forEach((painting) -> {
      Image image = readImage(path.resolve(getPaintingPath(packId, painting.id())));
      if (image != null && !image.isEmpty()) {
        images.put(new Identifier(packId, painting.id()), image);
      }
    });

    // TODO: Check for other images without metadata and add them.

    return new SinglePackResult(
        new LegacyPackResource(dirname, packId, name, description, paintings, migrations), images);
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

  private static PackMcmeta readPackMcmeta(Path path) {
    try {
      return GSON.fromJson(Files.newBufferedReader(path), PackMcmeta.class);
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

  private record SinglePackResult(LegacyPackResource pack, HashMap<Identifier, Image> images) {
  }

  public record ConvertPromptData(HashMap<String, LegacyPackResource> packs, HashMap<Identifier, Image> images) {
    public ConvertPromptData() {
      this(new HashMap<>(), new HashMap<>());
    }
  }
}
