package me.roundaround.custompaintings.client.gui.screen.edit;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.PaintingEditState.PaintingChangeListener;
import me.roundaround.custompaintings.client.gui.widget.EmptyWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class PaintingSelectScreen extends PaintingEditScreen implements PaintingChangeListener {
  protected static final int BUTTON_WIDTH = 150;
  protected static final int BUTTON_HEIGHT = 20;
  protected static final int BUTTON_SPACING = GuiUtil.PADDING * 2;

  private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(
      this, GuiUtil.COMPACT_HEADER_HEIGHT, GuiUtil.DEFAULT_HEADER_FOOTER_HEIGHT);
  private final ArrayList<LabelWidget> labels = new ArrayList<>();

  private TextFieldWidget searchBox;
  private PaintingListWidget paintingList;
  private EmptyWidget paintingSpacer;
  private IconButtonWidget prevButton;
  private LabelWidget controlsLabel;
  private IconButtonWidget nextButton;
  private ButtonWidget doneButton;
  private int paneWidth;
  private int rightPaneX;
  private double scrollAmount;
  private ArrayList<PaintingData> paintings = new ArrayList<>();
  private boolean hasHiddenPaintings = false;
  private PaintingData paintingData;
  private boolean canStay = true;
  private Sprite paintingSprite = null;
  private Rect2i paintingRect = new Rect2i(0, 0, 0, 0);

  public PaintingSelectScreen(PaintingEditState state) {
    super(generateTitle(state), state);

    this.paintingData = state.getCurrentPainting();
  }

  @Override
  public void init() {
    this.applyFilters();
    this.onPaintingChange(this.state.getCurrentPainting());

    this.paneWidth = this.width / 2 - 2 * GuiUtil.PADDING;
    this.rightPaneX = this.width - this.paneWidth;

    DirectionalLayoutWidget body = DirectionalLayoutWidget.horizontal().spacing(GuiUtil.PADDING * 2);
    this.layout.addBody(body);

    DirectionalLayoutWidget leftPane = DirectionalLayoutWidget.vertical().spacing(GuiUtil.PADDING);
    leftPane.getMainPositioner().alignHorizontalCenter();
    body.add(leftPane);

    DirectionalLayoutWidget searchRow = DirectionalLayoutWidget.horizontal().spacing(GuiUtil.PADDING);
    searchRow.getMainPositioner().alignVerticalCenter().marginLeft(GuiUtil.PADDING);
    leftPane.add(searchRow);

    this.searchBox = searchRow.add(
        new TextFieldWidget(this.textRenderer, 0, 0, this.getSearchWidth(), BUTTON_HEIGHT, this.searchBox,
            Text.translatable("custompaintings.painting.search")
        ));
    this.searchBox.setText(this.state.getFilters().getSearch());
    this.searchBox.setChangedListener(this::onSearchBoxChanged);

    searchRow.add(IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.FILTER_18, CustomPaintingsMod.MOD_ID)
        .vanillaSize()
        .messageAndTooltip(Text.translatable("custompaintings.painting.filter"))
        .onPress(this::filterButtonPressed)
        .build());

    this.paintingList = leftPane.add(
        new PaintingListWidget(this.state, this.client, 0, this.getPaintingListY(), this.paneWidth,
            this.getPaintingListHeight(), this::saveSelection
        ));
    this.paintingList.setPaintings(this.paintings);
    this.paintingList.selectFirst();

    DirectionalLayoutWidget rightPane = DirectionalLayoutWidget.vertical().spacing(GuiUtil.PADDING);
    rightPane.getMainPositioner().alignHorizontalCenter();
    body.add(rightPane);

    if (this.paintingData.hasLabel()) {
      this.labels.add(LabelWidget.builder(this.textRenderer, this.getLabelText(), 0, 0)
          .hideBackground()
          .maxWidth(this.getPaneWidth())
          .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
          .build());
    }

    this.labels.add(LabelWidget.builder(this.textRenderer, this.getIdText(), 0, 0)
        .hideBackground()
        .maxWidth(this.getPaneWidth())
        .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
        .build());

    this.labels.add(LabelWidget.builder(this.textRenderer, this.getDimensionsText(), 0, 0)
        .hideBackground()
        .maxWidth(this.getPaneWidth())
        .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
        .build());

    DirectionalLayoutWidget labelsContainer = DirectionalLayoutWidget.vertical();
    labelsContainer.getMainPositioner().alignHorizontalCenter();
    rightPane.add(labelsContainer);

    this.labels.forEach(labelsContainer::add);

    this.paintingSpacer = rightPane.add(EmptyWidget.ofHeight(this.getPaintingHeight()));

    DirectionalLayoutWidget controlsRow = DirectionalLayoutWidget.horizontal().spacing(GuiUtil.PADDING);
    controlsRow.getMainPositioner().alignVerticalCenter();
    rightPane.add(controlsRow);

    this.prevButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.PREV_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.previous"))
            .onPress((button) -> this.previousPainting())
            .build(), Positioner::alignLeft);

    this.controlsLabel = controlsRow.add(LabelWidget.builder(textRenderer, ).build())

    this.nextButton = controlsRow.add(
        IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.PREV_18, CustomPaintingsMod.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.painting.next"))
            .onPress((button) -> this.nextPainting())
            .build(), Positioner::alignLeft);

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
    row.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
      this.saveCurrentSelection();
    }).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());

    if (!this.canStay) {
      this.doneButton.setTooltip(Tooltip.of(
          Text.translatable("custompaintings.painting.big", this.paintingData.width(), this.paintingData.height())));
    } else {
      this.doneButton.setTooltip(null);
    }

    this.addDrawable((context, mouseX, mouseY, delta) -> {
      if (!this.state.getCurrentPainting().isEmpty()) {
        int x = this.paintingRect.getX();
        int y = this.paintingRect.getY();
        int width = this.paintingRect.getWidth();
        int height = this.paintingRect.getHeight();
        float color = this.canStay ? 1f : 0.5f;

        context.fill(x, y, x + width, y + height, 0xFF000000);

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderColor(color, color, color, 1f);
        RenderSystem.setShaderTexture(0, this.paintingSprite.getAtlasId());
        context.drawSprite(x + 1, y + 1, 0, width - 2, height - 2, this.paintingSprite);
      }
    });

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  public void populateWithAllPaintings() {
    this.paintings.addAll(this.state.getCurrentGroup().paintings());
  }

  @Override
  public void onPaintingChange(PaintingData paintingData) {
    this.paintingData = paintingData;
    this.canStay = this.state.canStay(paintingData);
    this.paintingSprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);

    int headerHeight = this.layout.getHeaderHeight();
    int footerHeight = this.layout.getFooterHeight();

    int paintingTop = headerHeight + 8 + (paintingData.hasLabel() ? 3 : 2) * this.textRenderer.fontHeight +
        (paintingData.hasLabel() ? 2 : 1) * 2;
    int paintingBottom = this.height - footerHeight - 8 - BUTTON_HEIGHT;

    int maxWidth = this.paneWidth - 2 * 8;
    int maxHeight = paintingBottom - paintingTop;

    int width = paintingData.getScaledWidth(maxWidth, maxHeight);
    int height = paintingData.getScaledHeight(maxWidth, maxHeight);

    int x = this.rightPaneX + (this.paneWidth - width) / 2;
    int y = (paintingTop + paintingBottom - height) / 2;

    this.paintingRect = new Rect2i(x, y, width, height);

    if (this.paintingList != null) {
      this.paintingList.selectPainting(paintingData);
    }

    if (this.doneButton != null) {
      this.doneButton.active = this.canStay;
      if (!this.canStay) {
        this.doneButton.setTooltip(
            Tooltip.of(Text.translatable("custompaintings.painting.big", paintingData.width(), paintingData.height())));
      } else {
        this.doneButton.setTooltip(null);
      }
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
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    Group currentGroup = this.state.getCurrentGroup();
    PaintingData paintingData = this.state.getCurrentPainting();

    if (paintingData.isEmpty()) {
      int paneHeight = this.layout.getContentHeight();
      int posX = this.rightPaneX + this.paneWidth / 2;
      int posY = this.layout.getHeaderHeight() + (paneHeight - this.textRenderer.fontHeight) / 2;

      context.drawCenteredTextWithShadow(
          this.textRenderer, Text.translatable("custompaintings.painting.none")
              .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)), posX, posY, 0xFFFFFFFF);

    } else {
      int currentPaintingIndex = currentGroup.paintings().indexOf(paintingData);
      int posX = this.rightPaneX + this.paneWidth / 2;
      int posY = this.layout.getHeaderHeight() + 4;

      if (paintingData.hasLabel()) {
        context.drawCenteredTextWithShadow(this.textRenderer, paintingData.getLabel(), posX, posY, 0xFFFFFFFF);

        posY += this.textRenderer.fontHeight + 2;
      }

      context.drawCenteredTextWithShadow(
          this.textRenderer, Text.literal("(" + paintingData.id().toString() + ")")
              .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)), posX, posY, 0xFFFFFFFF);

      posY += this.textRenderer.fontHeight + 2;

      context.drawCenteredTextWithShadow(this.textRenderer,
          Text.translatable("custompaintings.painting.dimensions", paintingData.width(), paintingData.height()), posX,
          posY, 0xFFFFFFFF
      );

      context.drawCenteredTextWithShadow(this.textRenderer,
          Text.translatable("custompaintings.painting.number", currentPaintingIndex + 1,
              currentGroup.paintings().size()
          ), posX, this.height - this.layout.getFooterHeight() - 4 - BUTTON_HEIGHT +
              (BUTTON_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFFFF
      );
    }
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount
  ) {
    if (this.paintingList.isMouseOver(mouseX, mouseY)) {
      return this.paintingList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    return false;
  }

  private void applyFilters() {
    if (!this.state.getFilters().hasFilters()) {
      this.hasHiddenPaintings = false;
      this.paintings = this.state.getCurrentGroup().paintings();
      if (this.paintingList != null) {
        this.paintingList.setPaintings(this.paintings);
      }

      if (this.prevButton != null && this.nextButton != null) {
        this.prevButton.active = this.hasMultiplePaintings();
        this.nextButton.active = this.hasMultiplePaintings();
      }
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

    if (this.paintingList != null) {
      this.paintingList.setPaintings(this.paintings);
    }

    if (this.prevButton != null && this.nextButton != null) {
      this.prevButton.active = this.hasMultiplePaintings();
      this.nextButton.active = this.hasMultiplePaintings();
    }
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

  private int getPaneWidth() {
    return this.width / 2 - GuiUtil.PADDING;
  }

  private int getSearchWidth() {
    return this.getPaneWidth() - 2 * GuiUtil.PADDING - IconButtonWidget.SIZE_V;
  }

  private int getPaintingListY() {
    return this.layout.getHeaderHeight() + Math.max(BUTTON_HEIGHT, IconButtonWidget.SIZE_V) + GuiUtil.PADDING;
  }

  private int getFooterY() {
    return this.height - this.layout.getFooterHeight();
  }

  private int getPaintingListHeight() {
    return this.getFooterY() - this.getPaintingListY();
  }

  private int getPaintingHeight() {
    return this.layout.getContentHeight() - this.
  }

  private Text getLabelText() {
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
