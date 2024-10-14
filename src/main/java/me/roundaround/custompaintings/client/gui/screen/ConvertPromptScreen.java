package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
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
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    private static abstract class Entry extends ParentElementEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }

    private static class LoadingEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Text LOADING_TEXT = Text.literal("Loading Legacy Pack List");

      private final TextRenderer textRenderer;

      protected LoadingEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width, HEIGHT);
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
      private static final int HEIGHT = 36;

      private final LabelWidget label;

      protected EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width, HEIGHT);

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
      private static final int HEIGHT = 48;
      private static final int PACK_ICON_SIZE = 36;
      private static final int STATUS_ICON_SIZE = 18;
      // TODO: i18n
      private static final Text LINE_NAME = Text.of("Name:");
      private static final Text LINE_DESCRIPTION = Text.of("Desc:");
      private static final Text LINE_FILE = Text.of("File:");
      private static final Text NONE_PLACEHOLDER = Text.literal("< None >")
          .formatted(Formatting.ITALIC, Formatting.GRAY);
      private static final Text LABEL_CONVERT = Text.of("Convert");
      private static final Text LABEL_RE_CONVERT = Text.of("Re-Convert");
      private static final Text LABEL_IGNORED = Text.of("Ignored");
      private static final Identifier TEXTURE_SUCCESS = new Identifier("pending_invite/accept");
      private static final Identifier TEXTURE_FAILURE = new Identifier("pending_invite/reject");

      private final LegacyPackResource pack;
      private final LoadingButtonWidget button;

      private Identifier statusTexture;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          PackMetadata meta,
          Consumer<PackEntry> convert
      ) {
        super(index, left, top, width, HEIGHT);
        this.pack = meta.pack();

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        layout.add(SpriteWidget.create(LegacyPackMigrator.getInstance().getSprite(pack.packId())), (parent, self) -> {
          self.setDimensions(PACK_ICON_SIZE, PACK_ICON_SIZE);
        });

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING);
        int headerWidth = Stream.of(LINE_FILE, LINE_NAME, LINE_DESCRIPTION)
            .mapToInt(textRenderer::getWidth)
            .max()
            .orElse(1);
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_FILE, pack.path().getFileName().toString()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_NAME, pack.name()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_DESCRIPTION, pack.description()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        layout.add(textSection, (parent, self) -> {
          self.setWidth(width - parent.getSpacing() - PACK_ICON_SIZE - parent.getSpacing() - 80 - parent.getSpacing() -
                        STATUS_ICON_SIZE);
        });

        layout.add(FillerWidget.empty());

        // TODO: i18n
        Text label = meta.converted() ? LABEL_RE_CONVERT : meta.ignored() ? LABEL_IGNORED : LABEL_CONVERT;
        this.button = layout.add(new LoadingButtonWidget(0, 0, 80, 20, label, (button) -> convert.accept(this)));
        this.button.active = !meta.ignored();

        this.statusTexture = meta.converted() ? TEXTURE_SUCCESS : null;
        layout.add(new DrawableWidget(STATUS_ICON_SIZE, STATUS_ICON_SIZE) {
          @Override
          protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            if (PackEntry.this.statusTexture == null) {
              return;
            }
            context.drawGuiTexture(
                PackEntry.this.statusTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight());
          }
        });

        layout.forEachChild(this::addDrawableChild);
      }

      private LinearLayoutWidget textLine(TextRenderer textRenderer, int headerWidth, Text header, String value) {
        LinearLayoutWidget line = LinearLayoutWidget.horizontal().spacing(2);
        Text valueText = value == null || value.isBlank() ? NONE_PLACEHOLDER : Text.of(value);

        line.add(
            LabelWidget.builder(textRenderer, header).hideBackground().showShadow().color(Colors.LIGHT_GRAY).build(),
            (parent, self) -> {
              self.setWidth(headerWidth);
            }
        );
        line.add(LabelWidget.builder(textRenderer, valueText)
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth() - parent.getSpacing() - headerWidth);
        });

        return line;
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
        this.statusTexture = succeeded ? TEXTURE_SUCCESS : TEXTURE_FAILURE;
        // TODO: Retry/error label?
      }
    }
  }
}
