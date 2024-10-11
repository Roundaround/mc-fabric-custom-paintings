package me.roundaround.custompaintings.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.legacy.LegacyPackMigrator;
import me.roundaround.custompaintings.resource.legacy.LegacyPackResource;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;

public class ConvertPromptScreen extends Screen {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final HashMap<String, LegacyPackResource> packs;
  private final HashMap<Identifier, Image> images;

  public ConvertPromptScreen(
      Screen parent, HashMap<String, LegacyPackResource> packs, HashMap<Identifier, Image> images
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

  private void convertPack(LegacyPackResource legacyPack) {
    String filename = legacyPack.filename();
    Path zipFile = FabricLoader.getInstance()
        .getGameDir()
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID)
        .resolve(cleanFilename(filename) + ".zip");
    LegacyPackMigrator.getInstance().convertPack(legacyPack, this.images, zipFile);

    // TODO: Save to file
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
