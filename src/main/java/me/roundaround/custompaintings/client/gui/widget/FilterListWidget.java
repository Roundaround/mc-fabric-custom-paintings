package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.IntSliderWidget;
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
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends FlowListWidget<FilterListWidget.Entry> {
  public FilterListWidget(
      PaintingEditState state, MinecraftClient client, ThreePartsLayoutWidget layout
  ) {
    super(client, layout.getX(), layout.getHeaderHeight(), layout.getWidth(), layout.getContentHeight());

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.search"), index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.any"), () -> state.getFilters().getSearch(),
        (value) -> state.getFilters().setSearch(value), index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.name"), () -> state.getFilters().getNameSearch(),
        (value) -> state.getFilters().setNameSearch(value), index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.artist"), () -> state.getFilters().getArtistSearch(),
        (value) -> state.getFilters().setArtistSearch(value), index, left, top, width
    ));

    // TODO: Name/artist is empty

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.size"), index, left, top, width
    ));

    this.addEntry(
        (index, left, top, width) -> new ToggleFilterEntry(Text.translatable("custompaintings.filter.canstay"),
            ScreenTexts.ON, ScreenTexts.OFF, () -> state.getFilters().getCanStayOnly(),
            (value) -> state.getFilters().setCanStayOnly(value), index, left, top, width
        ));

    this.addEntry((index, left, top, width) -> new SizeRangeEntry(() -> state.getFilters().getMinWidth(),
        () -> state.getFilters().getMaxWidth(), (value) -> state.getFilters().setMinWidth(value),
        (value) -> state.getFilters().setMaxWidth(value), "custompaintings.filter.minwidth",
        "custompaintings.filter.maxwidth", index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new SizeRangeEntry(() -> state.getFilters().getMinHeight(),
        () -> state.getFilters().getMaxHeight(), (value) -> state.getFilters().setMinHeight(value),
        (value) -> state.getFilters().setMaxHeight(value), "custompaintings.filter.minheight",
        "custompaintings.filter.maxheight", index, left, top, width
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

    public SectionTitleEntry(TextRenderer textRenderer, Text label, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.label = LabelWidget.builder(textRenderer, label)
          .refPosition(this.getContentCenterX(), this.getContentCenterY())
          .dimensions(this.getFullControlWidth(), this.getContentHeight())
          .justifiedCenter()
          .alignedMiddle()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawableChild(this.label);
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getFullControlWidth(), this.getContentHeight());
      });
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class ToggleFilterEntry extends Entry {
    private final Supplier<Boolean> getter;
    private final CyclingButtonWidget<Boolean> button;

    public ToggleFilterEntry(
        Text label,
        Text trueText,
        Text falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter,
        int index,
        int left,
        int top,
        int width
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
      super.refreshPositions();
      this.button.setDimensionsAndPosition(
          this.getFullControlWidth(), this.getContentHeight(), this.getControlLeft(), this.getContentTop());
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
        TextRenderer textRenderer,
        Text label,
        Supplier<String> getter,
        Consumer<String> setter,
        int index,
        int left,
        int top,
        int width
    ) {
      super(index, left, top, width, HEIGHT);

      this.getter = getter;

      this.label = LabelWidget.builder(textRenderer, label)
          .refPosition(this.getControlLeft(), this.getContentCenterY())
          .dimensions(this.getHalfControlWidth(), this.getContentHeight())
          .justifiedLeft()
          .alignedMiddle()
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
      super.refreshPositions();

      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getControlLeft(), this.getContentCenterY());
        this.label.setDimensions(this.getHalfControlWidth(), this.getContentHeight());
      });

      this.textField.setDimensionsAndPosition(
          this.getHalfControlWidth(), this.getContentHeight(), this.getRightControlLeft(), this.getContentTop());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class SizeRangeEntry extends Entry {
    private final IntSliderWidget lowSlider;
    private final IntSliderWidget highSlider;
    private final Supplier<Integer> lowGetter;
    private final Supplier<Integer> highGetter;
    private final Consumer<Integer> lowSetter;
    private final Consumer<Integer> highSetter;

    public SizeRangeEntry(
        Supplier<Integer> lowGetter,
        Supplier<Integer> highGetter,
        Consumer<Integer> lowSetter,
        Consumer<Integer> highSetter,
        String lowI18nKey,
        String highI18nKey,
        int index,
        int left,
        int top,
        int width
    ) {
      super(index, left, top, width, HEIGHT);

      this.lowGetter = lowGetter;
      this.highGetter = highGetter;
      this.lowSetter = lowSetter;
      this.highSetter = highSetter;

      this.lowSlider = this.addDrawableAndSelectableChild(
          new IntSliderWidget(this.getControlLeft(), this.getContentTop(), this.getHalfControlWidth(),
              this.getContentHeight(), this.lowGetter.get(), 1, 32, this::stepLow, this::onLowSliderChange,
              (value) -> Text.translatable(lowI18nKey, value)
          ));

      this.highSlider = this.addDrawableAndSelectableChild(
          new IntSliderWidget(this.getRightControlLeft(), this.getContentTop(), this.getHalfControlWidth(),
              this.getContentHeight(), this.highGetter.get(), 1, 32, this::stepHigh, this::onHighSliderChange,
              (value) -> Text.translatable(highI18nKey, value)
          ));
    }

    @Override
    public void resetToFilterValue() {
      this.lowSlider.setIntValue(this.lowGetter.get());
      this.highSlider.setIntValue(this.highGetter.get());
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();

      this.lowSlider.setDimensionsAndPosition(this.getHalfControlWidth(), this.getContentHeight(),
          this.getControlLeft(), this.getContentTop()
      );
      this.highSlider.setDimensionsAndPosition(this.getHalfControlWidth(), this.getContentHeight(),
          this.getRightControlLeft(), this.getContentTop()
      );
    }

    private int stepLow(int sign) {
      return step(this.lowSlider.getIntValue(), sign);
    }

    private int stepHigh(int sign) {
      return step(this.highSlider.getIntValue(), sign);
    }

    private void onLowSliderChange(int value) {
      this.lowSetter.accept(value);
    }

    private void onHighSliderChange(int value) {
      this.highSetter.accept(value);
    }

    private static int step(int from, int sign) {
      if (from == 1 && sign == 1) {
        return 4;
      }
      return MathHelper.clamp(from + 4 * sign, 1, 32);
    }
  }
}
