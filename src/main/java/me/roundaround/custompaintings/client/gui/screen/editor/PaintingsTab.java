package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.util.Observable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class PaintingsTab extends PackEditorTab {

  public PaintingsTab(@NotNull MinecraftClient client, @NotNull State state) {
    super(client, state, Text.translatable("custompaintings.editor.editor.paintings.title"));

    this.layout.add(new PaintingList(
        this.client,
        this.layout,
        this.state.paintings),
        (parent, self) -> {
          self.setDimensionsAndPosition(
              parent.getWidth(),
              parent.getHeight(),
              parent.getX(),
              parent.getY());
        });

    this.layout.refreshPositions();
  }

  static class PaintingList extends ParentElementEntryListWidget<PaintingList.Entry> {
    public PaintingList(MinecraftClient client, LinearLayoutWidget layout,
        Observable<List<PackData.Painting>> observable) {
      super(client, layout.getX(), layout.getY(), layout.getWidth(), layout.getHeight());
      this.setContentPadding(GuiUtil.PADDING);

      observable.subscribe((paintings) -> {
        int count = paintings.size();

        if (count > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < paintings.size(); i++) {
            this.addEntry(Entry.factory(
                this.client.textRenderer,
                this::image,
                this::edit,
                this::moveUp,
                this::moveDown,
                paintings.get(i),
                count));
          }
        } else {
          for (int i = this.getEntryCount(); i > count; i--) {
            this.removeEntry();
          }
        }

        int maxCountWidth = IntStream.range(1, count)
            .map((i) -> this.client.textRenderer.getWidth(Text.of(String.format("%d", i))))
            .max()
            .orElse(1);
        for (int i = 0; i < count; i++) {
          this.getEntry(i).setPainting(paintings.get(i));
          this.getEntry(i).setTotalCount(count, maxCountWidth);
        }
      });
    }

    @Override
    protected void renderListBackground(DrawContext context) {
      // Disable background
    }

    @Override
    protected void renderListBorders(DrawContext context) {
      // Disable borders
    }

    @Override
    protected int getPreferredContentWidth() {
      return VANILLA_LIST_WIDTH_L;
    }

    private void image(int index) {
      CustomPaintingsMod.LOGGER.info("Editing image {}", index);
    }

    private void edit(int index) {
      CustomPaintingsMod.LOGGER.info("Editing painting {}", index);
    }

    private void moveUp(int index) {
      if (index <= 0 || index >= this.getEntryCount()) {
        return;
      }

      PackData.Painting painting = this.getEntry(index).getPainting();
      PackData.Painting previousPainting = this.getEntry(index - 1).getPainting();

      this.getEntry(index).setPainting(previousPainting);
      this.getEntry(index - 1).setPainting(painting);
    }

    private void moveDown(int index) {
      if (index < 0 || index >= this.getEntryCount() - 1) {
        return;
      }

      PackData.Painting painting = this.getEntry(index).getPainting();
      PackData.Painting nextPainting = this.getEntry(index + 1).getPainting();

      this.getEntry(index).setPainting(nextPainting);
      this.getEntry(index + 1).setPainting(painting);
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
      private final IconButtonWidget moveDownButton;
      private final LabelWidget indexLabel;

      private PackData.Painting painting;

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
          int totalCount) {
        super(index, left, top, width, textRenderer.fontHeight * 4 + 3);
        this.textRenderer = textRenderer;
        this.imageCallback = imageCallback;
        this.editCallback = editCallback;
        this.moveUpCallback = moveUpCallback;
        this.moveDownCallback = moveDownCallback;
        this.painting = painting;

        this.layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.imageButton = new ImageButtonWidget(
            (button) -> this.imageCallback.accept(index),
            this.painting.image());
        this.indexLabel = LabelWidget.builder(this.textRenderer, Text.of(String.format("%d", this.index + 1)))
            .hideBackground()
            .showShadow()
            .build();

        this.layout.add(IconButtonWidget.builder(BuiltinIcon.SLIDERS_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.edit"))
            .onPress((button) -> this.editCallback.accept(index))
            .build());

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
          int textSectionWidth = this.getContentWidth();
          textSectionWidth -= (parent.getChildren().size() - 1) * parent.getSpacing();
          for (Widget widget : parent.getChildren()) {
            if (widget != self) {
              textSectionWidth -= widget.getWidth();
            }
          }
          self.setWidth(textSectionWidth);
        });

        this.layout.add(this.imageButton, (parent, self) -> {
          self.setDimensions(this.getContentHeight(), this.getContentHeight());
        });

        LinearLayoutWidget moveControls = LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING / 2);

        IconButtonWidget moveUpButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.UP_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.up"))
            .onPress((button) -> this.moveUpCallback.accept(index))
            .build());
        moveUpButton.active = index > 0;

        this.moveDownButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.DOWN_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.paintings.down"))
            .onPress((button) -> this.moveDownCallback.accept(index))
            .build());
        this.moveDownButton.active = index < totalCount - 1;

        this.layout.add(moveControls);

        this.layout.add(this.indexLabel);

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

      public void setPainting(PackData.Painting painting) {
        this.painting = painting;
        this.idLabel.setText(Text.of(this.painting.id()));
        this.nameLabel.setText(Text.of(this.painting.name()));
        this.artistLabel.setText(Text.of(this.painting.artist()));
        this.blocksLabel.setText(Text.of(this.getBlocksText()));
        this.imageButton.setImage(this.painting.image());

        this.refreshPositions();
      }

      public PackData.Painting getPainting() {
        return this.painting;
      }

      public void setTotalCount(int totalCount, int maxWidth) {
        this.moveDownButton.active = this.index < totalCount - 1;
        this.indexLabel.batchUpdates(() -> {
          this.indexLabel.setText(Text.of(String.format("%d", this.index + 1)));
          this.indexLabel.setWidth(maxWidth);
        });
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          TextRenderer textRenderer,
          Consumer<Integer> imageCallback,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
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
            totalCount);
      }
    }
  }
}
