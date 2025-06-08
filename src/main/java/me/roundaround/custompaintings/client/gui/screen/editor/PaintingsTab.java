package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.function.Consumer;

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
import net.minecraft.text.Text;

public class PaintingsTab extends PackEditorTab {

  public PaintingsTab(@NotNull MinecraftClient client, @NotNull State state) {
    super(client, state, Text.translatable("custompaintings.editor.editor.tab.paintings.title"));

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
      this.setContentPadding(2 * GuiUtil.PADDING);

      observable.subscribe((paintings) -> {
        int count = paintings.size();

        if (count > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < paintings.size(); i++) {
            this.addEntry(Entry.factory(
                this.client.textRenderer,
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

        for (int i = 0; i < count; i++) {
          this.getEntry(i).setPainting(paintings.get(i));
          this.getEntry(i).setTotalCount(count);
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
      private final Consumer<Integer> editCallback;
      private final Consumer<Integer> moveUpCallback;
      private final Consumer<Integer> moveDownCallback;
      private final LabelWidget nameLabel;
      private final ImageButtonWidget imageButton;
      private final IconButtonWidget moveDownButton;
      private final LabelWidget indexLabel;

      private PackData.Painting painting;
      private int totalCount;

      public Entry(
          TextRenderer textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> editCallback,
          Consumer<Integer> moveUpCallback,
          Consumer<Integer> moveDownCallback,
          PackData.Painting painting,
          int totalCount) {
        super(index, left, top, width, 36);
        this.editCallback = editCallback;
        this.moveUpCallback = moveUpCallback;
        this.moveDownCallback = moveDownCallback;
        this.painting = painting;
        this.totalCount = totalCount;

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.nameLabel = layout.add(LabelWidget.builder(textRenderer, Text.of(this.painting.name()))
            .hideBackground()
            .showShadow()
            .build());

        this.imageButton = layout.add(new ImageButtonWidget(
            (button) -> this.editCallback.accept(index),
            this.painting.image()), (parent, self) -> {
              self.setDimensions(this.getContentHeight(), this.getContentHeight());
            });

        layout.add(IconButtonWidget.builder(BuiltinIcon.SLIDERS_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.tab.paintings.edit"))
            .onPress((button) -> this.editCallback.accept(index))
            .build());

        LinearLayoutWidget moveControls = LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING / 2);

        IconButtonWidget moveUpButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.UP_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.tab.paintings.move_up"))
            .onPress((button) -> this.moveUpCallback.accept(index))
            .build());
        moveUpButton.active = index > 0;

        this.moveDownButton = moveControls.add(IconButtonWidget.builder(BuiltinIcon.DOWN_9, Constants.MOD_ID)
            .small()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.tab.paintings.move_down"))
            .onPress((button) -> this.moveDownCallback.accept(index))
            .build());
        this.moveDownButton.active = index < totalCount - 1;

        layout.add(moveControls);

        this.indexLabel = layout.add(LabelWidget.builder(textRenderer, this.getIndexText())
            .hideBackground()
            .showShadow()
            .build());

        this.addLayout(layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        layout.forEachChild(this::addDrawableChild);
      }

      public void setPainting(PackData.Painting painting) {
        this.painting = painting;
        this.nameLabel.setText(Text.of(this.painting.name()));
        this.imageButton.setImage(this.painting.image());

        this.refreshPositions();
      }

      public PackData.Painting getPainting() {
        return this.painting;
      }

      public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        this.moveDownButton.active = totalCount > 1;
        this.indexLabel.setText(this.getIndexText());
      }

      private Text getIndexText() {
        return Text.of(String.format("%d/%d", this.index + 1, this.totalCount));
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          TextRenderer textRenderer,
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
            editCallback,
            moveUpCallback,
            moveDownCallback,
            painting,
            totalCount);
      }
    }
  }
}
