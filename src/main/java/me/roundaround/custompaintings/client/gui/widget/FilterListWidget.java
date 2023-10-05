package me.roundaround.custompaintings.client.gui.widget;

import com.google.common.collect.ImmutableList;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ElementListWidget<FilterListWidget.FilterEntry> {
  private static final int ITEM_HEIGHT = 25;
  private static final int CONTROL_HEIGHT = 20;
  private static final int CONTROL_FULL_WIDTH = 310;
  private static final int CONTROL_HALF_WIDTH = 150;

  private final TextRenderer textRenderer;

  public FilterListWidget(
      PaintingEditState state,
      MinecraftClient minecraftClient,
      FilterListWidget previousInstance,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.textRenderer = minecraftClient.textRenderer;

    this.centerListVertically = false;
    setRenderHeader(false, 0);

    Queue<TextFilterEntry> previousEntries = new LinkedList<>();
    if (previousInstance != null) {
      for (FilterEntry entry : previousInstance.children()) {
        if (entry instanceof TextFilterEntry) {
          previousEntries.add((TextFilterEntry) entry);
        }
      }
    }

    addEntry(new SectionTitleEntry(Text.translatable("custompaintings.filter.section.search")));

    TextFilterEntry previousAnyFilterEntry = null;
    if (!previousEntries.isEmpty()) {
      previousAnyFilterEntry = previousEntries.remove();
    }
    addEntry(new TextFilterEntry(Text.translatable("custompaintings.filter.any"),
        previousAnyFilterEntry,
        () -> state.getFilters().getSearch(),
        (value) -> state.getFilters().setSearch(value)));

    TextFilterEntry previousNameFilterEntry = null;
    if (!previousEntries.isEmpty()) {
      previousNameFilterEntry = previousEntries.remove();
    }
    addEntry(new TextFilterEntry(Text.translatable("custompaintings.filter.name"),
        previousNameFilterEntry,
        () -> state.getFilters().getNameSearch(),
        (value) -> state.getFilters().setNameSearch(value)));

    TextFilterEntry previousArtistFilterEntry = null;
    if (!previousEntries.isEmpty()) {
      previousArtistFilterEntry = previousEntries.remove();
    }
    addEntry(new TextFilterEntry(Text.translatable("custompaintings.filter.artist"),
        previousArtistFilterEntry,
        () -> state.getFilters().getArtistSearch(),
        (value) -> state.getFilters().setArtistSearch(value)));

    // TODO: Name/artist is empty

    addEntry(new SectionTitleEntry(Text.translatable("custompaintings.filter.section.size")));

    addEntry(new ToggleFilterEntry(Text.translatable("custompaintings.filter.canstay"),
        ScreenTexts.ON,
        ScreenTexts.OFF,
        () -> state.getFilters().getCanStayOnly(),
        (value) -> state.getFilters().setCanStayOnly(value)));

    addEntry(new IntRangeFilterEntry((value) -> Text.translatable("custompaintings.filter.minwidth",
        value),
        (value) -> Text.translatable("custompaintings.filter.maxwidth", value),
        () -> state.getFilters().getMinWidth(),
        () -> state.getFilters().getMaxWidth(),
        (value) -> state.getFilters().setMinWidth(value),
        (value) -> state.getFilters().setMaxWidth(value),
        1,
        32));

    addEntry(new IntRangeFilterEntry((value) -> Text.translatable("custompaintings.filter.minheight",
        value),
        (value) -> Text.translatable("custompaintings.filter.maxheight", value),
        () -> state.getFilters().getMinHeight(),
        () -> state.getFilters().getMaxHeight(),
        (value) -> state.getFilters().setMinHeight(value),
        (value) -> state.getFilters().setMaxHeight(value),
        1,
        32));
  }

  @Override
  public int getRowWidth() {
    return 400;
  }

  @Override
  protected int getScrollbarPositionX() {
    return (this.width + getRowWidth()) / 2 + 4;
  }

  @Override
  public int getRowLeft() {
    return (this.width - getRowWidth()) / 2;
  }

  @Override
  public int getRowRight() {
    return (this.width + getRowWidth()) / 2;
  }

  @Override
  public void appendNarrations(NarrationMessageBuilder builder) {
  }

  public int getRowCenter() {
    return getRowLeft() + getRowWidth() / 2;
  }

  public int getControlLeft() {
    return getRowCenter() - CONTROL_FULL_WIDTH / 2;
  }

  public int getControlRight() {
    return getRowCenter() + CONTROL_FULL_WIDTH / 2;
  }

  public int getControlInnerLeft() {
    return getControlRight() - CONTROL_HALF_WIDTH;
  }

  public int getControlInnerRight() {
    return getControlLeft() + CONTROL_HALF_WIDTH;
  }

  public Optional<Element> getHoveredElement(double mouseX, double mouseY) {
    for (FilterEntry entry : children()) {
      for (Element element : entry.children()) {
        if (element.isMouseOver(mouseX, mouseY)) {
          return Optional.of(element);
        }
      }
    }
    return Optional.empty();
  }

  public void tick() {
    children().forEach(FilterEntry::tick);
  }

  public void updateFilters() {
    children().forEach(FilterEntry::resetToFilterValue);
  }

  public void removeFocus() {
    children().forEach(FilterEntry::unselect);
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class FilterEntry extends ElementListWidget.Entry<FilterEntry> {
    public void resetToFilterValue() {
    }

    public void tick() {
    }

    public void unselect() {
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of();
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      FilterListWidget.this.removeFocus();
      return super.mouseClicked(mouseX, mouseY, button);
    }

    protected int getRowCenter() {
      return FilterListWidget.this.getRowCenter();
    }

    protected int getControlLeft() {
      return FilterListWidget.this.getControlLeft();
    }

    protected int getControlRight() {
      return FilterListWidget.this.getControlRight();
    }

    protected int getControlInnerLeft() {
      return FilterListWidget.this.getControlInnerLeft();
    }

    protected int getControlInnerRight() {
      return FilterListWidget.this.getControlInnerRight();
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class SectionTitleEntry extends FilterEntry {
    private final Text label;

    public SectionTitleEntry(Text label) {
      this.label = label;
    }

    @Override
    public void render(
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawContext.drawCenteredTextWithShadow(FilterListWidget.this.textRenderer,
          label.asOrderedText(),
          getRowCenter(),
          y + MathHelper.ceil((entryHeight - FilterListWidget.this.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class ToggleFilterEntry extends FilterEntry {
    private final Supplier<Boolean> getter;
    private final CyclingButtonWidget<Boolean> button;

    public ToggleFilterEntry(
        Text label,
        Text trueText,
        Text falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter) {
      this(label, null, trueText, falseText, getter, setter);
    }

    public ToggleFilterEntry(
        Text label,
        Text tooltip,
        Text trueText,
        Text falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter) {
      this.getter = getter;

      this.button = CyclingButtonWidget.onOffBuilder(trueText, falseText)
          .values(List.of(true, false))
          .initially(this.getter.get())
          .tooltip((value) -> {
            if (tooltip == null || tooltip == Text.EMPTY) {
              return null;
            }
            return Tooltip.of(tooltip);
          })
          .build(getControlLeft(),
              0,
              CONTROL_FULL_WIDTH,
              CONTROL_HEIGHT,
              label,
              (button, value) -> {
                setter.accept(value);
              });
    }

    @Override
    public void render(
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      this.button.setY(y + (entryHeight - CONTROL_HEIGHT) / 2);
      this.button.render(drawContext, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of(this.button);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of(this.button);
    }

    @Override
    public void resetToFilterValue() {
      this.button.setValue(this.getter.get());
    }

    @Override
    public void unselect() {
      if (this.button.isFocused()) {
        this.button.setFocused(false);
      }
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class TextFilterEntry extends FilterEntry {
    private final Text label;
    private final Supplier<String> getter;
    private final TextFieldWidget textField;

    public TextFilterEntry(
        Text label,
        TextFilterEntry previousInstance,
        Supplier<String> getter,
        Consumer<String> setter) {
      this.label = label;
      this.getter = getter;

      this.textField = new TextFieldWidget(FilterListWidget.this.textRenderer,
          getControlInnerLeft(),
          0,
          CONTROL_HALF_WIDTH,
          CONTROL_HEIGHT,
          previousInstance != null ? previousInstance.textField : null,
          label);
      this.textField.setText(this.getter.get());
      this.textField.setChangedListener(setter);
    }

    @Override
    public void render(
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawContext.drawTextWithShadow(FilterListWidget.this.textRenderer,
          this.label,
          getControlLeft(),
          y + MathHelper.ceil((entryHeight - FilterListWidget.this.textRenderer.fontHeight) / 2f),
          0xFFFFFF);

      this.textField.setY(y + (entryHeight - CONTROL_HEIGHT) / 2);
      this.textField.render(drawContext, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of(this.textField);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of(this.textField);
    }

    @Override
    public void resetToFilterValue() {
      this.textField.setText(this.getter.get());
    }

    @Override
    public void unselect() {
      this.textField.setFocused(false);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class IntRangeFilterEntry extends FilterEntry {
    private final Supplier<Integer> getLow;
    private final Supplier<Integer> getHigh;
    private final IntSliderWidget lowSlider;
    private final IntSliderWidget highSlider;

    public IntRangeFilterEntry(
        Function<Integer, Text> lowLabelFactory,
        Function<Integer, Text> highLabelFactory,
        Supplier<Integer> getLow,
        Supplier<Integer> getHigh,
        Consumer<Integer> setLow,
        Consumer<Integer> setHigh,
        int min,
        int max) {
      this.getLow = getLow;
      this.getHigh = getHigh;

      this.lowSlider = new IntSliderWidget(getControlLeft(),
          0,
          CONTROL_HALF_WIDTH,
          CONTROL_HEIGHT,
          this.getLow.get(),
          min,
          max,
          lowLabelFactory,
          setLow);

      this.highSlider = new IntSliderWidget(getControlInnerLeft(),
          0,
          CONTROL_HALF_WIDTH,
          CONTROL_HEIGHT,
          this.getHigh.get(),
          min,
          max,
          highLabelFactory,
          setHigh);
    }

    @Override
    public void render(
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      this.lowSlider.setY(y + (entryHeight - CONTROL_HEIGHT) / 2);
      this.lowSlider.render(drawContext, mouseX, mouseY, partialTicks);

      this.highSlider.setY(y + (entryHeight - CONTROL_HEIGHT) / 2);
      this.highSlider.render(drawContext, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of(this.lowSlider, this.highSlider);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of(this.lowSlider, this.highSlider);
    }

    @Override
    public void resetToFilterValue() {
      this.lowSlider.setValue(this.getLow.get());
      this.highSlider.setValue(this.getHigh.get());
    }

    @Override
    public void unselect() {
      if (this.lowSlider.isFocused()) {
        this.lowSlider.setFocused(false);
      }
      if (this.highSlider.isFocused()) {
        this.highSlider.setFocused(false);
      }
    }
  }
}
