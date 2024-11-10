package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.util.StringUtil;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CacheScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.DEFAULT_WIDTH_SMALL;
  // TODO: i18n
  private static final Text LABEL_SERVERS = Text.of("Worlds/servers tracked");
  private static final Text LABEL_IMAGES = Text.of("Painting and icon images cached");
  private static final Text LABEL_BYTES = Text.of("Used storage space");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  public CacheScreen(Screen parent) {
    // TODO: i18n
    super(Text.of("Custom Paintings Mod Cache"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    StatsList list = this.layout.addBody(new StatsList(this.client, this.layout));
    this.fetchStats(list);

    // TODO: i18n
    this.layout.addFooter(
        ButtonWidget.builder(Text.of("Clear Cache"), (b) -> this.clearCache(list)).width(BUTTON_WIDTH).build());
    // TODO: i18n
    this.layout.addFooter(
        ButtonWidget.builder(Text.of("Configure"), (b) -> this.navigateConfig()).width(BUTTON_WIDTH).build());
    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (b) -> this.close()).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    assert this.client != null;
    this.client.setScreen(this.parent);
  }

  private void fetchStats(StatsList list) {
    CacheManager.getInstance().getStats().orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync((result, exception) -> {
      if (exception != null || result == null) {
        list.setError();
      } else {
        list.setStats(result.servers(), result.images(), result.bytes());
      }
    }, this.client);
  }

  private void clearCache(StatsList list) {
    try {
      // TODO: Run clear in IO thread
      CacheManager.getInstance().clear();
      this.fetchStats(list);
    } catch (IOException e) {
      // TODO: Handle exception
      throw new RuntimeException(e);
    }
  }

  private void navigateConfig() {
    assert this.client != null;
    this.client.setScreen(new ConfigScreen(this, CustomPaintingsMod.MOD_ID, CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()
    ));
  }

  private static class StatsList extends NarratableEntryListWidget<StatsList.Entry> {
    public StatsList(MinecraftClient client, ThreeSectionLayoutWidget layout) {
      super(client, layout);

      this.setShouldHighlightHover(false);
      this.setShouldHighlightSelection(false);

      this.addEntry(LoadingEntry.factory(client.textRenderer));
    }

    public void setError() {
      this.clearEntries();
      this.addEntry(ErrorEntry.factory(this.client.textRenderer));
      this.refreshPositions();
    }

    public void setStats(int servers, int images, long bytes) {
      this.clearEntries();

      Text valueServers = Text.of(String.valueOf(servers));
      Text valueImages = Text.of(String.valueOf(images));
      Text valueBytes = Text.of(StringUtil.formatBytes(bytes));
      int maxValueWidth = Stream.of(valueServers, valueImages, valueBytes)
          .mapToInt(this.client.textRenderer::getWidth)
          .max()
          .orElse(1);

      this.addEntry(StatEntry.factory(this.client.textRenderer, LABEL_SERVERS, valueServers, maxValueWidth));
      this.addEntry(StatEntry.factory(this.client.textRenderer, LABEL_IMAGES, valueImages, maxValueWidth));
      this.addEntry(StatEntry.factory(this.client.textRenderer, LABEL_BYTES, valueBytes, maxValueWidth));
      this.refreshPositions();
    }

    private abstract static class Entry extends NarratableEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width) {
        super(index, left, top, width, 11);
      }
    }

    private static class LoadingEntry extends Entry {
      // TODO: i18n
      private static final Text LOADING_TEXT = Text.translatable("custompaintings.cache.loading");

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

      @Override
      public Text getNarration() {
        return LOADING_TEXT;
      }
    }

    private static class ErrorEntry extends Entry {
      // TODO: i18n
      private static final Text MESSAGE_LINE_1 = Text.translatable("custompaintings.cache.error1");
      private static final Text MESSAGE_LINE_2 = Text.translatable("custompaintings.cache.error2");

      private final LabelWidget label;

      protected ErrorEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width);

        this.label = LabelWidget.builder(textRenderer, List.of(MESSAGE_LINE_1, MESSAGE_LINE_2))
            .position(this.getContentCenterX(), this.getContentCenterY())
            .dimensions(this.getContentWidth(), this.getContentHeight())
            .alignSelfCenterX()
            .alignSelfCenterY()
            .alignTextCenterX()
            .alignTextCenterY()
            .hideBackground()
            .showShadow()
            .color(Colors.RED)
            .build();

        this.addDrawable(this.label);
      }

      @Override
      public Text getNarration() {
        return this.label.getText();
      }

      public static FlowListWidget.EntryFactory<ErrorEntry> factory(TextRenderer textRenderer) {
        return (index, left, top, width) -> new ErrorEntry(index, left, top, width, textRenderer);
      }

      @Override
      public void refreshPositions() {
        this.label.batchUpdates(() -> {
          this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
          this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
        });
      }
    }

    private static class StatEntry extends Entry {
      private final Text label;
      private final Text value;

      public StatEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          Text label,
          Text value,
          int valueColumnWidth
      ) {
        super(index, left, top, width);
        this.label = label;
        this.value = value;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlignCenter(), (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            });

        layout.add(LabelWidget.builder(textRenderer, this.label)
            .alignTextLeft()
            .hideBackground()
            .showShadow()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .build(), (parent, self) -> self.setWidth(this.getContentWidth() - parent.getSpacing() - valueColumnWidth));

        layout.add(LabelWidget.builder(textRenderer, this.value)
            .alignTextRight()
            .hideBackground()
            .showShadow()
            .width(valueColumnWidth)
            .build());

        layout.forEachChild(this::addDrawable);
      }

      @Override
      public Text getNarration() {
        return this.label.copy().append(this.value);
      }

      public static FlowListWidget.EntryFactory<StatEntry> factory(
          TextRenderer textRenderer, Text label, Text value, int valueColumnWidth
      ) {
        return (index, left, top, width) -> new StatEntry(
            index, left, top, width, textRenderer, label, value, valueColumnWidth);
      }
    }
  }
}
