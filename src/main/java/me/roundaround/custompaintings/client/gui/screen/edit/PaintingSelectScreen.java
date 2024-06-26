package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
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

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingEditState.StateChangedListener {
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_SPACING = GuiUtil.PADDING * 2;

  private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private LabelWidget infoLabel;
  private PaintingSpriteWidget paintingSprite;
  private IconButtonWidget prevButton;
  private LabelWidget controlsLabel;
  private IconButtonWidget nextButton;
  private ButtonWidget doneButton;

  public PaintingSelectScreen(PaintingEditState state) {
    super(getTitleText(state), state);
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
        new PaintingListWidget(this.client, this.state, this::onPaintingListSelect, this::saveSelection),
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

    this.infoLabel = rightPane.add(LabelWidget.builder(this.textRenderer, this.getInfoLines())
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

    this.paintingSprite = rightPane.add(
        new PaintingSpriteWidget(this.state.getCurrentPainting(), true), (parent, self) -> {
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
            .onPress((button) -> this.state.setPreviousPainting())
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
            .onPress((button) -> this.state.setNextPainting())
            .build());

    DirectionalLayoutWidget row = DirectionalLayoutWidget.horizontal().spacing(BUTTON_SPACING);
    this.layout.addFooter(row);

    row.add(ButtonWidget.builder(ScreenTexts.BACK, (button) -> {
      Objects.requireNonNull(this.client).setScreen(new GroupSelectScreen(this.state));
    }).build());

    this.doneButton = row.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
      this.saveCurrentSelection();
    }).build());

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();

    this.state.setStateChangedListener(this);
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
        if (!this.state.hasPaintingsToIterate() || !hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.state.setPreviousPainting();
        return true;
      }
      case GLFW.GLFW_KEY_RIGHT -> {
        if (!this.state.hasPaintingsToIterate() || !hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.state.setNextPainting();
        return true;
      }
      case GLFW.GLFW_KEY_ESCAPE -> {
        // TODO: Come up with a way to let escape close the screen but still make it easy to go back to group select
        this.navigate(new GroupSelectScreen(this.state));
        return true;
      }
      case GLFW.GLFW_KEY_F -> {
        if (hasControlDown()) {
          if (hasShiftDown()) {
            this.navigate(new FiltersScreen(this.state));
            return true;
          }

          if (this.getFocused() != this.searchBox) {
            this.setFocused(this.searchBox);
          }
        }
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onPaintingsListChanged() {
    this.paintingList.refreshPaintings();

    this.controlsLabel.setText(this.getControlsText());

    this.prevButton.active = this.state.hasPaintingsToIterate();
    this.nextButton.active = this.state.hasPaintingsToIterate();
  }

  @Override
  public void onCurrentPaintingChanged() {
    PaintingData paintingData = this.state.getCurrentPainting();
    boolean canStay = this.state.canStay();

    this.paintingList.selectPainting(paintingData);

    this.infoLabel.batchUpdates(() -> {
      this.infoLabel.setText(this.getInfoLines());
      this.infoLabel.setHeight(this.infoLabel.getDefaultHeight());
    });

    this.paintingSprite.batchUpdates(() -> {
      this.paintingSprite.setPaintingData(paintingData);
      this.paintingSprite.setActive(canStay);
      this.paintingSprite.setTooltip(canStay ? null : Tooltip.of(getTooBigText(paintingData)));
    });

    this.controlsLabel.setText(this.getControlsText());

    this.doneButton.active = canStay;
    this.doneButton.setTooltip(canStay ? null : Tooltip.of(getTooBigText(paintingData)));
  }

  private void onSearchBoxChanged(String text) {
    if (this.state.getFilters().getSearch().equals(text)) {
      return;
    }

    this.state.getFilters().setSearch(text);
    this.state.updatePaintingList();
  }

  private void onPaintingListSelect(PaintingData paintingData) {
    this.state.setCurrentPainting(paintingData);
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
    if (this.state.getCurrentPainting().isEmpty()) {
      return Text.empty();
    }

    Group currentGroup = this.state.getCurrentGroup();
    int currentPaintingIndex = currentGroup.paintings().indexOf(this.state.getCurrentPainting());
    return Text.translatable("custompaintings.painting.number", currentPaintingIndex + 1,
        currentGroup.paintings().size()
    );
  }

  private static Text getTitleText(PaintingEditState state) {
    return Text.literal(state.getCurrentGroup().name() + " - ")
        .append(Text.translatable("custompaintings.painting.title"));
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
