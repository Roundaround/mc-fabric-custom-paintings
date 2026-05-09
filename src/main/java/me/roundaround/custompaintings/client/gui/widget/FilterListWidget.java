package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.FiltersState;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IntSliderWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ParentElementEntryListWidget<FilterListWidget.Entry> {
  public FilterListWidget(FiltersState state, Minecraft client, ThreeSectionLayoutWidget layout) {
    super(client, layout);

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(
        this.client.font,
        Component.translatable("custompaintings.filter.section.search"),
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(
        this.client.font,
        Component.translatable("custompaintings.filter.any"),
        state::getSearch,
        state::setSearch,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(
        this.client.font,
        Component.translatable("custompaintings.filter.name"),
        state::getNameSearch,
        state::setNameSearch,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new ToggleFilterEntry(
        Component.translatable("custompaintings.filter.name.empty"),
        CommonComponents.GUI_YES,
        CommonComponents.GUI_NO,
        state::getNonEmptyNameOnly,
        state::setNonEmptyNameOnly,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new TextFilterEntry(
        this.client.font,
        Component.translatable("custompaintings.filter.artist"),
        state::getArtistSearch,
        state::setArtistSearch,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new ToggleFilterEntry(
        Component.translatable("custompaintings.filter.artist.empty"),
        CommonComponents.GUI_YES,
        CommonComponents.GUI_NO,
        state::getNonEmptyArtistOnly,
        state::setNonEmptyArtistOnly,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new SectionTitleEntry(
        this.client.font,
        Component.translatable("custompaintings.filter.section.size"),
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new ToggleFilterEntry(
        Component.translatable("custompaintings.filter.canstay"),
        CommonComponents.GUI_YES,
        CommonComponents.GUI_NO,
        state::getCanStayOnly,
        state::setCanStayOnly,
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new SizeRangeEntry(
        state::getMinWidth,
        state::getMaxWidth,
        state::setMinWidth,
        state::setMaxWidth,
        "custompaintings.filter.minwidth",
        "custompaintings.filter.maxwidth",
        index,
        left,
        top,
        width
    ));

    this.addEntry((index, left, top, width) -> new SizeRangeEntry(
        state::getMinHeight,
        state::getMaxHeight,
        state::setMinHeight,
        state::setMaxHeight,
        "custompaintings.filter.minheight",
        "custompaintings.filter.maxheight",
        index,
        left,
        top,
        width
    ));
  }

  public void updateFilters() {
    this.forEachEntry(Entry::resetToFilterValue);
  }

  public GuiEventListener getFirstFocusable() {
    return this.children()
        .stream()
        .flatMap((entry) -> entry.children().stream())
        .filter((element -> element instanceof EditBox || element instanceof Button ||
                            element instanceof AbstractSliderButton))
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

    public SectionTitleEntry(Font textRenderer, Component label, int index, int left, int top, int width) {
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
    public void arrangeElements() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentLeft(), this.getContentTop());
        this.label.setSize(this.getFullControlWidth(), this.getContentHeight());
      });
    }

    @Override
    protected void renderRowShade(GuiGraphicsExtractor context) {
      renderRowShade(
          context,
          this.getX(),
          this.getY() + this.margin.top(),
          this.getRight(),
          this.getBottom() - this.margin.bottom(),
          this.getRowShadeFadeWidth(),
          this.getRowShadeStrength()
      );
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class ToggleFilterEntry extends Entry {
    private final Supplier<Boolean> getter;
    private final CycleButton<Boolean> button;

    public ToggleFilterEntry(
        Component label,
        Component trueText,
        Component falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter,
        int index,
        int left,
        int top,
        int width
    ) {
      super(index, left, top, width, HEIGHT);

      this.getter = getter;

      this.button = CycleButton.booleanBuilder(trueText, falseText, this.getter.get())
          .withValues(List.of(true, false))
          .create(
              this.getControlLeft(),
              this.getContentTop(),
              this.getFullControlWidth(),
              this.getContentHeight(),
              label,
              (button, value) -> setter.accept(value)
          );

      this.addDrawableChild(this.button);
    }

    @Override
    public void arrangeElements() {
      this.button.setRectangle(
          this.getFullControlWidth(),
          this.getContentHeight(),
          this.getControlLeft(),
          this.getContentTop()
      );
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
    private final EditBox textField;

    public TextFilterEntry(
        Font textRenderer,
        Component label,
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

      this.textField = new EditBox(
          textRenderer,
          this.getRightControlLeft(),
          this.getContentTop(),
          this.getHalfControlWidth(),
          this.getContentHeight(),
          label
      );
      this.textField.setResponder(setter);
      this.textField.setValue(this.getter.get());

      this.addDrawableChild(this.textField);
    }

    @Override
    public void resetToFilterValue() {
      this.textField.setValue(this.getter.get());
    }

    @Override
    public void arrangeElements() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getControlLeft(), this.getContentCenterY());
        this.label.setSize(this.getHalfControlWidth(), this.getContentHeight());
      });

      this.textField.setRectangle(
          this.getHalfControlWidth(),
          this.getContentHeight(),
          this.getRightControlLeft(),
          this.getContentTop()
      );
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

      this.lowSlider = this.addDrawableChild(new IntSliderWidget(
          this.getControlLeft(),
          this.getContentTop(),
          this.getHalfControlWidth(),
          this.getContentHeight(),
          1,
          32,
          this.lowGetter.get(),
          this::stepLow,
          this::onLowSliderChange,
          (value) -> Component.translatable(lowI18nKey, value)
      ));

      this.highSlider = this.addDrawableChild(new IntSliderWidget(
          this.getRightControlLeft(),
          this.getContentTop(),
          this.getHalfControlWidth(),
          this.getContentHeight(),
          1,
          32,
          this.highGetter.get(),
          this::stepHigh,
          this::onHighSliderChange,
          (value) -> Component.translatable(highI18nKey, value)
      ));
    }

    @Override
    public void resetToFilterValue() {
      this.lowSlider.setIntValue(this.lowGetter.get());
      this.highSlider.setIntValue(this.highGetter.get());
    }

    @Override
    public void arrangeElements() {
      super.arrangeElements();

      this.lowSlider.setRectangle(
          this.getHalfControlWidth(),
          this.getContentHeight(),
          this.getControlLeft(),
          this.getContentTop()
      );
      this.highSlider.setRectangle(
          this.getHalfControlWidth(),
          this.getContentHeight(),
          this.getRightControlLeft(),
          this.getContentTop()
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
      return Mth.clamp(from + 4 * sign, 1, 32);
    }
  }
}
