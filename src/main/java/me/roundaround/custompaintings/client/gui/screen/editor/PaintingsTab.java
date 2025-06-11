package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.ImageButtonWidget;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.util.Axis;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.EmptyWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import me.roundaround.custompaintings.roundalib.observable.Subscription;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class PaintingsTab extends PackEditorTab {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final int BUTTON_HEIGHT = 20;

  private final LabelWidget countLabel;
  private final TextFieldWidget searchBox;
  private final PaintingList paintingList;

  public PaintingsTab(
      @NotNull MinecraftClient client,
      @NotNull State state,
      @NotNull EditorScreen screen) {
    super(
        client,
        state,
        screen,
        Text.translatable("custompaintings.editor.editor.paintings.title"));

    this.layout.flowAxis(Axis.HORIZONTAL)
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    LinearLayoutWidget sidePanel = LinearLayoutWidget.vertical()
        .spacing(GuiUtil.PADDING);

    this.countLabel = sidePanel.add(LabelWidget.builder(this.client.textRenderer, Text.of("0"))
        .hideBackground()
        .showShadow()
        .build());
    this.subscriptions.add(this.state.paintings.subscribe((paintings) -> {
      this.countLabel.setText(Text.of(String.format("%d", paintings.size())));
    }));

    this.layout.add(sidePanel, (parent, self) -> {
      self.setDimensions(this.getPanelWidth(layout), parent.getInnerHeight());
    });

    LinearLayoutWidget listColumn = LinearLayoutWidget.vertical()
        .defaultOffAxisContentAlignCenter()
        .spacing(GuiUtil.PADDING);

    LinearLayoutWidget searchRow = listColumn.add(
        LinearLayoutWidget
            .horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter(),
        (parent, self) -> {
          self.setDimensions(listColumn.getWidth(), BUTTON_HEIGHT + GuiUtil.PADDING);
        });

    this.searchBox = searchRow.add(
        new TextFieldWidget(
            this.client.textRenderer,
            0,
            BUTTON_HEIGHT,
            Text.translatable("custompaintings.editor.editor.paintings.search")),
        (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });
    this.searchBox.setChangedListener(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(BuiltinIcon.CLOSE_13, CustomPaintingsMod.MOD_ID)
        .medium()
        .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.clear"))
        .onPress(this::clearSearch)
        .build());

    this.paintingList = listColumn.add(new PaintingList(
        this.client,
        listColumn.getInnerWidth(),
        listColumn.getInnerHeight(),
        this.state.paintings,
        this::editInfo,
        this::editImage,
        this.state::movePaintingUp,
        this.state::movePaintingDown,
        this.subscriptions::add),
        (parent, self) -> {
          self.setDimensions(parent.getInnerWidth(), parent.getUnusedSpace(self));
        });

    this.layout.add(listColumn, (parent, self) -> {
      self.setDimensions(
          parent.getUnusedSpace(self),
          parent.getInnerHeight());
    });

    this.layout.refreshPositions();
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private void onSearchBoxChanged(String text) {
    this.paintingList.setSearch(text);
  }

  private void clearSearch(ButtonWidget button) {
    this.searchBox.setText("");
  }

  private void editInfo(int paintingIndex) {
    CustomPaintingsMod.LOGGER.info("Editing info for painting {}", paintingIndex);
  }

  private void editImage(int paintingIndex) {
    List<PackData.Painting> paintings = this.state.paintings.get();
    if (paintingIndex < 0 || paintingIndex >= paintings.size()) {
      return;
    }

    this.client.setScreen(new ImageScreen(
        Text.of("Edit image"),
        new ScreenParent(() -> new EditorScreen(
            this.screen.getParent(),
            this.client,
            this.state)),
        this.client,
        paintings.get(paintingIndex).image(),
        (image) -> {
          this.state.setImage(paintingIndex, image);
        }));
  }

  record FilteredState(
      String search,
      int totalCount,
      List<PackData.Painting> filtered,
      Map<Integer, Integer> indexMap) {
  }

  static class PaintingList extends ParentElementEntryListWidget<PaintingList.Entry> {
    private final Subject<List<PackData.Painting>> paintings;
    private final Subject<String> search = Subject.of("");

    public PaintingList(
        MinecraftClient client,
        int width,
        int height,
        Subject<List<PackData.Painting>> observable,
        Consumer<Integer> editCallback,
        Consumer<Integer> imageCallback,
        Consumer<Integer> moveUpCallback,
        Consumer<Integer> moveDownCallback,
        Consumer<Subscription> addSubscription) {
      super(client, 0, 0, width, height);
      this.setContentPadding(GuiUtil.PADDING);
      this.setRowSpacing(GuiUtil.PADDING / 2);

      this.paintings = observable;

      addSubscription.accept(Observable.subscribeAll(this.search, this.paintings, (search, paintings) -> {
        int totalCount = paintings.size();
        List<PackData.Painting> filtered = new ArrayList<>();
        Map<Integer, Integer> indexMap = new HashMap<>();

        for (int paintingIdx = 0; paintingIdx < paintings.size(); paintingIdx++) {
          PackData.Painting painting = paintings.get(paintingIdx);
          if (this.matches(search, painting)) {
            indexMap.put(filtered.size(), paintingIdx);
            filtered.add(painting);
          }
        }

        int filteredCount = filtered.size();
        if (filteredCount > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < filteredCount; i++) {
            this.addEntry(Entry.factory(
                this.client.textRenderer,
                imageCallback,
                editCallback,
                moveUpCallback,
                moveDownCallback,
                filtered.get(i),
                indexMap.get(i),
                totalCount));
          }
        } else {
          for (int i = this.getEntryCount(); i > filteredCount; i--) {
            this.removeEntry();
          }
        }

        // TODO: Handle filtered count == 0

        int maxIndexWidth = IntStream.range(1, totalCount)
            .map((i) -> this.client.textRenderer.getWidth(Text.of(String.format("%d", i))))
            .max()
            .orElse(1);
        for (int i = 0; i < this.getEntryCount(); i++) {
          this.getEntry(i).setData(
              filtered.get(i),
              indexMap.get(i),
              maxIndexWidth,
              totalCount);
        }
      }));
    }

    public void setSearch(String search) {
      this.search.set(search);
    }

    private boolean matches(String search, PackData.Painting painting) {
      String query = this.sanitize(search);
      if (query.isBlank()) {
        return true;
      }

      return Stream.of(painting.id(), painting.name(), painting.artist())
          .map(this::sanitize)
          .anyMatch((value) -> value.contains(query));
    }

    private String sanitize(String text) {
      return text.toLowerCase().replace(" ", "");
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      private static final Text LINE_ID = Text.translatable("custompaintings.editor.editor.paintings.id");
      private static final Text LINE_NAME = Text.translatable("custompaintings.editor.editor.paintings.name");
      private static final Text LINE_ARTIST = Text.translatable("custompaintings.editor.editor.paintings.artist");
      private static final Text LINE_BLOCKS = Text.translatable("custompaintings.editor.editor.paintings.blocks");

      private final TextRenderer textRenderer;
      private final Consumer<Integer> imageCallback;
      private final Consumer<Integer> editCallback;
      private final Consumer<Integer> moveUpCallback;
      private final Consumer<Integer> moveDownCallback;
      private final LinearLayoutWidget layout;
      private final LabelWidget idLabel;
      private final LabelWidget nameLabel;
      private final LabelWidget artistLabel;
      private final LabelWidget blocksLabel;
      private final ImageButtonWidget imageButton;
      private final IconButtonWidget moveUpButton;
      private final IconButtonWidget moveDownButton;
      private final LabelWidget indexLabel;

      private PackData.Painting painting;
      private int paintingIndex;

      public Entry(
          TextRenderer textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> imageCallback,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
          int paintingIndex,
          int totalCount) {
        super(index, left, top, width, textRenderer.fontHeight * 4 + 3);
        this.textRenderer = textRenderer;
        this.imageCallback = imageCallback;
        this.editCallback = editCallback;
        this.moveUpCallback = moveUpCallback;
        this.moveDownCallback = moveDownCallback;
        this.painting = painting;
        this.paintingIndex = paintingIndex;

        this.layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.imageButton = this.layout.add(
            new ImageButtonWidget(
                Text.translatable("custompaintings.editor.editor.paintings.image"),
                (button) -> this.imageCallback.accept(this.paintingIndex),
                (image) -> State.getImageTextureId(image),
                this.painting.image()),
            (parent, self) -> {
              self.setDimensions(this.getContentHeight(), this.getContentHeight());
            });

        this.layout.add(new EmptyWidget());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical()
            .spacing(1)
            .defaultOffAxisContentAlignStart();
        int headerWidth = Stream.of(LINE_ID, LINE_NAME, LINE_ARTIST, LINE_BLOCKS)
            .mapToInt(this.textRenderer::getWidth)
            .max()
            .orElse(1);
        this.idLabel = this.textLine(textSection, headerWidth, LINE_ID, this.painting.id());
        this.nameLabel = this.textLine(textSection, headerWidth, LINE_NAME, this.painting.name());
        this.artistLabel = this.textLine(textSection, headerWidth, LINE_ARTIST, this.painting.artist());
        this.blocksLabel = this.textLine(textSection, headerWidth, LINE_BLOCKS, this.getBlocksText());
        this.layout.add(textSection, (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });

        this.layout.add(IconButtonWidget.builder(BuiltinIcon.SLIDERS_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.edit"))
            .onPress((button) -> this.editCallback.accept(this.paintingIndex))
            .build());

        this.layout.add(new EmptyWidget());

        this.indexLabel = this.layout
            .add(LabelWidget.builder(this.textRenderer, Text.of(String.format("%d", this.index + 1)))
                .hideBackground()
                .showShadow()
                .alignTextRight()
                .build());

        LinearLayoutWidget moveControls = LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING / 2);
        this.moveUpButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.UP_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.up"))
            .onPress((button) -> this.moveUpCallback.accept(this.paintingIndex))
            .build());
        this.moveUpButton.active = this.paintingIndex > 0;
        this.moveDownButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.DOWN_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.down"))
            .onPress((button) -> this.moveDownCallback.accept(this.paintingIndex))
            .build());
        this.moveDownButton.active = this.paintingIndex < totalCount - 1;
        this.layout.add(moveControls);

        this.addLayout(this.layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        this.layout.forEachChild(this::addDrawableChild);
      }

      private LabelWidget textLine(
          LinearLayoutWidget textSection,
          int headerWidth,
          Text header,
          String value) {
        return this.textLine(textSection, headerWidth, header, Text.of(value));
      }

      private LabelWidget textLine(
          LinearLayoutWidget textSection,
          int headerWidth,
          Text header,
          Text value) {
        LinearLayoutWidget line = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING / 2);
        line.add(LabelWidget.builder(this.textRenderer, header)
            .hideBackground()
            .showShadow()
            .width(headerWidth)
            .color(Colors.LIGHT_GRAY)
            .build());
        LabelWidget valueLabel = line.add(
            LabelWidget.builder(this.textRenderer, value)
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(),
            (parent, self) -> {
              self.setWidth(parent.getWidth() - parent.getSpacing() - headerWidth);
            });
        textSection.add(line, (parent, self) -> {
          self.setWidth(parent.getWidth());
        });
        return valueLabel;
      }

      private Text getBlocksText() {
        return Text.translatable(
            "custompaintings.editor.editor.paintings.blocks.value",
            this.painting.blockWidth(),
            this.painting.blockHeight());
      }

      public void setData(
          PackData.Painting painting,
          int paintingIndex,
          int indexWidth,
          int totalCount) {
        this.painting = painting;
        this.paintingIndex = paintingIndex;

        this.idLabel.setText(Text.of(this.painting.id()));
        this.nameLabel.setText(Text.of(this.painting.name()));
        this.artistLabel.setText(Text.of(this.painting.artist()));
        this.blocksLabel.setText(Text.of(this.getBlocksText()));
        this.imageButton.setImage(this.painting.image());
        this.moveUpButton.active = this.paintingIndex > 0;
        this.moveDownButton.active = this.paintingIndex < totalCount - 1;
        this.indexLabel.batchUpdates(() -> {
          this.indexLabel.setText(Text.of(String.format("%d", this.paintingIndex + 1)));
          this.indexLabel.setWidth(indexWidth);
        });

        this.refreshPositions();
      }

      public PackData.Painting getPainting() {
        return this.painting;
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          TextRenderer textRenderer,
          Consumer<Integer> imageCallback,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
          int paintingIndex,
          int totalCount) {
        return (index, left, top, width) -> new Entry(
            textRenderer,
            index,
            left,
            top,
            width,
            imageCallback,
            editCallback,
            moveUpCallback,
            moveDownCallback,
            painting,
            paintingIndex,
            totalCount);
      }
    }
  }
}
