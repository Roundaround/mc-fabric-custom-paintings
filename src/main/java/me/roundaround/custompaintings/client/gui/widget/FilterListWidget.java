package me.roundaround.custompaintings.client.gui.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ElementListWidget<FilterListWidget.FilterEntry> {
  private static final int ITEM_HEIGHT = 20;

  public FilterListWidget(
      PaintingEditScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.centerListVertically = false;
    setRenderHeader(false, 0);

    addEntry(new ToggleFilterEntry(
        Text.translatable("custompaintings.filter.canstay"),
        ScreenTexts.YES,
        ScreenTexts.NO,
        () -> parent.getFilters().getCanStayOnly(),
        (value) -> parent.getFilters().setCanStayOnly(value)));
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
      this.getter = getter;

      this.button = CyclingButtonWidget.onOffBuilder(trueText, falseText)
          .values(List.of(true, false))
          .initially(getter.get())
          .build(width / 2 - 155, 0, 150, ITEM_HEIGHT, label, (button, value) -> {
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
      this.button.y = y;
      this.button.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<? extends Element> children() {
      return this.buttonList;
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return this.buttonList;
    }

    @Override
    public void resetToFilterValue() {
      this.button.setValue(this.getter.get());
    }
  }
}
