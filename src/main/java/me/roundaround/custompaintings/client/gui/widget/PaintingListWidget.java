package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class PaintingListWidget extends NarratableEntryListWidget<PaintingListWidget.Entry> {
  private final PaintingEditState state;
  private final Consumer<PaintingData> onPaintingSelect;
  private final Consumer<PaintingData> onPaintingConfirm;

  private Identifier clickedId = null;
  private long clickedTime = 0L;

  public PaintingListWidget(
      MinecraftClient client,
      PaintingEditState state,
      Consumer<PaintingData> onPaintingSelect,
      Consumer<PaintingData> onPaintingConfirm
  ) {
    super(client, 0, 0, 0, 0);

    this.setAlternatingRowShading(true);
    this.setAutoPadForShading(false);

    this.state = state;
    this.onPaintingSelect = onPaintingSelect;
    this.onPaintingConfirm = onPaintingConfirm;

    this.refreshPaintings();
  }

  public void refreshPaintings() {
    this.clearEntries();

    this.state.updatePaintingList();

    for (PaintingData paintingData : this.state.getCurrentPaintings()) {
      this.addEntry((index, left, top, width) -> new PaintingEntry(index, left, top, width, this.client.textRenderer,
          paintingData, this.state.canStay(paintingData), this.onPaintingSelect, this.onPaintingConfirm
      ));
    }

    if (this.getEntryCount() == 0 || this.state.areAnyPaintingsFiltered()) {
      this.addEntry((index, left, top, width) -> new EmptyEntry(index, left, top, width, this.client.textRenderer));
    }

    this.refreshPositions();
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);
    this.state.setCurrentPainting(entry.getPaintingData());
    this.onPaintingSelect.accept(this.state.getCurrentPainting());
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
    if (selected.isPresent() && selected.get().idEquals(paintingData)) {
      return;
    }

    for (Element child : this.children()) {
      if (child instanceof Entry entry && entry.paintingData.idEquals(paintingData)) {
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
      if (entry.mouseClicked(mouseX, mouseY, button) && entry.getPaintingData().id().equals(this.clickedId) &&
          Util.getMeasuringTimeMs() - this.clickedTime < 250L) {
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
  public static abstract class Entry extends NarratableEntryListWidget.Entry {
    protected static final int HEIGHT = 36;

    protected final PaintingData paintingData;

    public Entry(int index, int left, int top, int width, int contentHeight, PaintingData paintingData) {
      super(index, left, top, width, contentHeight);
      this.paintingData = paintingData;
    }

    public PaintingData getPaintingData() {
      return this.paintingData;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
    }

    public boolean mouseDoubleClicked(double mouseX, double mouseY, int button) {
      return false;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class EmptyEntry extends Entry {
    private final LabelWidget label;

    public EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
      super(index, left, top, width, HEIGHT, PaintingData.EMPTY);

      this.label = LabelWidget.builder(textRenderer, Text.translatable("custompaintings.painting.empty"))
          .position(this.getContentCenterX(), this.getContentCenterY())
          .dimensions(this.getContentWidth(), this.getContentHeight())
          .alignSelfCenterX()
          .alignSelfCenterY()
          .hideBackground()
          .showShadow()
          .build();

      this.addDrawable(this.label);
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

  @Environment(value = EnvType.CLIENT)
  public static class PaintingEntry extends Entry {
    private final Consumer<PaintingData> onSelect;
    private final Consumer<PaintingData> onConfirm;
    private final boolean canStay;

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

      assert !paintingData.isEmpty();

      this.onSelect = onSelect;
      this.onConfirm = onConfirm;
      this.canStay = canStay;

      LinearLayoutWidget layout = this.addLayout(LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING), (self) -> {
        self.setPosition(this.getContentLeft(), this.getContentTop());
        self.setDimensions(this.getContentWidth(), this.getContentHeight());
      });

      layout.add(ImageSpriteWidget.create(paintingData), (parent, self) -> {
        self.setDimensions(this.getContentHeight(), this.getContentHeight());
        self.setActive(this.canStay);
      });

      layout.add(LabelWidget.builder(textRenderer, this.getPaintingData().getInfoLines())
          .alignTextLeft()
          .alignTextCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
          .hideBackground()
          .showShadow()
          .build(), (parent, self) -> {
        self.setDimensions(this.getContentWidth() - GuiUtil.PADDING - this.getContentHeight(), this.getContentHeight());
      });

      layout.forEachChild(this::addDrawable);
    }

    @Override
    public Text getNarration() {
      return !this.paintingData.hasLabel() ?
          Text.literal(this.paintingData.id().toString()) :
          this.paintingData.getLabel();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.onSelect.accept(this.paintingData);
      return true;
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
        GuiUtil.playClickSound();
        this.onConfirm.accept(this.paintingData);
        return true;
      }

      return false;
    }
  }
}
