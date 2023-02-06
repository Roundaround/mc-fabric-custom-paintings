package me.roundaround.custompaintings.client.gui.widget;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(value = EnvType.CLIENT)
public class ButtonListWidget extends ElementListWidget<ButtonListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;
  private static final int BUTTON_WIDTH = 204;
  private static final int BUTTON_HEIGHT = 20;

  public ButtonListWidget(
      MinecraftClient client,
      int width,
      int height,
      int top,
      int bottom) {
    super(client, width, height, top, bottom, ITEM_HEIGHT);
  }

  public void addEntry(Text text, ButtonWidget.PressAction action) {
    this.addEntry(new Entry(new ButtonWidget(
        (this.width - BUTTON_WIDTH) / 2,
        0,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        text,
        action)));
  }

  @Environment(value = EnvType.CLIENT)
  public class Entry extends ElementListWidget.Entry<Entry> {
    private final ButtonWidget button;

    public Entry(ButtonWidget button) {
      this.button = button;
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
      this.button.y = y + (entryHeight - BUTTON_HEIGHT) / 2;
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
  }
}
