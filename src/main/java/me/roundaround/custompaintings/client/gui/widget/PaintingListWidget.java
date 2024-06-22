package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.Coords;
import me.roundaround.roundalib.client.gui.layout.Spacing;
import me.roundaround.roundalib.client.gui.widget.AlwaysSelectedFlowListWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import me.roundaround.roundalib.client.gui.widget.LinearLayoutWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class PaintingListWidget extends AlwaysSelectedFlowListWidget<PaintingListWidget.Entry> {
  private final PaintingEditState state;
  private final Consumer<PaintingData> onPaintingSelect;
  private final Consumer<PaintingData> onPaintingConfirm;

  private Identifier clickedId = null;
  private long clickedTime = 0L;

  public PaintingListWidget(
      PaintingEditState state,
      MinecraftClient client,
      Consumer<PaintingData> onPaintingSelect,
      Consumer<PaintingData> onPaintingConfirm
  ) {
    super(client, 0, 0, 0, 0);

    this.state = state;
    this.onPaintingSelect = onPaintingSelect;
    this.onPaintingConfirm = onPaintingConfirm;
  }

  public void setPaintings(ArrayList<PaintingData> paintings) {
    this.clearEntries();

    boolean selected = false;

    for (PaintingData paintingData : paintings) {
      Entry entry = this.addEntry((index, left, top, width) -> {
        if (paintingData.isEmpty()) {
          return new EmptyEntry(index, left, top, width, this.client.textRenderer);
        }
        return new PaintingEntry(index, left, top, width, this.client.textRenderer, paintingData,
            this.state.canStay(paintingData), this.onPaintingSelect, this.onPaintingConfirm
        );
      });

      if (!paintingData.isEmpty() && this.state.getCurrentPainting().id() == paintingData.id()) {
        this.setSelected(entry);
        selected = true;
      }
    }

    if (!selected) {
      this.selectFirst();
    }
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);
    this.state.setCurrentPainting(entry.getPaintingData());
  }

  public Optional<PaintingData> getSelectedPainting() {
    Entry selected = this.getSelected();
    if (selected == null) {
      return Optional.empty();
    }
    return Optional.of(selected.paintingData);
  }

  public void selectPainting(PaintingData paintingData) {
    Optional<PaintingData> selected = this.getSelectedPainting();
    if (selected.isPresent() && selected.get().id() == paintingData.id()) {
      return;
    }

    for (Element child : this.children()) {
      if (child instanceof Entry entry && entry.paintingData.id() == paintingData.id()) {
        this.setSelected(entry);
        this.ensureVisible(entry);
        return;
      }
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    Entry entry = this.getEntryAtPosition(mouseX, mouseY);
    if (entry != null) {
      if (entry.getPaintingData().id().equals(this.clickedId) && Util.getMeasuringTimeMs() - this.clickedTime < 250L) {
        return entry.mouseDoubleClicked(mouseX, mouseY, button);
      }

      this.clickedId = entry.getPaintingData().id();
      this.clickedTime = Util.getMeasuringTimeMs();
    }

    return super.mouseClicked(mouseX, mouseY, button) || entry != null;
  }

  @Override
  protected Entry getNeighboringEntry(NavigationDirection direction) {
    return this.getNeighboringEntry(direction, (entry) -> !entry.getPaintingData().isEmpty());
  }

  @Environment(value = EnvType.CLIENT)
  public static abstract class Entry extends AlwaysSelectedFlowListWidget.Entry {
    protected static final int HEIGHT = 36;

    protected final PaintingData paintingData;

    public Entry(int index, int left, int top, int width, int contentHeight, PaintingData paintingData) {
      super(index, left, top, width, contentHeight);
      this.paintingData = paintingData;
    }

    public PaintingData getPaintingData() {
      return this.paintingData;
    }

    public boolean mouseDoubleClicked(double mouseX, double mouseY, int button) {
      return false;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class PaintingEntry extends Entry {
    private final Consumer<PaintingData> onSelect;
    private final Consumer<PaintingData> onConfirm;
    private final boolean canStay;
    private final LinearLayoutWidget layout;

    public PaintingEntry(
        int index,
        int left,
        int top,
        int width,
        TextRenderer textRenderer,
        PaintingData paintingData,
        boolean canStay,
        Consumer<PaintingData> onSelect,
        Consumer<PaintingData> onConfirm
    ) {
      super(index, left, top, width, HEIGHT, paintingData);

      this.setMargin(Spacing.of(GuiUtil.PADDING / 2, GuiUtil.PADDING));

      this.onSelect = onSelect;
      this.onConfirm = onConfirm;
      this.canStay = canStay;

      this.layout = LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING);
      this.layout.setPosition(this.getContentLeft(), this.getContentTop());
      this.layout.setDimensions(this.getContentWidth(), this.getContentHeight());

      this.layout.add(new PaintingSpriteWidget(paintingData), (parent, self) -> {
        self.setDimensions(this.getContentHeight(), this.getContentHeight());
      });

      ArrayList<Text> lines = new ArrayList<>();
      if (paintingData.hasLabel()) {
        lines.add(this.getPaintingData().getLabel());
      }

      MutableText idText = Text.literal("(" + this.getPaintingData().id() + ")");
      if (this.getPaintingData().hasLabel()) {
        idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      }
      lines.add(idText);

      lines.add(Text.translatable("custompaintings.painting.dimensions", this.getPaintingData().width(),
          this.getPaintingData().height()
      ));

      LabelWidget labels = LabelWidget.builder(textRenderer, lines)
          .justifiedLeft()
          .alignedMiddle()
          .overflowBehavior(LabelWidget.OverflowBehavior.CLIP)
          .hideBackground()
          .showShadow()
          .build();
      this.layout.add(labels, (parent, self) -> {
        self.setDimensions(this.getContentWidth() - GuiUtil.PADDING - this.getContentHeight(), this.getContentHeight());
      });

      this.layout.forEachChild(this::addDrawableChild);
    }

    @Override
    public void refreshPositions() {
      this.layout.setPosition(this.getContentLeft(), this.getContentTop());
      this.layout.setDimensions(this.getContentWidth(), this.getContentHeight());
      this.layout.refreshPositions();
      super.refreshPositions();
    }

    @Override
    public Text getNarration() {
      return !this.paintingData.hasLabel() ?
          Text.literal(this.paintingData.id().toString()) :
          this.paintingData.getLabel();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.canStay) {
        this.onSelect.accept(this.paintingData);
        return true;
      }

      return false;
    }

    @Override
    public boolean mouseDoubleClicked(double mouseX, double mouseY, int button) {
      if (this.canStay) {
        this.onConfirm.accept(this.paintingData);
        return true;
      }

      return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ENTER && this.canStay) {
        this.onSelect.accept(this.paintingData);
        return true;
      }

      return false;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class EmptyEntry extends Entry {
    private final LabelWidget label;

    public EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
      super(index, left, top, width, HEIGHT, PaintingData.EMPTY);

      this.label = LabelWidget.builder(textRenderer, Text.translatable("custompaintings.painting.empty"))
          .refPosition(this.getContentCenterX(), this.getContentCenterY())
          .dimensions(this.getContentWidth(), this.getContentHeight())
          .justifiedCenter()
          .alignedMiddle()
          .hideBackground()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .build();

      this.addDrawableChild(this.label);
    }

    @Override
    public Text getNarration() {
      return this.label.getText();
    }

    @Override
    public void refreshPositions() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
    }
  }
}
