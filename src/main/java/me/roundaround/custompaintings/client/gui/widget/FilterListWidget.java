package me.roundaround.custompaintings.client.gui.widget;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ElementListWidget<FilterListWidget.FilterEntry> {
  private static final int ITEM_HEIGHT = 26;
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
    setRenderSelection(false);
    setRenderHeader(false, 0);

    Queue<TextFilterEntry> previousEntries = new LinkedList<>();
    if (previousInstance != null) {
      for (FilterEntry entry : previousInstance.children()) {
        if (entry instanceof TextFilterEntry) {
          previousEntries.add((TextFilterEntry) entry);
        }
      }
    }

    addEntry(new SectionTitleEntry(
        Text.translatable("custompaintings.filter.section.search")));

    TextFilterEntry previousNameFilterEntry = null;
    if (!previousEntries.isEmpty()) {
      previousNameFilterEntry = previousEntries.remove();
    }
    addEntry(new TextFilterEntry(
        Text.translatable("custompaintings.filter.name"),
        previousNameFilterEntry,
        () -> state.getFilters().getNameSearch(),
        (value) -> state.getFilters().setNameSearch(value)));

    TextFilterEntry previousArtistFilterEntry = null;
    if (!previousEntries.isEmpty()) {
      previousArtistFilterEntry = previousEntries.remove();
    }
    addEntry(new TextFilterEntry(
        Text.translatable("custompaintings.filter.artist"),
        previousArtistFilterEntry,
        () -> state.getFilters().getArtistSearch(),
        (value) -> state.getFilters().setArtistSearch(value)));

    // TODO: Name/artist is empty

    addEntry(new SectionTitleEntry(
        Text.translatable("custompaintings.filter.section.size")));

    addEntry(new ToggleFilterEntry(
        Text.translatable("custompaintings.filter.canstay"),
        ScreenTexts.ON,
        ScreenTexts.OFF,
        () -> state.getFilters().getCanStayOnly(),
        (value) -> state.getFilters().setCanStayOnly(value)));

    // TODO: Min/max width/height
  }

  @Override
  public int getRowWidth() {
    return 400;
  }

  @Override
  protected int getScrollbarPositionX() {
    return super.getScrollbarPositionX() + 32;
  }

  @Override
  public void appendNarrations(NarrationMessageBuilder builder) {
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
    children().forEach((child) -> {
      child.tick();
    });
  }

  public void updateFilters() {
    children().forEach((child) -> {
      child.resetToFilterValue();
    });
  }

  public void removeFocus() {
    children().forEach((child) -> {
      child.unselect();
    });
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
  }

  @Environment(value = EnvType.CLIENT)
  public class SectionTitleEntry extends FilterEntry {
    private final Text label;

    public SectionTitleEntry(Text label) {
      this.label = label;
    }

    @Override
    public void render(
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawCenteredTextWithShadow(
          matrixStack,
          FilterListWidget.this.textRenderer,
          label.asOrderedText(),
          x + entryWidth / 2,
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
            return FilterListWidget.this.textRenderer.wrapLines(tooltip, 200);
          })
          .build(
              (FilterListWidget.this.width - CONTROL_FULL_WIDTH) / 2,
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
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      this.button.y = y + (entryHeight - CONTROL_HEIGHT) / 2;
      this.button.render(matrixStack, mouseX, mouseY, partialTicks);
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
        this.button.changeFocus(false);
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

      this.textField = new TextFieldWidget(
          FilterListWidget.this.textRenderer,
          (FilterListWidget.this.width - CONTROL_FULL_WIDTH) / 2 + CONTROL_HALF_WIDTH,
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
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawTextWithShadow(
          matrixStack,
          FilterListWidget.this.textRenderer,
          this.label,
          (FilterListWidget.this.width - CONTROL_FULL_WIDTH) / 2,
          y + MathHelper.ceil((entryHeight - FilterListWidget.this.textRenderer.fontHeight) / 2f),
          0xFFFFFF);

      this.textField.y = y + (entryHeight - CONTROL_HEIGHT) / 2;
      this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
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
    public void tick() {
      this.textField.tick();
    }

    @Override
    public void unselect() {
      this.textField.setTextFieldFocused(false);
    }
  }
}
