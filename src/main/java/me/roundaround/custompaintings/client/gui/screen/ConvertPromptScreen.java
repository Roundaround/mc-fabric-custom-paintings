package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.resource.legacy.LegacyPackMigrator;
import me.roundaround.custompaintings.resource.legacy.LegacyPackResource;
import me.roundaround.custompaintings.resource.legacy.PackMetadata;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ConvertPromptScreen extends Screen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  private LegacyPackList list;

  public ConvertPromptScreen(
      Screen parent, CompletableFuture<Collection<PackMetadata>> future
  ) {
    // TODO: i18n
    super(Text.of("Convert?"));
    this.parent = parent;

    future.whenCompleteAsync((metas, exception) -> {
      if (exception != null) {
        // TODO: Handle error
        CustomPaintingsMod.LOGGER.warn(exception);
        return;
      }

      if (this.list != null) {
        this.list.setPacks(metas);
      }
    }, this.executor);
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new LegacyPackList(this.client, this.layout, this::convertPack));

    // TODO: i18n
    this.layout.addFooter(ButtonWidget.builder(Text.of("Open Output Folder"), this::openOutDir).build());
    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).build());

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

  private void convertPack(LegacyPackList.PackEntry entry) {
    LegacyPackResource pack = entry.getPack();
    Path path = LegacyPackMigrator.getInstance().getOutDir().resolve(cleanFilename(pack.path()) + ".zip");

    // TODO: Error handling
    // TODO: Add some kind of timeout on conversion to avoid hanging thread
    entry.markLoading();
    LegacyPackMigrator.getInstance().convertPack(pack, path).thenAcceptAsync(entry::markConvertFinished, this.executor);
  }

  private void openOutDir(ButtonWidget button) {
    Path outDir = LegacyPackMigrator.getInstance().getOutDir();
    if (outDir != null) {
      Util.getOperatingSystem().open(outDir.toUri());
    }
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private static String cleanFilename(Path path) {
    String noExtension = path.getFileName().toString();
    int dotIndex = noExtension.lastIndexOf(".");
    if (dotIndex > 0 && dotIndex < noExtension.length() - 1) {
      return noExtension.substring(0, dotIndex);
    }
    return noExtension;
  }

  private static class LegacyPackList extends ParentElementEntryListWidget<LegacyPackList.Entry> {
    private final Consumer<LegacyPackList.PackEntry> convert;

    public LegacyPackList(
        MinecraftClient client, ThreeSectionLayoutWidget layout, Consumer<LegacyPackList.PackEntry> convert
    ) {
      super(client, layout);
      this.convert = convert;
      this.addEntry(LoadingEntry.factory(client.textRenderer));
    }

    public void setPacks(Collection<PackMetadata> metas) {
      this.clearEntries();

      if (metas.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.textRenderer));
        return;
      }

      for (PackMetadata meta : metas) {
        this.addEntry(PackEntry.factory(this.client.textRenderer, meta, this.convert));
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
      // TODO: i18n
      private static final Text LABEL_CONVERT = Text.of("Convert");
      private static final Text LABEL_RE_CONVERT = Text.of("Re-Convert");
      private static final Text LABEL_IGNORED = Text.of("Ignored");

      private final LegacyPackResource pack;
      private final LoadingButtonWidget button;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          PackMetadata meta,
          Consumer<PackEntry> convert
      ) {
        super(index, left, top, width);
        this.pack = meta.pack();

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        // TODO: Pack icon!
        layout.add(FillerWidget.empty(), (parent, self) -> {
          self.setDimensions(parent.getHeight(), parent.getHeight());
        });

        LinearLayoutWidget paragraph = LinearLayoutWidget.vertical().spacing(1);
        paragraph.add(LabelWidget.builder(textRenderer, Text.of(pack.path().getFileName().toString()))
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
          self.setWidth(width - GuiUtil.PADDING - parent.getHeight() - GuiUtil.PADDING - 80);
        });

        // TODO: i18n
        Text label = meta.converted() ? LABEL_RE_CONVERT : meta.ignored() ? LABEL_IGNORED : LABEL_CONVERT;
        this.button = layout.add(new LoadingButtonWidget(0, 0, 80, 20, label, (button) -> convert.accept(this)));
        this.button.active = !meta.ignored();

        layout.forEachChild(this::addDrawableChild);
      }

      public static FlowListWidget.EntryFactory<PackEntry> factory(
          TextRenderer textRenderer, PackMetadata meta, Consumer<PackEntry> convert
      ) {
        return (index, left, top, width) -> new PackEntry(index, left, top, width, textRenderer, meta, convert);
      }

      public LegacyPackResource getPack() {
        return this.pack;
      }

      public String getPackId() {
        return this.pack.packId();
      }

      public void markLoading() {
        this.button.setLoading(true);
      }

      public void markConvertFinished(boolean succeeded) {
        this.button.setLoading(false);
        this.button.setMessage(succeeded ? LABEL_RE_CONVERT : LABEL_CONVERT);
        // TODO: Retry/error label?
      }
    }
  }
}
