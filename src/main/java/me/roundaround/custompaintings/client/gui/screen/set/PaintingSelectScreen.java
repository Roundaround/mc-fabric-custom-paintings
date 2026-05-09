package me.roundaround.custompaintings.client.gui.screen.set;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingSpriteWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Axis;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.mojang.math.Divisor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

public class PaintingSelectScreen extends BaseSetPaintingScreen implements PaintingEditState.StateChangedListener {
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_SPACING = GuiUtil.PADDING * 2;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  private EditBox searchBox;
  private PaintingListWidget paintingList;
  private LabelWidget infoLabel;
  private PaintingSpriteWidget paintingSprite;
  private IconButtonWidget prevButton;
  private LabelWidget controlsLabel;
  private IconButtonWidget nextButton;
  private Button doneButton;

  public PaintingSelectScreen(PaintingEditState state) {
    super(getTitleText(state), state);
  }

  @Override
  public void init() {
    this.layout.addHeader(this.font, this.title);

    this.layout.getBody().flowAxis(Axis.HORIZONTAL).spacing(0);

    LinearLayoutWidget leftPane = LinearLayoutWidget.vertical()
        .defaultOffAxisContentAlignEnd()
        .spacing(GuiUtil.PADDING);

    LinearLayoutWidget searchRow = leftPane.add(
        LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING),
        (parent, self) -> self.setDimensions(leftPane.getWidth() - GuiUtil.PADDING, BUTTON_HEIGHT)
    );

    this.searchBox = searchRow.add(
        new EditBox(this.font, 0, BUTTON_HEIGHT, Component.translatable("custompaintings.painting.search")),
        (parent, self) -> self.setWidth(parent.getWidth() - parent.getSpacing() - IconButtonWidget.SIZE_V)
    );
    this.searchBox.setResponder(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(BuiltinIcon.FILTER_18, Constants.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Component.translatable("custompaintings.painting.filter"))
        .onPress(this::filterButtonPressed)
        .build());

    this.paintingList = leftPane.add(
        new PaintingListWidget(this.minecraft, this.state, this::onPaintingListSelect, this::saveSelection),
        (parent, self) -> {
          self.setSize(parent.getWidth(), parent.getHeight() - parent.getSpacing() - BUTTON_HEIGHT);
        }
    );

    LinearLayoutWidget rightPane = LinearLayoutWidget.vertical()
        .spacing(GuiUtil.PADDING)
        .defaultOffAxisContentAlignCenter();

    this.infoLabel = rightPane.add(
        LabelWidget.builder(this.font, this.getInfoLines())
            .alignTextCenterX()
            .alignTextCenterY()
            .hideBackground()
            .showShadow()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .lineSpacing(1)
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth());
        }
    );

    this.paintingSprite = rightPane.add(
        PaintingSpriteWidget.builder(this.state.getCurrentPainting()).border(true).build(), (parent, self) -> {
          self.setSize(
              parent.getWidth() - 2 * GuiUtil.PADDING,
              parent.getHeight() - this.infoLabel.getHeight() - IconButtonWidget.SIZE_V - 2 * parent.getSpacing()
          );
        }
    );

    LinearLayoutWidget controlsRow = LinearLayoutWidget.horizontal()
        .spacing(GuiUtil.PADDING)
        .defaultOffAxisContentAlignCenter();

    this.prevButton = controlsRow.add(IconButtonWidget.builder(BuiltinIcon.PREV_18, Constants.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Component.translatable("custompaintings.painting.previous"))
        .onPress((button) -> this.state.setPreviousPainting())
        .build());

    this.controlsLabel = controlsRow.add(
        LabelWidget.builder(this.font, this.getControlsText())
            .alignTextCenterX()
            .alignTextCenterY()
            .hideBackground()
            .showShadow()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .build(), (parent, self) -> {
          self.setSize(parent.getWidth() - 2 * (GuiUtil.PADDING + IconButtonWidget.SIZE_V), parent.getHeight());
        }
    );

    this.nextButton = controlsRow.add(IconButtonWidget.builder(BuiltinIcon.NEXT_18, Constants.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Component.translatable("custompaintings.painting.next"))
        .onPress((button) -> this.state.setNextPainting())
        .build());

    rightPane.add(
        controlsRow, (parent, self) -> {
          self.setWidth(parent.getWidth());
        }
    );

    this.layout.addBody(
        leftPane, (parent, self) -> {
          Divisor divider = new Divisor(parent.getWidth() - 2 * GuiUtil.PADDING, 2);
          self.setDimensions(divider.nextInt(), parent.getHeight());
        }
    );
    this.layout.addBody(FillerWidget.ofWidth(2 * GuiUtil.PADDING));
    this.layout.addBody(
        rightPane, (parent, self) -> {
          Divisor divider = new Divisor(parent.getWidth() - 2 * GuiUtil.PADDING, 2);
          divider.skip(1);
          self.setDimensions(divider.nextInt() - GuiUtil.PADDING, parent.getHeight());
        }
    );
    this.layout.addBody(FillerWidget.ofWidth(GuiUtil.PADDING));

    this.layout.addFooter(Button.builder(
        CommonComponents.GUI_BACK, (button) -> {
          Objects.requireNonNull(this.minecraft).setScreen(new PackSelectScreen(this.state));
        }
    ).build());
    this.doneButton = this.layout.addFooter(Button.builder(
        CommonComponents.GUI_DONE, (button) -> {
          this.saveCurrentSelection();
        }
    ).build());

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();

    this.state.setStateChangedListener(this);

    this.searchBox.setValue(this.state.getFilters().getSearch());
    this.setInitialFocus(this.searchBox);
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public boolean keyPressed(KeyEvent input) {
    switch (input.input()) {
      case GLFW.GLFW_KEY_LEFT -> {
        if (!this.state.hasPaintingsToIterate() || !input.hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.state.setPreviousPainting();
        return true;
      }
      case GLFW.GLFW_KEY_RIGHT -> {
        if (!this.state.hasPaintingsToIterate() || !input.hasControlDown()) {
          break;
        }
        GuiUtil.playClickSound();
        this.state.setNextPainting();
        return true;
      }
      case GLFW.GLFW_KEY_ESCAPE -> {
        // TODO: Come up with a way to let escape close the screen but still make it easy to go back to group select
        this.navigate(new PackSelectScreen(this.state));
        return true;
      }
      case GLFW.GLFW_KEY_F -> {
        if (input.hasControlDown()) {
          if (input.hasShiftDown()) {
            this.navigate(new FiltersScreen(this.state));
            return true;
          }

          if (this.getFocused() != this.searchBox) {
            this.setFocused(this.searchBox);
          }
        }
      }
    }

    return super.keyPressed(input);
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
      this.paintingSprite.setTooltip(canStay ? null : Tooltip.create(getTooBigText(paintingData)));
    });

    this.controlsLabel.setText(this.getControlsText());

    this.doneButton.active = canStay;
    this.doneButton.setTooltip(canStay ? null : Tooltip.create(getTooBigText(paintingData)));
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
    PackData currentPack = this.state.getCurrentPack();
    PaintingData currentPainting = this.state.getCurrentPainting();
    if (currentPack == null || currentPainting == null) {
      this.saveEmpty();
      return;
    }
    this.saveSelection(currentPainting);
  }

  private void filterButtonPressed(Button button) {
    Objects.requireNonNull(this.minecraft).setScreen(new FiltersScreen(this.state));
  }

  private List<Component> getInfoLines() {
    PaintingData paintingData = this.state.getCurrentPainting();

    if (paintingData.isEmpty()) {
      return List.of(Component.translatable("custompaintings.painting.none")
          .setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY)));
    }

    return paintingData.getInfoLines();
  }

  private Component getControlsText() {
    if (this.state.getCurrentPainting().isEmpty()) {
      return Component.empty();
    }

    PackData currentPack = this.state.getCurrentPack();
    int currentPaintingIndex = currentPack.paintings().indexOf(this.state.getCurrentPainting());
    return Component.translatable(
        "custompaintings.painting.number",
        currentPaintingIndex + 1,
        currentPack.paintings().size()
    );
  }

  private static Component getTitleText(PaintingEditState state) {
    return Component.literal(state.getCurrentPack().name() + " - ")
        .append(Component.translatable("custompaintings.painting.title"));
  }

  private static Component getTooBigText(PaintingData paintingData) {
    return Component.translatable("custompaintings.painting.big", paintingData.width(), paintingData.height());
  }
}
