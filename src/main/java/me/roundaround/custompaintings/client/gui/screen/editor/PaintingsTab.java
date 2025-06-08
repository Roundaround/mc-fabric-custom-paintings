package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.util.IntRect;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.util.Observable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

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
        if (paintings.size() > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < paintings.size(); i++) {
            this.addEntry(Entry.factory(this.client.textRenderer, (index) -> {
              CustomPaintingsMod.LOGGER.info("Clicked painting {}", index);
            }, paintings.get(i)));
          }
        } else {
          for (int i = this.getEntryCount(); i > paintings.size(); i--) {
            this.removeEntry();
          }
        }

        for (int i = 0; i < this.getEntryCount(); i++) {
          this.getEntry(i).setPainting(paintings.get(i));
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

    static class Entry extends ParentElementEntryListWidget.Entry {
      private final Consumer<Integer> onClick;
      private final LabelWidget nameLabel;
      private final ImageButtonWidget imageButton;

      private PackData.Painting painting;

      public Entry(
          TextRenderer textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> onClick,
          PackData.Painting painting) {
        super(index, left, top, width, 36);
        this.onClick = onClick;
        this.painting = painting;

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.nameLabel = layout.add(LabelWidget.builder(textRenderer, Text.of(this.painting.name()))
            .hideBackground()
            .showShadow()
            .build());

        this.imageButton = layout.add(new ImageButtonWidget(
            (button) -> this.onClick.accept(index),
            this.painting.image()), (parent, self) -> {
              self.setDimensions(this.getContentHeight(), this.getContentHeight());
            });

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
      }

      public static FlowListWidget.EntryFactory<Entry> factory(TextRenderer textRenderer, Consumer<Integer> onClick,
          PackData.Painting painting) {
        return (index, left, top, width) -> new Entry(textRenderer, index, left, top, width, onClick, painting);
      }
    }
  }

  static class ImageButtonWidget extends ButtonWidget {
    protected Image image;
    protected int imageWidth;
    protected int imageHeight;
    protected IntRect imageBounds = IntRect.zero();
    protected boolean inBatchUpdate = false;

    public ImageButtonWidget(ButtonWidget.PressAction pressAction, Image image) {
      this(pressAction, image, true);
    }

    public ImageButtonWidget(ButtonWidget.PressAction pressAction, Image image, boolean immediatelyCalculateBounds) {
      super(0, 0, 0, 0, ScreenTexts.EMPTY, pressAction, DEFAULT_NARRATION_SUPPLIER);
      this.image = image;
      this.imageWidth = image == null ? 32 : image.width();
      this.imageHeight = image == null ? 32 : image.height();
      if (immediatelyCalculateBounds) {
        this.calculateBounds();
      }
    }

    public void batchUpdates(Runnable runnable) {
      this.inBatchUpdate = true;
      try {
        runnable.run();
      } finally {
        this.inBatchUpdate = false;
        this.calculateBounds();
      }
    }

    @Override
    public void setX(int x) {
      super.setX(x);
      this.calculateBounds();
    }

    @Override
    public void setY(int y) {
      super.setY(y);
      this.calculateBounds();
    }

    @Override
    public void setWidth(int width) {
      super.setWidth(width);
      this.calculateBounds();
    }

    @Override
    public void setHeight(int height) {
      super.setHeight(height);
      this.calculateBounds();
    }

    @Override
    public void setDimensions(int width, int height) {
      super.setDimensions(width, height);
      this.calculateBounds();
    }

    public void setImage(Image image) {
      this.image = image;
      this.calculateBounds();
    }

    public void calculateBounds() {
      if (this.inBatchUpdate || !this.visible) {
        return;
      }

      int width = this.getWidth() - 2;
      int height = this.getHeight() - 2;
      int x = this.getX() + 1;
      int y = this.getY() + 1;

      float scale = Math.min(
          (float) this.getWidth() / this.imageWidth,
          (float) this.getHeight() / this.imageHeight);
      int scaledWidth = Math.round(scale * this.imageWidth);
      int scaledHeight = Math.round(scale * this.imageHeight);

      this.imageBounds = IntRect.byDimensions(
          x + (width - scaledWidth) / 2,
          y + (height - scaledHeight) / 2,
          scaledWidth,
          scaledHeight);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.imageBounds.contains(mouseX, mouseY);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      this.hovered = this.hovered && this.imageBounds.contains(mouseX, mouseY);

      context.fill(
          this.imageBounds.left() - 1,
          this.imageBounds.top() - 1,
          this.imageBounds.right() + 1,
          this.imageBounds.bottom() + 1,
          this.hovered ? Colors.WHITE : Colors.BLACK);
      context.drawTexture(
          RenderLayer::getGuiTextured,
          State.getImageTextureId(this.image),
          this.imageBounds.left(),
          this.imageBounds.top(),
          0,
          0,
          this.imageBounds.getWidth(),
          this.imageBounds.getHeight(),
          this.imageWidth,
          this.imageHeight,
          this.imageWidth,
          this.imageHeight);
    }
  }
}
