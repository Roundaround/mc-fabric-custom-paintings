package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.resource.legacy.LegacyPackMigrator;
import me.roundaround.custompaintings.resource.legacy.LegacyPackResource;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ConvertPromptScreen extends Screen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final HashMap<String, LegacyPackResource> packs = new HashMap<>();
  private final HashMap<Identifier, Image> images = new HashMap<>();

  private LegacyPackList list;

  public ConvertPromptScreen(
      Screen parent, CompletableFuture<LegacyPackMigrator.ConvertPromptData> future
  ) {
    super(Text.of("Convert?"));
    this.parent = parent;

    future.whenCompleteAsync((data, exception) -> {
      if (exception != null) {
        // TODO: Handle error
        return;
      }

      this.packs.putAll(data.packs());
      this.images.putAll(data.images());

      if (this.list != null) {
        this.list.setPacks(this.packs.values());
      }

      this.clearChildren();
      this.layout.forEachChild(this::addDrawableChild);
    }, this.executor);
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new LegacyPackList(this.client, this.layout, this::convertPack));

    this.layout.addFooter(ButtonWidget.builder(Text.of("ALL!"), this::convertPacks).build());
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

  private void convertPack(LegacyPackResource pack) {
    String filename = pack.filename();
    Path zipFile = FabricLoader.getInstance()
        .getGameDir()
        .resolve("data")
        .resolve(CustomPaintingsMod.MOD_ID)
        .resolve(cleanFilename(filename) + ".zip");
    if (LegacyPackMigrator.getInstance().convertPack(pack, this.images, zipFile)) {
      this.list.getEntry(pack).ifPresent(LegacyPackList.PackEntry::markInactive);
    }
  }

  private void convertPacks(ButtonWidget button) {
    this.packs.values().forEach(this::convertPack);
    this.close();
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

  private static class LegacyPackList extends ParentElementEntryListWidget<LegacyPackList.Entry> {
    private final Consumer<LegacyPackResource> convert;

    public LegacyPackList(
        MinecraftClient client, ThreeSectionLayoutWidget layout, Consumer<LegacyPackResource> convert
    ) {
      super(client, layout);
      this.convert = convert;
      this.addEntry(LoadingEntry.factory(client.textRenderer));
    }

    public void setPacks(Collection<LegacyPackResource> packs) {
      this.clearEntries();

      if (packs.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.textRenderer));
        return;
      }

      for (LegacyPackResource pack : packs) {
        this.addEntry(PackEntry.factory(this.client.textRenderer, pack, this.convert));
      }

      this.refreshPositions();
    }

    public Optional<PackEntry> getEntry(LegacyPackResource pack) {
      return this.entries.stream()
          .map((entry) -> entry instanceof PackEntry packEntry ? packEntry : null)
          .filter((entry) -> entry != null && pack.packId().equals(entry.getPackId()))
          .findFirst();
    }

    private static abstract class Entry extends ParentElementEntryListWidget.Entry {
      protected static final int HEIGHT = 36;

      protected Entry(int index, int left, int top, int width) {
        super(index, left, top, width, HEIGHT);
      }
    }

    private static class LoadingEntry extends Entry {
      private static final Text LOADING_TEXT = Text.literal("Loading Legacy Pack List");

      private final TextRenderer textRenderer;

      protected LoadingEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width);
        this.textRenderer = textRenderer;
      }

      public static FlowListWidget.EntryFactory<LoadingEntry> factory(TextRenderer textRenderer) {
        return (index, left, top, width) -> new LoadingEntry(index, left, top, width, textRenderer);
      }

      @Override
      protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getContentCenterX() - this.textRenderer.getWidth(LOADING_TEXT) / 2;
        int y = this.getContentTop() + (this.getContentHeight() - this.textRenderer.fontHeight) / 2;
        context.drawText(this.textRenderer, LOADING_TEXT, x, y, GuiUtil.LABEL_COLOR, false);

        String spinner = LoadingDisplay.get(Util.getMeasuringTimeMs());
        x = this.getContentCenterX() - this.textRenderer.getWidth(spinner) / 2;
        y += this.textRenderer.fontHeight;
        context.drawText(this.textRenderer, spinner, x, y, Colors.GRAY, false);
      }
    }

    private static class EmptyEntry extends Entry {
      private final LabelWidget label;

      protected EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width);

        this.label = LabelWidget.builder(textRenderer, Text.literal("No legacy painting packs found!"))
            .position(this.getContentCenterX(), this.getContentCenterY())
            .dimensions(this.getContentWidth(), this.getContentHeight())
            .alignSelfCenterX()
            .alignSelfCenterY()
            .alignTextCenterX()
            .alignTextCenterY()
            .hideBackground()
            .showShadow()
            .build();

        this.addDrawable(this.label);
      }

      public static FlowListWidget.EntryFactory<EmptyEntry> factory(TextRenderer textRenderer) {
        return (index, left, top, width) -> new EmptyEntry(index, left, top, width, textRenderer);
      }

      @Override
      public void refreshPositions() {
        this.label.batchUpdates(() -> {
          this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
          this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
        });
      }
    }

    private static class PackEntry extends Entry {
      private final LegacyPackResource pack;
      private final ButtonWidget button;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          LegacyPackResource pack,
          Consumer<LegacyPackResource> convert
      ) {
        super(index, left, top, width);
        this.pack = pack;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        LinearLayoutWidget paragraph = LinearLayoutWidget.vertical().spacing(1);
        paragraph.add(LabelWidget.builder(textRenderer, Text.of(pack.filename()))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth());
        });
        paragraph.add(LabelWidget.builder(textRenderer, Text.of(pack.name()))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth());
        });
        paragraph.add(LabelWidget.builder(textRenderer, Text.of(pack.description()))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth());
        });
        layout.add(paragraph, (parent, self) -> {
          self.setWidth(width - GuiUtil.PADDING - 80);
        });

        this.button = layout.add(ButtonWidget.builder(Text.of("Convert"), (button) -> {
          convert.accept(pack);
        }).build(), (parent, self) -> {
          self.setDimensions(80, 20);
        });

        layout.forEachChild(this::addDrawableChild);
      }

      public static FlowListWidget.EntryFactory<PackEntry> factory(
          TextRenderer textRenderer, LegacyPackResource pack, Consumer<LegacyPackResource> convert
      ) {
        return (index, left, top, width) -> new PackEntry(index, left, top, width, textRenderer, pack, convert);
      }

      public String getPackId() {
        return this.pack.packId();
      }

      public void markInactive() {
        this.button.active = false;
      }
    }
  }
}
