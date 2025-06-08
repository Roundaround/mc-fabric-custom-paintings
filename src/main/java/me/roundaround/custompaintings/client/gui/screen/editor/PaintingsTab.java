package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.util.IntRect;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.DrawableWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.util.Observable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
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
        if (paintings.size() > this.getEntryCount()) {
          for (int i = this.getEntryCount(); i < paintings.size(); i++) {
            this.addEntry(Entry.factory(this.client.textRenderer, paintings.get(i)));
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
      private final LabelWidget nameLabel;

      private PackData.Painting painting;

      public Entry(TextRenderer textRenderer, int index, int left, int top, int width, PackData.Painting painting) {
        super(index, left, top, width, 36);
        this.painting = painting;

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.nameLabel = layout.add(LabelWidget.builder(textRenderer, Text.of(this.painting.name()))
            .hideBackground()
            .showShadow()
            .build());

        layout.add(new DrawableWidget() {
          @Override
          public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            Image image = Entry.this.painting.image();

            int width = this.getWidth();
            int height = this.getHeight();
            int x = this.getX();
            int y = this.getY();

            int imageWidth = image == null ? 32 : image.width();
            int imageHeight = image == null ? 32 : image.height();

            float scale = Math.min((float) width / imageWidth, (float) height / imageHeight);
            int scaledWidth = Math.round(scale * imageWidth);
            int scaledHeight = Math.round(scale * imageHeight);

            IntRect bounds = IntRect.byDimensions(
                x + (width - scaledWidth) / 2,
                y + (height - scaledHeight) / 2,
                scaledWidth,
                scaledHeight);

            context.drawTexture(
                RenderLayer::getGuiTextured,
                State.getImageTextureId(image),
                bounds.left(),
                bounds.top(),
                0,
                0,
                bounds.getWidth(),
                bounds.getHeight(),
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight);
          }
        }, (parent, self) -> {
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
      }

      public static FlowListWidget.EntryFactory<Entry> factory(TextRenderer textRenderer, PackData.Painting painting) {
        return (index, left, top, width) -> new Entry(textRenderer, index, left, top, width, painting);
      }
    }
  }
}
