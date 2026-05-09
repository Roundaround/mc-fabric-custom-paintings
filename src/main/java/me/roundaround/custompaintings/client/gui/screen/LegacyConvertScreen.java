package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.legacy.LegacyPackConverter;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
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
  private static final Component LABEL_CONVERT = Component.translatable("custompaintings.legacy.entry.convert");
  private static final Component LABEL_RE_CONVERT = Component.translatable("custompaintings.legacy.entry.reConvert");
  private static final Component TOOLTIP_SUCCESS = Component.translatable("custompaintings.legacy.entry.viewOutput");
  private static final Component TOOLTIP_FAILURE = Component.translatable("custompaintings.legacy.entry.error");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final HashMap<String, ConvertState> globalStates = new HashMap<>();
  private final HashMap<String, ConvertState> worldStates = new HashMap<>();

  private LegacyPackList list;
  private Path outDir;
  private HashMap<String, ConvertState> currentStates;

  public LegacyConvertScreen(Minecraft client, Screen parent) {
    super(Component.translatable("custompaintings.legacy.title"));
    this.parent = parent;

    this.setOutDir(client.isLocalServer());

    LegacyPackConverter.getInstance()
        .checkForLegacyPacksAndConvertedIds(client)
        .orTimeout(30, TimeUnit.SECONDS)
        .whenCompleteAsync(
            (result, exception) -> {
              if (exception != null) {
                CustomPaintingsMod.LOGGER.warn(exception);
                if (this.list != null) {
                  this.list.showErrorMessage();
                }
                return;
              }

              for (Metadata meta : result.metas()) {
                String fileUid = meta.fileUid().stringValue();

                Path globalOutPath = result.globalConvertedIds().get(fileUid);
                if (globalOutPath != null) {
                  this.globalStates.put(fileUid, ConvertState.success(globalOutPath));
                }

                Path worldOutPath = result.worldConvertedIds().get(fileUid);
                if (worldOutPath != null) {
                  this.worldStates.put(fileUid, ConvertState.success(worldOutPath));
                }
              }

              if (this.list != null) {
                this.list.setPacks(this.currentStates, result.metas());
              }
            }, this.screenExecutor
        );
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.font, this.title);
    if (this.minecraft.isLocalServer()) {
      this.layout.addHeader(Checkbox.builder(Component.translatable("custompaintings.legacy.direct"), this.font)
          .onValueChange(this::changeOutDir)
          .selected(true)
          .build());
    }

    this.list = this.layout.addBody(new LegacyPackList(this.minecraft, this.layout, this::convertPack));

    this.layout.addFooter(Button.builder(Component.translatable("custompaintings.legacy.output"), this::openOutDir)
        .build());
    this.layout.addFooter(Button.builder(CommonComponents.GUI_DONE, this::close).build());

    VersionStamp.create(this.font, this.layout);

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public void onClose() {
    Objects.requireNonNull(this.minecraft).setScreen(this.parent);
  }

  private void setOutDir(boolean worldScoped) {
    LegacyPackConverter migrator = LegacyPackConverter.getInstance();
    this.outDir = worldScoped ? migrator.getWorldOutDir() : migrator.getGlobalOutDir();
    this.currentStates = worldScoped ? this.worldStates : this.globalStates;
  }

  private void changeOutDir(Checkbox checkbox, boolean checked) {
    this.setOutDir(checked);
    this.list.updateAllStates(this.currentStates);
  }

  private void convertPack(LegacyPackList.PackEntry entry) {
    Metadata meta = entry.getMeta();
    Path path = this.outDir.resolve(cleanFilename(meta.fileUid().filename()) + ".zip");

    entry.markLoading();
    LegacyPackConverter.getInstance().convertPack(meta, path).orTimeout(30, TimeUnit.SECONDS).whenCompleteAsync(
        (converted, exception) -> {
          ConvertState state;
          if (converted == null || !converted || exception != null) {
            // TODO: Include error message to show as a tooltip
            if (exception != null) {
              CustomPaintingsMod.LOGGER.warn(exception);
            }
            state = ConvertState.failed();
          } else {
            state = ConvertState.success(path);
          }
          this.currentStates.put(meta.fileUid().stringValue(), state);
          entry.setState(state);
        }, this.screenExecutor
    );
  }

  private void openOutDir(Button button) {
    if (this.outDir != null) {
      Util.getPlatform().openUri(this.outDir.toUri());
    }
  }

  private void close(Button button) {
    this.onClose();
  }

  private static String cleanFilename(String fileName) {
    int dotIndex = fileName.lastIndexOf(".");
    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
      return fileName.substring(0, dotIndex);
    }
    return fileName;
  }

  private static class LegacyPackList extends ParentElementEntryListWidget<LegacyPackList.Entry> {
    private final Consumer<LegacyPackList.PackEntry> convert;

    public LegacyPackList(
        Minecraft client,
        ThreeSectionLayoutWidget layout,
        Consumer<LegacyPackList.PackEntry> convert
    ) {
      super(client, layout);
      this.convert = convert;
      this.addEntry(LoadingEntry.factory(client.font));
    }

    public void showErrorMessage() {
      this.clearEntries();
      this.addEntry(ErrorEntry.factory(this.client.font));
      this.arrangeElements();
    }

    public void setPacks(HashMap<String, ConvertState> currentStates, Collection<Metadata> metas) {
      this.clearEntries();

      if (metas.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.font));
        return;
      }

      for (Metadata meta : metas) {
        ConvertState state = currentStates.getOrDefault(meta.fileUid().stringValue(), ConvertState.none());
        this.addEntry(PackEntry.factory(this.client.font, state, meta, this.convert));
      }

      this.arrangeElements();
    }

    public void updateAllStates(HashMap<String, ConvertState> states) {
      this.entries.forEach((entry) -> {
        if (entry instanceof PackEntry packEntry) {
          packEntry.setState(states.getOrDefault(packEntry.getMeta().fileUid().stringValue(), ConvertState.none()));
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
      private static final Component LOADING_TEXT = Component.translatable("custompaintings.legacy.loading");

      private final Font textRenderer;

      protected LoadingEntry(int index, int left, int top, int width, Font textRenderer) {
        super(index, left, top, width, HEIGHT);
        this.textRenderer = textRenderer;
      }

      public static FlowListWidget.EntryFactory<LoadingEntry> factory(Font textRenderer) {
        return (index, left, top, width) -> new LoadingEntry(index, left, top, width, textRenderer);
      }

      @Override
      protected void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = this.getContentCenterX() - this.textRenderer.width(LOADING_TEXT) / 2;
        int y = this.getContentTop() + (this.getContentHeight() - this.textRenderer.lineHeight) / 2;
        context.text(this.textRenderer, LOADING_TEXT, x, y, GuiUtil.LABEL_COLOR, false);

        String spinner = LoadingDotsText.get(Util.getMillis());
        x = this.getContentCenterX() - this.textRenderer.width(spinner) / 2;
        y += this.textRenderer.lineHeight;
        context.text(this.textRenderer, spinner, x, y, CommonColors.GRAY, false);
      }
    }

    private static class EmptyEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Component MESSAGE = Component.translatable("custompaintings.legacy.none");

      private final LabelWidget label;

      protected EmptyEntry(int index, int left, int top, int width, Font textRenderer) {
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

      public static FlowListWidget.EntryFactory<EmptyEntry> factory(Font textRenderer) {
        return (index, left, top, width) -> new EmptyEntry(index, left, top, width, textRenderer);
      }

      @Override
      public void arrangeElements() {
        this.label.batchUpdates(() -> {
          this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
          this.label.setSize(this.getContentWidth(), this.getContentHeight());
        });
      }
    }

    private static class ErrorEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Component MESSAGE_LINE_1 = Component.translatable("custompaintings.legacy.error1");
      private static final Component MESSAGE_LINE_2 = Component.translatable("custompaintings.legacy.error2");

      private final LabelWidget label;

      protected ErrorEntry(int index, int left, int top, int width, Font textRenderer) {
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
            .color(CommonColors.RED)
            .build();

        this.addDrawable(this.label);
      }

      public static FlowListWidget.EntryFactory<ErrorEntry> factory(Font textRenderer) {
        return (index, left, top, width) -> new ErrorEntry(index, left, top, width, textRenderer);
      }

      @Override
      public void arrangeElements() {
        this.label.batchUpdates(() -> {
          this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
          this.label.setSize(this.getContentWidth(), this.getContentHeight());
        });
      }
    }

    private static class PackEntry extends Entry {
      private static final int HEIGHT = 48;
      private static final int PACK_ICON_SIZE = 36;
      private static final int CONVERT_BUTTON_SIZE = 80;
      private static final int STATUS_BUTTON_SIZE = 20;
      private static final Component LINE_NAME = Component.translatable("custompaintings.legacy.entry.name");
      private static final Component LINE_DESCRIPTION = Component.translatable("custompaintings.legacy.entry.desc");
      private static final Component LINE_FILE = Component.translatable("custompaintings.legacy.entry.file");
      private static final Component NONE_PLACEHOLDER = Component.translatable(
          "custompaintings.legacy.entry" + ".emptyField").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

      private final Metadata meta;
      private final LoadingButtonWidget convertButton;
      private final IconButtonWidget statusButton;

      private Path outPath;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          Font textRenderer,
          ConvertState initialState,
          Metadata meta,
          Consumer<PackEntry> convert
      ) {
        super(index, left, top, width, HEIGHT);
        this.meta = meta;
        this.outPath = initialState.path;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight()
              );
            }
        );

        layout.add(
            SpriteWidget.create(LegacyPackConverter.getInstance().getSprite(this.meta.pack().id())), (parent, self) -> {
              self.setSize(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING);
        int headerWidth = Stream.of(LINE_FILE, LINE_NAME, LINE_DESCRIPTION)
            .mapToInt(textRenderer::width)
            .max()
            .orElse(1);
        textSection.add(
            this.textLine(textRenderer, headerWidth, LINE_FILE, this.meta.fileUid().filename()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(
            this.textLine(textRenderer, headerWidth, LINE_NAME, this.meta.pack().name()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(
            this.textLine(textRenderer, headerWidth, LINE_DESCRIPTION, this.meta.pack().description()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        layout.add(
            textSection, (parent, self) -> {
              int textSectionWidth = this.getContentWidth();
              textSectionWidth -= (parent.getChildren().size() - 1) * parent.getSpacing();
              for (LayoutElement widget : parent.getChildren()) {
                if (widget != self) {
                  textSectionWidth -= widget.getWidth();
                }
              }
              self.setWidth(textSectionWidth);
            }
        );

        layout.add(FillerWidget.empty());

        Status status = initialState.status;
        this.convertButton = layout.add(new LoadingButtonWidget(
            0,
            0,
            CONVERT_BUTTON_SIZE,
            Button.DEFAULT_HEIGHT,
            status.getButtonLabel(),
            (button) -> convert.accept(this)
        ));
        this.statusButton = layout.add(IconButtonWidget.builder(status.getTexture(), IconButtonWidget.SIZE_L)
            .dimensions(STATUS_BUTTON_SIZE)
            .tooltip(status.getTooltip())
            .onPress((button) -> {
              if (this.outPath == null) {
                return;
              }
              Util.getPlatform().openUri(this.outPath.getParent().toUri());
            })
            .build());
        if (status == Status.NONE) {
          this.statusButton.visible = false;
        } else {
          this.statusButton.active = status == Status.SUCCESS;
        }

        layout.visitWidgets(this::addDrawableChild);
      }

      private LinearLayoutWidget textLine(Font textRenderer, int headerWidth, Component header, String value) {
        LinearLayoutWidget line = LinearLayoutWidget.horizontal().spacing(2);
        Component valueText = value == null || value.isBlank() ? NONE_PLACEHOLDER : Component.nullToEmpty(value);

        line.add(
            LabelWidget.builder(textRenderer, header)
                .hideBackground()
                .showShadow()
                .color(CommonColors.LIGHT_GRAY)
                .build(), (parent, self) -> {
              self.setWidth(headerWidth);
            }
        );
        line.add(
            LabelWidget.builder(textRenderer, valueText)
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> {
              self.setWidth(parent.getWidth() - parent.getSpacing() - headerWidth);
            }
        );

        return line;
      }

      public static FlowListWidget.EntryFactory<PackEntry> factory(
          Font textRenderer,
          ConvertState initialState,
          Metadata meta,
          Consumer<PackEntry> convert
      ) {
        return (index, left, top, width) -> new PackEntry(
            index,
            left,
            top,
            width,
            textRenderer,
            initialState,
            meta,
            convert
        );
      }

      public Metadata getMeta() {
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
          this.statusButton.setTooltip(Tooltip.create(state.status.getTooltip()));
        }
      }
    }
  }

  private enum Status {
    NONE(LABEL_CONVERT, Component.empty(), null),
    SUCCESS(LABEL_RE_CONVERT, TOOLTIP_SUCCESS, Identifier.withDefaultNamespace("pending_invite/accept")),
    FAILURE(LABEL_CONVERT, TOOLTIP_FAILURE, Identifier.withDefaultNamespace("pending_invite/reject"));

    private final Component buttonLabel;
    private final Component tooltip;
    private final Identifier texture;

    Status(Component buttonLabel, Component tooltip, Identifier texture) {
      this.buttonLabel = buttonLabel;
      this.tooltip = tooltip;
      this.texture = texture;
    }

    public Component getButtonLabel() {
      return this.buttonLabel;
    }

    // TODO: Tooltip based on actual error?
    public Component getTooltip() {
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
