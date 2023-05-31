package me.roundaround.custompaintings.client.gui.widget;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

@Environment(value = EnvType.CLIENT)
public class ButtonListWidget extends ElementListWidget<ButtonListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;
  private static final int BUTTON_WIDTH = 204;
  private static final int BUTTON_HEIGHT = 20;

  public ButtonListWidget(
      MinecraftClient client, int width, int height, int top, int bottom) {
    super(client, width, height, top, bottom, ITEM_HEIGHT);
  }

  public void addEntry(Text text, ButtonWidget.PressAction action) {
    this.addEntry(new Entry(ButtonWidget.builder(text, action)
        .position((this.width - BUTTON_WIDTH) / 2, 0)
        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
        .build()));
  }

  @Environment(value = EnvType.CLIENT)
  public class Entry extends ElementListWidget.Entry<Entry> {
    private final ButtonWidget button;

    public Entry(ButtonWidget button) {
      this.button = button;
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
      this.button.setY(y + (entryHeight - BUTTON_HEIGHT) / 2);
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
  }
}
