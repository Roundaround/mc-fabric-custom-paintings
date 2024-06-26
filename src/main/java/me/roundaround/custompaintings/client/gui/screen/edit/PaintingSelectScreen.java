package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.PaintingEditState.PaintingChangeListener;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingSpriteWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import me.roundaround.roundalib.client.gui.widget.LinearLayoutWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Divider;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingChangeListener {
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_SPACING = GuiUtil.PADDING * 2;

  private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private List<PaintingData> paintings = List.of();
  private boolean hasHiddenPaintings = false;

  private Runnable paintingsListChangedHandler = () -> {
  };
  private Runnable selectionChangedHandler = () -> {
  };
  private Runnable focusSearchInput = () -> {
  };

  public PaintingSelectScreen(PaintingEditState state) {
    super(getTitleText(state), state);

    this.applyFilters();
  }

  @Override
  public void init() {
    this.layout.addHeader(this.title, this.textRenderer);

    LinearLayoutWidget body = this.layout.addBody(LinearLayoutWidget.horizontal((self) -> {
      self.setPosition(0, this.layout.getHeaderHeight());
      self.setDimensions(this.width, this.layout.getContentHeight());
    }).spacing(2 * GuiUtil.PADDING));

    LinearLayoutWidget leftPane = body.add(LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING), (parent, self) -> {
      Divider divider = new Divider(parent.getWidth() - parent.getSpacing(), 2);
      self.setDimensions(divider.nextInt(), parent.getHeight());
    });
    leftPane.getMainPositioner().alignRight();

    LinearLayoutWidget searchRow = leftPane.add(LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING),
        (parent, self) -> self.setDimensions(parent.getWidth() - GuiUtil.PADDING, BUTTON_HEIGHT),
        Positioner::alignVerticalCenter
    );

    TextFieldWidget searchBox = searchRow.add(
        new TextFieldWidget(this.textRenderer, 0, BUTTON_HEIGHT, Text.translatable("custompaintings.painting.search")),
        (parent, self) -> self.setWidth(parent.getWidth() - parent.getSpacing() - IconButtonWidget.SIZE_V)
    );
    searchBox.setText(this.state.getFilters().getSearch());
    searchBox.setChangedListener(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.FILTER_18, CustomPaintingsMod.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Text.translatable("custompaintings.painting.filter"))
        .onPress(this::filterButtonPressed)
        .build());

    PaintingListWidget paintingList = leftPane.add(new PaintingListWidget(
            this.client, this.state, this.paintings, this.state::setCurrentPainting, this::saveSelection),
        (parent, self) -> {
          self.setDimensions(parent.getWidth(), parent.getHeight() - parent.getSpacing() - BUTTON_HEIGHT);
        }
    );

    LinearLayoutWidget rightPane = body.add(LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING), (parent, self) -> {
      Divider divider = new Divider(parent.getWidth() - parent.getSpacing(), 2);
      divider.skip(1);
      self.setDimensions(divider.nextInt(), parent.getHeight());
    });
    rightPane.getMainPositioner().alignHorizontalCenter();

    LabelWidget infoLabel = rightPane.add(LabelWidget.builder(this.textRenderer, this.getInfoLines())
        .justifiedCenter()
        .alignedMiddle()
        .hideBackground()
        .showShadow()
        .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
        .lineSpacing(1)
        .build(), (parent, self) -> {
      self.setDimensions(parent.getWidth(),
          LabelWidget.getDefaultHeight(this.textRenderer, 3, LabelWidget.DEFAULT_PADDING.getVertical(), 1)
      );
    });

    PaintingSpriteWidget paintingSprite = rightPane.add(
        new PaintingSpriteWidget(this.state.getCurrentPainting(), true), (parent, self) -> {
          self.setDimensions(parent.getWidth() - 2 * GuiUtil.PADDING,
              parent.getHeight() - infoLabel.getHeight() - IconButtonWidget.SIZE_V - 2 * parent.getSpacing()
          );
        });

    LinearLayoutWidget controlsRow = rightPane.add(LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING),
        (parent, self) -> self.setDimensions(parent.getWidth() - 4 * GuiUtil.PADDING, IconButtonWidget.SIZE_V)
    );
    controlsRow.getMainPositioner().alignVerticalCenter();

    IconButtonWidget prevButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.PREV_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.previous"))
            .onPress((button) -> this.selectPreviousPainting())
            .build());

    LabelWidget controlsLabel = controlsRow.add(LabelWidget.builder(this.textRenderer, this.getControlsText())
            .justifiedCenter()
            .alignedMiddle()
            .hideBackground()
            .showShadow()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .build(),
        (parent, self) -> self.setDimensions(parent.getWidth() - 2 * (GuiUtil.PADDING + IconButtonWidget.SIZE_V),
            parent.getHeight()
        )
    );

    IconButtonWidget nextButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.NEXT_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.next"))
            .onPress((button) -> this.selectNextPainting())
            .build());

    DirectionalLayoutWidget row = DirectionalLayoutWidget.horizontal().spacing(BUTTON_SPACING);
    this.layout.addFooter(row);

    if (this.state.hasMultipleGroups()) {
      row.add(ButtonWidget.builder(ScreenTexts.BACK, (button) -> {
        Objects.requireNonNull(this.client).setScreen(new GroupSelectScreen(this.state));
      }).build());
    } else {
      row.add(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
        this.saveEmpty();
      }).build());
    }

    ButtonWidget doneButton = row.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
      this.saveCurrentSelection();
    }).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();

    this.paintingsListChangedHandler = () -> {
      paintingList.setPaintings(this.paintings);
      if (!this.paintings.isEmpty() && this.state.getCurrentPainting().isEmpty()) {
        paintingList.selectFirst();
      }

      prevButton.active = this.hasMultiplePaintings();
      nextButton.active = this.hasMultiplePaintings();
    };

    this.selectionChangedHandler = () -> {
      PaintingData paintingData = this.state.getCurrentPainting();
      boolean canStay = this.state.canStay();

      paintingList.selectPainting(paintingData);

      infoLabel.batchUpdates(() -> {
        infoLabel.setText(this.getInfoLines());
        infoLabel.setHeight(infoLabel.getDefaultHeight());
      });

      paintingSprite.batchUpdates(() -> {
        paintingSprite.setPaintingData(paintingData);
        paintingSprite.setActive(canStay);
        paintingSprite.setTooltip(canStay ? null : Tooltip.of(getTooBigText(paintingData)));
      });

      controlsLabel.setText(this.getControlsText());

      doneButton.active = canStay;
      doneButton.setTooltip(canStay ? null : Tooltip.of(getTooBigText(paintingData)));
    };

    this.focusSearchInput = () -> {
      if (this.getFocused() != searchBox) {
        this.setFocused(searchBox);
      }
    };

    this.paintingsListChangedHandler.run();
    this.selectionChangedHandler.run();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT -> {
        if (!this.state.hasMultiplePaintings() || !hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.selectPreviousPainting();
        return true;
      }
      case GLFW.GLFW_KEY_RIGHT -> {
        if (!this.state.hasMultiplePaintings() || !hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.selectNextPainting();
        return true;
      }
      case GLFW.GLFW_KEY_ESCAPE -> {
        if (this.state.hasMultipleGroups()) {
          Objects.requireNonNull(this.client).setScreen(new GroupSelectScreen(this.state));
        } else {
          this.saveEmpty();
          this.close();
        }
        return true;
      }
      case GLFW.GLFW_KEY_F -> {
        if (hasControlDown()) {
          if (hasShiftDown()) {
            Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
            return true;
          }

          this.focusSearchInput.run();
        }
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  private void applyFilters() {
    if (!this.state.getFilters().hasFilters()) {
      this.hasHiddenPaintings = false;
      this.setPaintings(this.state.getCurrentGroup().paintings());
      return;
    }

    // Manually iterate to guarantee order
    ArrayList<PaintingData> paintings = new ArrayList<>();
    this.state.getCurrentGroup().paintings().forEach((paintingData) -> {
      if (this.state.getFilters().test(paintingData)) {
        paintings.add(paintingData);
      }
    });

    this.hasHiddenPaintings = paintings.size() < this.state.getCurrentGroup().paintings().size();
    if (this.hasHiddenPaintings) {
      paintings.add(PaintingData.EMPTY);
    }

    this.setPaintings(paintings);
  }

  public void setPaintings(List<PaintingData> paintings) {
    this.paintings = List.copyOf(paintings);

    if (this.paintings.isEmpty()) {
      this.state.setCurrentPainting(PaintingData.EMPTY);
    } else if (!this.paintings.contains(this.state.getCurrentPainting())) {
      this.state.setCurrentPainting(this.paintings.getFirst());
    }

    this.paintingsListChangedHandler.run();
  }

  private void onSearchBoxChanged(String text) {
    if (this.state.getFilters().getSearch().equals(text)) {
      return;
    }

    this.state.getFilters().setSearch(text);
    this.applyFilters();
  }

  @Override
  public void onPaintingChange(PaintingData paintingData) {
    this.selectionChangedHandler.run();
  }

  private boolean hasMultiplePaintings() {
    return this.paintings.size() > (this.hasHiddenPaintings ? 2 : 1);
  }

  private void selectPreviousPainting() {
    if (!this.hasMultiplePaintings()) {
      return;
    }

    this.state.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      if (currentIndex == -1) {
        currentIndex = 0;
      }
      int nextIndex = this.paintings.size() + currentIndex - 1;
      if (this.hasHiddenPaintings && nextIndex == this.paintings.size() - 1) {
        nextIndex--;
      }
      return this.paintings.get(nextIndex % this.paintings.size());
    });
  }

  private void selectNextPainting() {
    if (!this.hasMultiplePaintings()) {
      return;
    }

    this.state.setCurrentPainting((currentPainting) -> {
      int currentIndex = this.paintings.indexOf(currentPainting);
      int nextIndex = currentIndex + 1;
      if (this.hasHiddenPaintings && nextIndex == this.paintings.size() - 1) {
        nextIndex++;
      }
      return this.paintings.get(nextIndex % this.paintings.size());
    });
  }

  private void saveCurrentSelection() {
    Group currentGroup = this.state.getCurrentGroup();
    PaintingData currentPainting = this.state.getCurrentPainting();
    if (currentGroup == null || currentPainting == null) {
      this.saveEmpty();
    }
    this.saveSelection(currentPainting);
  }

  private void filterButtonPressed(ButtonWidget button) {
    Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
  }

  private List<Text> getInfoLines() {
    PaintingData paintingData = this.state.getCurrentPainting();

    if (paintingData.isEmpty()) {
      return List.of(Text.translatable("custompaintings.painting.none")
          .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)));
    }

    ArrayList<Text> infoLines = new ArrayList<>();
    if (paintingData.hasLabel()) {
      infoLines.add(getLabelText(paintingData));
    }
    infoLines.add(getIdText(paintingData));
    infoLines.add(getDimensionsText(paintingData));
    return infoLines;
  }

  private Text getControlsText() {
    if (this.paintings.isEmpty()) {
      return Text.empty();
    }

    Group currentGroup = this.state.getCurrentGroup();
    int currentPaintingIndex = currentGroup.paintings().indexOf(this.state.getCurrentPainting());
    return Text.translatable("custompaintings.painting.number", currentPaintingIndex + 1,
        currentGroup.paintings().size()
    );
  }

  private static Text getTitleText(PaintingEditState state) {
    MutableText title = Text.translatable("custompaintings.painting.title");
    if (state.hasMultipleGroups()) {
      title = Text.literal(state.getCurrentGroup().name() + " - ").append(title);
    }
    return title;
  }

  private static Text getLabelText(PaintingData paintingData) {
    if (!paintingData.hasLabel()) {
      return Text.empty();
    }
    return paintingData.getLabel();
  }

  private static Text getIdText(PaintingData paintingData) {
    MutableText idText = Text.literal("(" + paintingData.id() + ")");
    if (paintingData.hasLabel()) {
      idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
    }
    return idText;
  }

  private static Text getDimensionsText(PaintingData paintingData) {
    return Text.translatable("custompaintings.painting.dimensions", paintingData.width(), paintingData.height());
  }

  private static Text getTooBigText(PaintingData paintingData) {
    return Text.translatable("custompaintings.painting.big", paintingData.width(), paintingData.height());
  }
}
