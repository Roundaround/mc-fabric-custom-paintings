package me.roundaround.custompaintings.client.gui.widget;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
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

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ElementListWidget<FilterListWidget.FilterEntry> {
  private static final int ITEM_HEIGHT = 24;
  private static final int CONTROL_HEIGHT = 20;
  private static final int CONTROL_FULL_WIDTH = 310;
  private static final int CONTROL_HALF_WIDTH = 150;

  private final TextRenderer textRenderer;

  public FilterListWidget(
      PaintingEditScreen parent,
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

    TextFilterEntry previousNameFilterEntry = null;
    if (previousInstance != null && !previousInstance.children().isEmpty()) {
      previousNameFilterEntry = (TextFilterEntry) previousInstance.children().get(0);
    }
    addEntry(new TextFilterEntry(
        Text.translatable("custompaintings.filter.name.label"),
        previousNameFilterEntry,
        () -> parent.getState().getFilters().getNameSearch(),
        (value) -> parent.getState().getFilters().setNameSearch(value)));

    TextFilterEntry previousArtistFilterEntry = null;
    if (previousInstance != null && previousInstance.children().size() > 1) {
      previousArtistFilterEntry = (TextFilterEntry) previousInstance.children().get(1);
    }
    addEntry(new TextFilterEntry(
        Text.translatable("custompaintings.filter.artist.label"),
        previousArtistFilterEntry,
        () -> parent.getState().getFilters().getArtistSearch(),
        (value) -> parent.getState().getFilters().setArtistSearch(value)));

    // TODO: Name: Empty
    // TODO: Artist: Empty

    addEntry(new ToggleFilterEntry(
        Text.translatable("custompaintings.filter.canstay.label"),
        Text.translatable("custompaintings.filter.canstay.tooltip"),
        ScreenTexts.YES,
        ScreenTexts.NO,
        () -> parent.getState().getFilters().getCanStayOnly(),
        (value) -> parent.getState().getFilters().setCanStayOnly(value)));

    // TODO: Min width: >/</=/<>/etc
    // TODO: Min height: >/</=/<>/etc
    // TODO: Max width: >/</=/<>/etc
    // TODO: Max height: >/</=/<>/etc
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

  public void updateFilters() {
    children().forEach((child) -> {
      child.resetToFilterValue();
    });
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class FilterEntry extends ElementListWidget.Entry<FilterEntry> {
    public abstract void resetToFilterValue();
  }

  @Environment(value = EnvType.CLIENT)
  public class ToggleFilterEntry extends FilterEntry {
    private final Supplier<Boolean> getter;
    private final CyclingButtonWidget<Boolean> button;
    private final List<CyclingButtonWidget<Boolean>> buttonList;

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
              (FilterListWidget.this.width - CONTROL_HALF_WIDTH) / 2,
              0,
              CONTROL_HALF_WIDTH,
              CONTROL_HEIGHT,
              label,
              (button, value) -> {
                setter.accept(value);
              });
      this.buttonList = List.of(this.button);
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
      return this.buttonList;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return List.of();
    }

    @Override
    public void resetToFilterValue() {
      this.button.setValue(this.getter.get());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class TextFilterEntry extends FilterEntry {
    private final Text label;
    private final Supplier<String> getter;
    private final TextFieldWidget textField;
    private final List<TextFieldWidget> textFieldList;

    public TextFilterEntry(
        Text label,
        TextFilterEntry previousInstance,
        Supplier<String> getter,
        Consumer<String> setter) {
      this.label = label;
      this.getter = getter;

      this.textField = new TextFieldWidget(
          FilterListWidget.this.textRenderer,
          CONTROL_FULL_WIDTH - CONTROL_HALF_WIDTH,
          0,
          CONTROL_HALF_WIDTH,
          CONTROL_HEIGHT,
          previousInstance != null ? previousInstance.textField : null,
          label);
      this.textField.setText(this.getter.get());
      this.textField.setChangedListener((value) -> {
        setter.accept(value);
      });
      this.textFieldList = List.of(this.textField);
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
      int width = FilterListWidget.this.textRenderer.getWidth(this.label);
      FilterListWidget.this.textRenderer.draw(
          matrixStack,
          this.label,
          x + CONTROL_HALF_WIDTH - width,
          y + (entryHeight - FilterListWidget.this.textRenderer.fontHeight) / 2,
          0xFFFFFF);

      this.textField.y = y + (entryHeight - CONTROL_HEIGHT) / 2;
      this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<? extends Element> children() {
      return this.textFieldList;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return this.textFieldList;
    }

    @Override
    public void resetToFilterValue() {
      this.textField.setText(this.getter.get());
    }
  }
}
