package me.roundaround.custompaintings.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.PaintingResource;
import me.roundaround.custompaintings.resource.legacy.LegacyPackResource;
import me.roundaround.custompaintings.resource.legacy.LegacyPackWrapper;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConvertPromptScreen extends Screen {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final HashMap<String, LegacyPackWrapper> packs;
  private final HashMap<Identifier, Image> images;

  public ConvertPromptScreen(
      Screen parent, HashMap<String, LegacyPackWrapper> packs, HashMap<Identifier, Image> images
  ) {
    super(Text.of("Convert?"));
    this.parent = parent;
    this.packs = packs;
    this.images = images;
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.textRenderer, this.title);

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.YES, this::convertPacks).build());
    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.NO, this::close).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  private void convertPacks(ButtonWidget button) {
    this.packs.values().forEach(this::convertPack);
    this.close();
  }

  private void convertPack(LegacyPackWrapper wrapper) {
    String filename = wrapper.filename();
    LegacyPackResource legacyPack = wrapper.resource();
    ArrayList<PaintingResource> paintings = new ArrayList<>();
    legacyPack.paintings().forEach((legacyPainting) -> {
      paintings.add(new PaintingResource(legacyPainting.id(), legacyPainting.name(), legacyPainting.artist(),
          legacyPainting.height(), legacyPainting.width()
      ));
    });
    paintings.removeIf((painting) -> !this.images.containsKey(new Identifier(legacyPack.id(), painting.id())));
    PackResource pack = new PackResource(1, legacyPack.id(), legacyPack.name(), "", paintings);

    Path zipFile = FabricLoader.getInstance()
        .getGameDir()
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID)
        .resolve(cleanFilename(filename) + ".zip");
    try (
        FileOutputStream fos = new FileOutputStream(zipFile.toFile()); ZipOutputStream zos = new ZipOutputStream(fos)
    ) {
      this.writeJson(zos, pack);
    } catch (IOException e) {

    }

    // TODO: Save to file
  }

  private void writeJson(ZipOutputStream zos, PackResource pack) throws IOException {
    ZipEntry entry = new ZipEntry("custompaintings.json");
    zos.putNextEntry(entry);

    String content = GSON.toJson(pack);
    byte[] bytes = content.getBytes();
    zos.write(bytes, 0, bytes.length);
    zos.closeEntry();
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private static String cleanFilename(String filename) {
    Path path = Paths.get(filename);
    String noExtension = path.getFileName().toString();

    int dotIndex = noExtension.lastIndexOf(".");
    if (dotIndex > 0 && dotIndex < noExtension.length() - 1) {
      return noExtension.substring(0, dotIndex);
    }
    return noExtension;
  }
}
