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
import net.minecraft.client.gui.Element;
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
import java.util.Objects;
import java.util.Optional;

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingChangeListener {
  protected static final int BUTTON_WIDTH = 150;
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_SPACING = GuiUtil.PADDING * 2;

  private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private boolean initialized = false;
  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private LabelWidget infoLabel;
  private PaintingSpriteWidget paintingSprite;
  private IconButtonWidget prevButton;
  private LabelWidget controlsLabel;
  private IconButtonWidget nextButton;
  private ButtonWidget doneButton;
  private ArrayList<PaintingData> paintings = new ArrayList<>();
  private boolean hasHiddenPaintings = false;
  private PaintingData paintingData;
  private boolean canStay = true;

  public PaintingSelectScreen(PaintingEditState state) {
    super(generateTitle(state), state);

    this.paintingData = state.getCurrentPainting();
  }

  @Override
  public void init() {
    this.applyFilters();
    this.onPaintingChange(this.state.getCurrentPainting());

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

    this.searchBox = searchRow.add(
        new TextFieldWidget(this.textRenderer, 0, BUTTON_HEIGHT, Text.translatable("custompaintings.painting.search")),
        (parent, self) -> self.setWidth(parent.getWidth() - parent.getSpacing() - IconButtonWidget.SIZE_V)
    );
    this.searchBox.setText(this.state.getFilters().getSearch());
    this.searchBox.setChangedListener(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.FILTER_18, CustomPaintingsMod.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Text.translatable("custompaintings.painting.filter"))
        .onPress(this::filterButtonPressed)
        .build());

    this.paintingList = leftPane.add(
        new PaintingListWidget(this.state, this.client, this.state::setCurrentPainting, this::saveSelection),
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

    ArrayList<Text> infoLines = new ArrayList<>();
    if (this.paintingData.hasLabel()) {
      infoLines.add(this.getLabelText());
    }
    infoLines.add(this.getIdText());
    infoLines.add(this.getDimensionsText());
    this.infoLabel = rightPane.add(LabelWidget.builder(this.textRenderer, infoLines)
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

    this.paintingSprite = rightPane.add(new PaintingSpriteWidget(this.paintingData, true), (parent, self) -> {
      self.setDimensions(parent.getWidth() - 2 * GuiUtil.PADDING,
          parent.getHeight() - this.infoLabel.getHeight() - IconButtonWidget.SIZE_V - 2 * parent.getSpacing()
      );
    });

    LinearLayoutWidget controlsRow = rightPane.add(LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING),
        (parent, self) -> self.setDimensions(parent.getWidth() - 4 * GuiUtil.PADDING, IconButtonWidget.SIZE_V)
    );
    controlsRow.getMainPositioner().alignVerticalCenter();

    this.prevButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.PREV_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.previous"))
            .onPress((button) -> this.previousPainting())
            .build());

    this.controlsLabel = controlsRow.add(LabelWidget.builder(this.textRenderer, this.getControlsText())
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

    this.nextButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.NEXT_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.next"))
            .onPress((button) -> this.nextPainting())
            .build());

    this.prevButton.active = this.hasMultiplePaintings();
    this.nextButton.active = this.hasMultiplePaintings();

    DirectionalLayoutWidget row = DirectionalLayoutWidget.horizontal().spacing(BUTTON_SPACING);
    this.layout.addFooter(row);

    row.add(ButtonWidget.builder(this.state.hasMultipleGroups() ? ScreenTexts.BACK : ScreenTexts.CANCEL, (button) -> {
      if (this.state.hasMultipleGroups()) {
        Objects.requireNonNull(this.client).setScreen(new GroupSelectScreen(this.state));
      } else {
        this.saveEmpty();
      }
    }).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());
    this.doneButton = row.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
      this.saveCurrentSelection();
    }).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());

    if (!this.canStay) {
      this.doneButton.setTooltip(Tooltip.of(
          Text.translatable("custompaintings.painting.big", this.paintingData.width(), this.paintingData.height())));
    } else {
      this.doneButton.setTooltip(null);
    }

    this.layout.forEachChild(this::addDrawableChild);

    this.initialized = true;
    this.updateWidgetsAfterFilterChange();
    this.updateWidgetsAfterPaintingChange();

    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void onPaintingChange(PaintingData paintingData) {
    this.paintingData = paintingData;
    this.canStay = this.state.canStay(paintingData);

    this.updateWidgetsAfterPaintingChange();
  }

  private void updateWidgetsAfterPaintingChange() {
    if (!this.initialized) {
      return;
    }

    this.paintingList.selectPainting(this.paintingData);

    ArrayList<Text> infoLines = new ArrayList<>();
    if (this.paintingData.isEmpty()) {
      infoLines.add(Text.translatable("custompaintings.painting.none")
          .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)));
    } else {
      if (this.paintingData.hasLabel()) {
        infoLines.add(this.getLabelText());
      }
      infoLines.add(this.getIdText());
      infoLines.add(this.getDimensionsText());
    }
    this.infoLabel.setText(infoLines);
    this.infoLabel.setHeight(this.infoLabel.getTextBounds().getHeight());

    this.paintingSprite.setPaintingData(this.paintingData);
    this.paintingSprite.setActive(this.canStay);

    this.controlsLabel.setText(this.getControlsText());

    this.doneButton.active = this.canStay;
    if (!this.canStay) {
      this.doneButton.setTooltip(Tooltip.of(
          Text.translatable("custompaintings.painting.big", this.paintingData.width(), this.paintingData.height())));
    } else {
      this.doneButton.setTooltip(null);
    }
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
        this.previousPainting();
        return true;
      }
      case GLFW.GLFW_KEY_RIGHT -> {
        if (!this.state.hasMultiplePaintings() || !hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.nextPainting();
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
      case GLFW.GLFW_KEY_ENTER -> {
        if (!this.paintingList.isFocused()) {
          break;
        }
        Optional<PaintingData> painting = this.paintingList.getSelectedPainting();
        if (painting.isPresent() && this.state.canStay(painting.get())) {
          GuiUtil.playClickSound();
          this.saveSelection(painting.get());
          this.close();
          return true;
        }
      }
      case GLFW.GLFW_KEY_F -> {
        if (hasControlDown()) {
          if (hasShiftDown()) {
            Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
            return true;
          }

          Element focused = this.getFocused();
          if (focused != this.searchBox) {
            if (focused != null) {
              focused.setFocused(false);
            }

            this.setFocused(this.searchBox);
            return true;
          }
        }
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    return false;
  }

  private void applyFilters() {
    if (!this.state.getFilters().hasFilters()) {
      this.hasHiddenPaintings = false;
      this.paintings = this.state.getCurrentGroup().paintings();

      this.updateWidgetsAfterFilterChange();
      return;
    }

    // Manually iterate to guarantee order
    this.paintings = new ArrayList<>();
    this.state.getCurrentGroup().paintings().forEach((paintingData) -> {
      if (this.state.getFilters().test(paintingData)) {
        this.paintings.add(paintingData);
      }
    });

    this.hasHiddenPaintings = this.paintings.size() < this.state.getCurrentGroup().paintings().size();
    if (this.hasHiddenPaintings) {
      this.paintings.add(PaintingData.EMPTY);
    }

    this.updateWidgetsAfterFilterChange();
  }

  private void updateWidgetsAfterFilterChange() {
    if (!this.initialized) {
      return;
    }

    this.paintingList.setPaintings(this.paintings);
    if (!this.paintings.isEmpty() && this.state.getCurrentPainting().isEmpty()) {
      this.paintingList.selectFirst();
    }

    this.prevButton.active = this.hasMultiplePaintings();
    this.nextButton.active = this.hasMultiplePaintings();
  }

  private boolean hasMultiplePaintings() {
    return this.paintings.size() > (this.hasHiddenPaintings ? 2 : 1);
  }

  private void previousPainting() {
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

  private void nextPainting() {
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

  private void onSearchBoxChanged(String text) {
    if (this.state.getFilters().getSearch().equals(text)) {
      return;
    }

    this.state.getFilters().setSearch(text);
    this.applyFilters();
  }

  private static Text generateTitle(PaintingEditState state) {
    MutableText title = Text.translatable("custompaintings.painting.title");
    if (state.hasMultipleGroups()) {
      title = Text.literal(state.getCurrentGroup().name() + " - ").append(title);
    }
    return title;
  }

  private void filterButtonPressed(ButtonWidget button) {
    Objects.requireNonNull(this.client).setScreen(new FiltersScreen(this.state));
  }

  private Text getLabelText() {
    if (!this.paintingData.hasLabel()) {
      return Text.empty();
    }
    return this.paintingData.getLabel();
  }

  private Text getIdText() {
    MutableText idText = Text.literal("(" + this.paintingData.id() + ")");
    if (this.paintingData.hasLabel()) {
      idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
    }
    return idText;
  }

  private Text getDimensionsText() {
    return Text.translatable(
        "custompaintings.painting.dimensions", this.paintingData.width(), this.paintingData.height());
  }

  private Text getControlsText() {
    Group currentGroup = this.state.getCurrentGroup();
    int currentPaintingIndex = currentGroup.paintings().indexOf(this.paintingData);
    return Text.translatable("custompaintings.painting.number", currentPaintingIndex + 1,
        currentGroup.paintings().size()
    );
  }
}
