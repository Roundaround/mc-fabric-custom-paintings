package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.resource.legacy.LegacyPackMigrator;
import me.roundaround.custompaintings.resource.legacy.PackMetadata;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LegacyConvertScreen extends Screen {
  private static final Text LABEL_CONVERT = Text.translatable("custompaintings.legacy.entry.convert");
  private static final Text LABEL_RE_CONVERT = Text.translatable("custompaintings.legacy.entry.reConvert");
  private static final Text TOOLTIP_SUCCESS = Text.translatable("custompaintings.legacy.entry.viewOutput");
  private static final Text TOOLTIP_FAILURE = Text.translatable("custompaintings.legacy.entry.error");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final HashMap<String, ConvertState> globalStates = new HashMap<>();
  private final HashMap<String, ConvertState> worldStates = new HashMap<>();

  private LegacyPackList list;
  private Path outDir;
  private HashMap<String, ConvertState> currentStates;

  public LegacyConvertScreen(
      MinecraftClient client, Screen parent
  ) {
    super(Text.translatable("custompaintings.legacy.title"));
    this.parent = parent;

    this.setOutDir(client.isInSingleplayer());

    LegacyPackMigrator.getInstance()
        .checkForLegacyPacks(client)
        .orTimeout(30, TimeUnit.SECONDS)
        .whenCompleteAsync((result, exception) -> {
          if (exception != null) {
            CustomPaintingsMod.LOGGER.warn(exception);
            if (this.list != null) {
              this.list.showErrorMessage();
            }
            return;
          }

          for (PackMetadata meta : result.metas()) {
            String legacyPackId = meta.id().asString();

            Path globalOutPath = result.globalConvertedIds().get(legacyPackId);
            if (globalOutPath != null) {
              this.globalStates.put(legacyPackId, ConvertState.success(globalOutPath));
            }

            Path worldOutPath = result.worldConvertedIds().get(legacyPackId);
            if (worldOutPath != null) {
              this.worldStates.put(legacyPackId, ConvertState.success(worldOutPath));
            }
          }

          if (this.list != null) {
            this.list.setPacks(this.currentStates, result.metas());
          }
        }, this.executor);
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);
    if (this.client.isInSingleplayer()) {
      this.layout.addHeader(
          CheckboxWidget.builder(Text.translatable("custompaintings.legacy.direct"), this.textRenderer)
              .callback(this::changeOutDir)
              .checked(true)
              .build());
    }

    this.list = this.layout.addBody(new LegacyPackList(this.client, this.layout, this::convertPack));

    this.layout.addFooter(
        ButtonWidget.builder(Text.translatable("custompaintings.legacy.output"), this::openOutDir).build());
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

  private void setOutDir(boolean worldScoped) {
    LegacyPackMigrator migrator = LegacyPackMigrator.getInstance();
    this.outDir = worldScoped ? migrator.getWorldOutDir() : migrator.getGlobalOutDir();
    this.currentStates = worldScoped ? this.worldStates : this.globalStates;
  }

  private void changeOutDir(CheckboxWidget checkbox, boolean checked) {
    this.setOutDir(checked);
    this.list.updateAllStates(this.currentStates);
  }

  private void convertPack(LegacyPackList.PackEntry entry) {
    String legacyPackId = entry.getLegacyPackId();
    PackMetadata meta = entry.getMeta();
    Path path = this.outDir.resolve(cleanFilename(meta.pack().path()) + ".zip");

    entry.markLoading();
    LegacyPackMigrator.getInstance()
        .convertPack(meta, path)
        .orTimeout(30, TimeUnit.SECONDS)
        .whenCompleteAsync((converted, exception) -> {
          ConvertState state;
          if (converted == null || !converted || exception != null) {
            // TODO: Include error message to show as a tooltip
            state = ConvertState.failed();
          } else {
            state = ConvertState.success(path);
          }
          this.currentStates.put(legacyPackId, state);
          entry.setState(state);
        }, this.executor);
  }

  private void openOutDir(ButtonWidget button) {
    if (this.outDir != null) {
      Util.getOperatingSystem().open(this.outDir.toUri());
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

    public void showErrorMessage() {
      this.clearEntries();
      this.addEntry(ErrorEntry.factory(this.client.textRenderer));
      this.refreshPositions();
    }

    public void setPacks(HashMap<String, ConvertState> currentStates, Collection<PackMetadata> metas) {
      this.clearEntries();

      if (metas.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.textRenderer));
        return;
      }

      for (PackMetadata meta : metas) {
        ConvertState state = currentStates.getOrDefault(meta.id().asString(), ConvertState.none());
        this.addEntry(PackEntry.factory(this.client.textRenderer, state, meta, this.convert));
      }

      this.refreshPositions();
    }

    public void updateAllStates(HashMap<String, ConvertState> states) {
      this.entries.forEach((entry) -> {
        if (entry instanceof PackEntry packEntry) {
          packEntry.setState(states.getOrDefault(packEntry.getLegacyPackId(), ConvertState.none()));
        }
      });
    }

    private static abstract class Entry extends ParentElementEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }

    private static class LoadingEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Text LOADING_TEXT = Text.translatable("custompaintings.legacy.loading");

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
      private static final Text MESSAGE = Text.translatable("custompaintings.legacy.none");

      private final LabelWidget label;

      protected EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width, HEIGHT);

        this.label = LabelWidget.builder(textRenderer, MESSAGE)
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

    private static class ErrorEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Text MESSAGE_LINE_1 = Text.translatable("custompaintings.legacy.error1");
      private static final Text MESSAGE_LINE_2 = Text.translatable("custompaintings.legacy.error2");

      private final LabelWidget label;

      protected ErrorEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width, HEIGHT);

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

    private static class PackEntry extends Entry {
      private static final int HEIGHT = 48;
      private static final int PACK_ICON_SIZE = 36;
      private static final int CONVERT_BUTTON_SIZE = 80;
      private static final int STATUS_BUTTON_SIZE = 20;
      private static final Text LINE_NAME = Text.translatable("custompaintings.legacy.entry.name");
      private static final Text LINE_DESCRIPTION = Text.translatable("custompaintings.legacy.entry.desc");
      private static final Text LINE_FILE = Text.translatable("custompaintings.legacy.entry.file");
      private static final Text NONE_PLACEHOLDER = Text.translatable("custompaintings.legacy.entry.emptyField")
          .formatted(Formatting.ITALIC, Formatting.GRAY);

      private final String legacyPackId;
      private final PackMetadata meta;
      private final LoadingButtonWidget convertButton;
      private final IconButtonWidget statusButton;

      private Path outPath;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          ConvertState initialState,
          PackMetadata meta,
          Consumer<PackEntry> convert
      ) {
        super(index, left, top, width, HEIGHT);
        this.legacyPackId = meta.id().asString();
        this.meta = meta;
        this.outPath = initialState.path;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        layout.add(SpriteWidget.create(LegacyPackMigrator.getInstance().getSprite(this.meta.pack().packId())),
            (parent, self) -> {
              self.setDimensions(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING);
        int headerWidth = Stream.of(LINE_FILE, LINE_NAME, LINE_DESCRIPTION)
            .mapToInt(textRenderer::getWidth)
            .max()
            .orElse(1);
        textSection.add(
            this.textLine(textRenderer, headerWidth, LINE_FILE, this.meta.pack().path().getFileName().toString()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_NAME, this.meta.pack().name()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_DESCRIPTION, this.meta.pack().description()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        layout.add(textSection, (parent, self) -> {
          int textSectionWidth = this.getContentWidth();
          textSectionWidth -= (parent.getChildren().size() - 1) * parent.getSpacing();
          for (Widget widget : parent.getChildren()) {
            if (widget != self) {
              textSectionWidth -= widget.getWidth();
            }
          }
          self.setWidth(textSectionWidth);
        });

        layout.add(FillerWidget.empty());

        Status status = initialState.status;
        this.convertButton = layout.add(
            new LoadingButtonWidget(0, 0, CONVERT_BUTTON_SIZE, ButtonWidget.DEFAULT_HEIGHT, status.getButtonLabel(),
                (button) -> convert.accept(this)
            ));
        this.statusButton = layout.add(IconButtonWidget.builder(status.getTexture(), IconButtonWidget.SIZE_L)
            .dimensions(STATUS_BUTTON_SIZE)
            .tooltip(status.getTooltip())
            .onPress((button) -> {
              if (this.outPath == null) {
                return;
              }
              Util.getOperatingSystem().open(this.outPath.getParent().toUri());
            })
            .build());
        if (status == Status.NONE) {
          this.statusButton.visible = false;
        } else {
          this.statusButton.active = status == Status.SUCCESS;
        }

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
          TextRenderer textRenderer, ConvertState initialState, PackMetadata meta, Consumer<PackEntry> convert
      ) {
        return (index, left, top, width) -> new PackEntry(
            index, left, top, width, textRenderer, initialState, meta, convert);
      }

      public String getLegacyPackId() {
        return this.legacyPackId;
      }

      public PackMetadata getMeta() {
        return this.meta;
      }

      public void markLoading() {
        this.convertButton.setLoading(true);
      }

      public void setState(ConvertState state) {
        this.convertButton.setLoading(false);
        this.convertButton.setMessage(state.status.getButtonLabel());

        if (state.status == Status.NONE) {
          this.statusButton.visible = false;
        } else {
          this.outPath = state.path;
          this.statusButton.visible = true;
          this.statusButton.active = state.status == Status.SUCCESS;
          this.statusButton.setTexture(state.status.getTexture());
          this.statusButton.setTooltip(Tooltip.of(state.status.getTooltip()));
        }
      }
    }
  }

  private enum Status {
    NONE(LABEL_CONVERT, null, null),
    SUCCESS(LABEL_RE_CONVERT, TOOLTIP_SUCCESS, new Identifier("pending_invite/accept")),
    FAILURE(LABEL_CONVERT, TOOLTIP_FAILURE, new Identifier("pending_invite/reject"));

    private final Text buttonLabel;
    private final Text tooltip;
    private final Identifier texture;

    Status(Text buttonLabel, Text tooltip, Identifier texture) {
      this.buttonLabel = buttonLabel;
      this.tooltip = tooltip;
      this.texture = texture;
    }

    public Text getButtonLabel() {
      return this.buttonLabel;
    }

    // TODO: Tooltip based on actual error?
    public Text getTooltip() {
      return this.tooltip;
    }

    public Identifier getTexture() {
      return this.texture;
    }
  }

  private static class ConvertState {
    public Status status;
    public Path path;

    private ConvertState(Status status, Path path) {
      this.status = status;
      this.path = path;
    }

    public static ConvertState success(Path path) {
      return new ConvertState(Status.SUCCESS, path);
    }

    public static ConvertState none() {
      return new ConvertState(Status.NONE, null);
    }

    public static ConvertState failed() {
      return new ConvertState(Status.FAILURE, null);
    }
  }
}
