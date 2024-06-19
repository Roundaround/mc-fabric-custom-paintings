package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends FlowListWidget<FilterListWidget.Entry> {
  public FilterListWidget(
      PaintingEditState state, MinecraftClient client, ThreePartsLayoutWidget layout
  ) {
    super(client, layout.getX(), layout.getHeaderHeight(), layout.getWidth(), layout.getContentHeight());

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(index, left, top, width, this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.search")
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(index, left, top, width, this.client.textRenderer,
        Text.translatable("custompaintings.filter.any"), () -> state.getFilters().getSearch(),
        (value) -> state.getFilters().setSearch(value)
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(index, left, top, width, this.client.textRenderer,
        Text.translatable("custompaintings.filter.name"), () -> state.getFilters().getNameSearch(),
        (value) -> state.getFilters().setNameSearch(value)
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(index, left, top, width, this.client.textRenderer,
        Text.translatable("custompaintings.filter.artist"), () -> state.getFilters().getArtistSearch(),
        (value) -> state.getFilters().setArtistSearch(value)
    ));

    // TODO: Name/artist is empty

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(index, left, top, width, this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.size")
    ));

    this.addEntry((index, left, top, width) -> new ToggleFilterEntry(index, left, top, width,
        Text.translatable("custompaintings.filter.canstay"), ScreenTexts.ON, ScreenTexts.OFF,
        () -> state.getFilters().getCanStayOnly(), (value) -> state.getFilters().setCanStayOnly(value)
    ));

    this.addEntry((index, left, top, width) -> new IntRangeFilterEntry(index, left, top, width,
        (value) -> Text.translatable("custompaintings.filter.minwidth", value),
        (value) -> Text.translatable("custompaintings.filter.maxwidth", value), () -> state.getFilters().getMinWidth(),
        () -> state.getFilters().getMaxWidth(), (value) -> state.getFilters().setMinWidth(value),
        (value) -> state.getFilters().setMaxWidth(value), 1, 32
    ));

    this.addEntry((index, left, top, width) -> new IntRangeFilterEntry(index, left, top, width,
        (value) -> Text.translatable("custompaintings.filter.minheight", value),
        (value) -> Text.translatable("custompaintings.filter.maxheight", value),
        () -> state.getFilters().getMinHeight(), () -> state.getFilters().getMaxHeight(),
        (value) -> state.getFilters().setMinHeight(value), (value) -> state.getFilters().setMaxHeight(value), 1, 32
    ));
  }

  public void updateFilters() {
    this.forEachEntry(Entry::resetToFilterValue);
  }

  @Environment(value = EnvType.CLIENT)
  public abstract static class Entry extends FlowListWidget.Entry {
    protected static final int HEIGHT = 20;
    protected static final int MAX_FULL_WIDTH = 310;

    protected Entry(int index, int left, int top, int width, int contentHeight) {
      super(index, left, top, width, contentHeight);
    }

    public void resetToFilterValue() {
    }

    protected int getFullControlWidth() {
      return Math.min(MAX_FULL_WIDTH, this.getContentWidth());
    }

    protected int getHalfControlWidth() {
      return (this.getFullControlWidth() - 2 * GuiUtil.PADDING) / 2;
    }

    protected int getControlLeft() {
      return this.getContentCenterX() - this.getFullControlWidth() / 2;
    }

    protected int getControlRight() {
      return this.getContentCenterX() + this.getFullControlWidth() / 2;
    }

    protected int getRightControlLeft() {
      return this.getControlRight() - this.getHalfControlWidth();
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class SectionTitleEntry extends Entry {
    protected final LabelWidget label;

    public SectionTitleEntry(int index, int left, int top, int width, TextRenderer textRenderer, Text label) {
      super(index, left, top, width, HEIGHT);

      this.label = LabelWidget.builder(textRenderer, label, this.getContentCenterX(), this.getContentCenterY())
          .justifiedCenter()
          .alignedMiddle()
          .maxWidth(this.getFullControlWidth())
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawableChild(this.label);
    }

    @Override
    public void refreshPositions() {
      this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
      this.label.setMaxWidth(this.getFullControlWidth());
      super.refreshPositions();
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class ToggleFilterEntry extends Entry {
    private final Supplier<Boolean> getter;
    private final CyclingButtonWidget<Boolean> button;

    public ToggleFilterEntry(
        int index,
        int left,
        int top,
        int width,
        Text label,
        Text trueText,
        Text falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter
    ) {
      super(index, left, top, width, HEIGHT);

      this.setMarginX(DEFAULT_MARGIN_HORIZONTAL + GuiUtil.PADDING);

      this.getter = getter;

      this.button = CyclingButtonWidget.onOffBuilder(trueText, falseText)
          .values(List.of(true, false))
          .initially(this.getter.get())
          .build(this.getControlLeft(), this.getContentTop(), this.getFullControlWidth(), this.getContentHeight(),
              label, (button, value) -> setter.accept(value)
          );

      this.addChild(this.button);
      this.addSelectableChild(this.button);
    }

    @Override
    public void refreshPositions() {
      this.button.setDimensionsAndPosition(
          this.getFullControlWidth(), this.getContentHeight(), this.getControlLeft(), this.getContentTop());

      super.refreshPositions();
    }

    @Override
    public void resetToFilterValue() {
      this.button.setValue(this.getter.get());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class TextFilterEntry extends Entry {
    private final Supplier<String> getter;
    private final LabelWidget label;
    private final TextFieldWidget textField;

    public TextFilterEntry(
        int index,
        int left,
        int top,
        int width,
        TextRenderer textRenderer,
        Text label,
        Supplier<String> getter,
        Consumer<String> setter
    ) {
      super(index, left, top, width, HEIGHT);

      this.getter = getter;

      this.label = LabelWidget.builder(textRenderer, label, this.getControlLeft(), this.getContentCenterY())
          .justifiedLeft()
          .alignedMiddle()
          .maxWidth(this.getHalfControlWidth())
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawableChild(this.label);

      this.textField = new TextFieldWidget(textRenderer, this.getRightControlLeft(), this.getContentTop(),
          this.getHalfControlWidth(), this.getContentHeight(), label
      );
      this.textField.setText(this.getter.get());
      this.textField.setChangedListener(setter);

      this.addChild(this.textField);
      this.addSelectableChild(this.textField);
    }

    @Override
    public void resetToFilterValue() {
      this.textField.setText(this.getter.get());
    }

    @Override
    public void refreshPositions() {
      this.label.setPosition(this.getControlLeft(), this.getContentCenterY());
      this.label.setMaxWidth(this.getHalfControlWidth());

      this.textField.setDimensionsAndPosition(
          this.getHalfControlWidth(), this.getContentHeight(), this.getRightControlLeft(), this.getContentTop());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class IntRangeFilterEntry extends Entry {
    private final Supplier<Integer> getLow;
    private final Supplier<Integer> getHigh;
    private final IntSliderWidget lowSlider;
    private final IntSliderWidget highSlider;

    public IntRangeFilterEntry(
        int index,
        int left,
        int top,
        int width,
        Function<Integer, Text> lowLabelFactory,
        Function<Integer, Text> highLabelFactory,
        Supplier<Integer> getLow,
        Supplier<Integer> getHigh,
        Consumer<Integer> setLow,
        Consumer<Integer> setHigh,
        int min,
        int max
    ) {
      super(index, left, top, width, HEIGHT);

      this.getLow = getLow;
      this.getHigh = getHigh;

      this.lowSlider = new IntSliderWidget(this.getControlLeft(), this.getContentTop(), this.getHalfControlWidth(),
          this.getContentHeight(), this.getLow.get(), min, max, lowLabelFactory, setLow
      );

      this.addChild(this.lowSlider);
      this.addSelectableChild(this.lowSlider);

      this.highSlider = new IntSliderWidget(this.getRightControlLeft(), this.getContentTop(),
          this.getHalfControlWidth(), this.getContentHeight(), this.getHigh.get(), min, max, highLabelFactory, setHigh
      );

      this.addChild(this.highSlider);
      this.addSelectableChild(this.highSlider);
    }

    @Override
    public void resetToFilterValue() {
      this.lowSlider.setValue(this.getLow.get());
      this.highSlider.setValue(this.getHigh.get());
    }

    @Override
    public void refreshPositions() {
      this.lowSlider.setDimensionsAndPosition(this.getHalfControlWidth(), this.getContentHeight(),
          this.getControlLeft(), this.getContentTop()
      );
      this.highSlider.setDimensionsAndPosition(this.getHalfControlWidth(), this.getContentHeight(),
          this.getRightControlLeft(), this.getContentTop()
      );
    }
  }
}
