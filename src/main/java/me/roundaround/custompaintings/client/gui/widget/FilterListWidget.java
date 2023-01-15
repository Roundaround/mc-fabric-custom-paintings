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
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(value = EnvType.CLIENT)
public class FilterListWidget extends ElementListWidget<FilterListWidget.FilterEntry> {
  private static final int ITEM_HEIGHT = 20;

  private final PaintingEditScreen parent;

  public FilterListWidget(
      PaintingEditScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.parent = parent;
    setRenderBackground(false);
    setRenderHeader(false, 0);
  }

  @Override
  public void appendNarrations(NarrationMessageBuilder builder) {
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class FilterEntry extends ElementListWidget.Entry<FilterEntry> {

  }

  @Environment(value = EnvType.CLIENT)
  public class ToggleFilterEntry extends FilterEntry {
    private final String name;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    private final ClickableWidget button;

    public ToggleFilterEntry(
        String name,
        Text trueText,
        Text falseText,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter) {
      this.name = name;
      this.getter = getter;
      this.setter = setter;

      this.button = CyclingButtonWidget.onOffBuilder(
          trueText,
          falseText).values(List.of(true, false))
          .initially(false)
          .build(height, bottom, width, headerHeight, falseText);
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

    }

    @Override
    public List<? extends Element> children() {
      return List.of(button);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return List.of(button);
    }
  }
}
