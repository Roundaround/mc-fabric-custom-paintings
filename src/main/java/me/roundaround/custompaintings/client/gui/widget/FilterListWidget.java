package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.FiltersState;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.IntSliderWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ParentElementEntryListWidget<FilterListWidget.Entry> {
  public FilterListWidget(
      FiltersState state, MinecraftClient client, ThreeSectionLayoutWidget layout
  ) {
    super(client, layout);

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.search"), index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.any"), state::getSearch, state::setSearch, index, left, top, width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.name"), state::getNameSearch, state::setNameSearch, index, left, top,
        width
    ));

    this.addEntry(
        (index, left, top, width) -> new ToggleFilterEntry(Text.translatable("custompaintings.filter.name.empty"),
            ScreenTexts.YES, ScreenTexts.NO, state::getNonEmptyNameOnly, state::setNonEmptyNameOnly, index, left, top,
            width
        ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.artist"), state::getArtistSearch, state::setArtistSearch, index, left,
        top, width
    ));

    this.addEntry(
        (index, left, top, width) -> new ToggleFilterEntry(Text.translatable("custompaintings.filter.artist.empty"),
            ScreenTexts.YES, ScreenTexts.NO, state::getNonEmptyArtistOnly, state::setNonEmptyArtistOnly, index, left,
            top, width
        ));

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(this.client.textRenderer,
        Text.translatable("custompaintings.filter.section.size"), index, left, top, width
    ));

    this.addEntry(
        (index, left, top, width) -> new ToggleFilterEntry(Text.translatable("custompaintings.filter.canstay"),
            ScreenTexts.YES, ScreenTexts.NO, state::getCanStayOnly, state::setCanStayOnly, index, left, top, width
        ));

    this.addEntry(
        (index, left, top, width) -> new SizeRangeEntry(state::getMinWidth, state::getMaxWidth, state::setMinWidth,
            state::setMaxWidth, "custompaintings.filter.minwidth", "custompaintings.filter.maxwidth", index, left, top,
            width
        ));

    this.addEntry(
        (index, left, top, width) -> new SizeRangeEntry(state::getMinHeight, state::getMaxHeight, state::setMinHeight,
            state::setMaxHeight, "custompaintings.filter.minheight", "custompaintings.filter.maxheight", index, left,
            top, width
        ));
  }

  public void updateFilters() {
    this.forEachEntry(Entry::resetToFilterValue);
  }

  public Element getFirstFocusable() {
    return this.children()
        .stream()
        .flatMap((entry) -> entry.children().stream())
        .filter((element -> element instanceof TextFieldWidget || element instanceof ButtonWidget ||
                            element instanceof SliderWidget))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected int getPreferredContentWidth() {
    return VANILLA_LIST_WIDTH_M;
  }

  @Environment(value = EnvType.CLIENT)
  public abstract static class Entry extends ParentElementEntryListWidget.Entry {
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

      this.setForceRowShading(true);
      this.setMarginY(2 * GuiUtil.PADDING);

      this.label = LabelWidget.builder(textRenderer, label)
          .position(this.getContentLeft(), this.getContentTop())
          .dimensions(this.getFullControlWidth(), this.getContentHeight())
          .alignTextCenterX()
          .alignTextCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawableChild(this.label);
    }

    @Override
    public void refreshPositions() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentLeft(), this.getContentTop());
        this.label.setDimensions(this.getFullControlWidth(), this.getContentHeight());
      });
    }

    @Override
    protected void renderRowShade(DrawContext context) {
      renderRowShade(context, this.getX(), this.getY() + this.margin.top(), this.getRight(),
          this.getBottom() - this.margin.bottom(), this.getRowShadeFadeWidth(), this.getRowShadeStrength()
      );
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

      this.getter = getter;

      this.button = CyclingButtonWidget.onOffBuilder(trueText, falseText)
          .values(List.of(true, false))
          .initially(this.getter.get())
          .build(this.getControlLeft(), this.getContentTop(), this.getFullControlWidth(), this.getContentHeight(),
              label, (button, value) -> setter.accept(value)
          );

      this.addDrawableChild(this.button);
    }

    @Override
    public void refreshPositions() {
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
          .position(this.getControlLeft(), this.getContentCenterY())
          .dimensions(this.getHalfControlWidth(), this.getContentHeight())
          .alignSelfLeft()
          .alignSelfCenterY()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawableChild(this.label);

      this.textField = new TextFieldWidget(textRenderer, this.getRightControlLeft(), this.getContentTop(),
          this.getHalfControlWidth(), this.getContentHeight(), label
      );
      this.textField.setChangedListener(setter);
      this.textField.setText(this.getter.get());

      this.addDrawableChild(this.textField);
    }

    @Override
    public void resetToFilterValue() {
      this.textField.setText(this.getter.get());
    }

    @Override
    public void refreshPositions() {
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

      this.lowSlider = this.addDrawableChild(
          new IntSliderWidget(this.getControlLeft(), this.getContentTop(), this.getHalfControlWidth(),
              this.getContentHeight(), 1, 32, this.lowGetter.get(), this::stepLow, this::onLowSliderChange,
              (value) -> Text.translatable(lowI18nKey, value)
          ));

      this.highSlider = this.addDrawableChild(
          new IntSliderWidget(this.getRightControlLeft(), this.getContentTop(), this.getHalfControlWidth(),
              this.getContentHeight(), 1, 32, this.highGetter.get(), this::stepHigh, this::onHighSliderChange,
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
