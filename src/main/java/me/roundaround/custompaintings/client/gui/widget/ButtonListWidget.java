package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;

@Environment(value = EnvType.CLIENT)
public class ButtonListWidget extends FlowListWidget<ButtonListWidget.Entry> {
  private static final int BUTTON_WIDTH = 204;
  private static final int BUTTON_HEIGHT = 20;

  public ButtonListWidget(MinecraftClient client, ThreePartsLayoutWidget layout) {
    super(client, layout);
  }

  public void addEntry(Text text, ButtonWidget.PressAction action) {
    this.addEntry((index, x, y, width) -> new Entry(text, action, index, x, y, width));
  }

  @Environment(value = EnvType.CLIENT)
  public static class Entry extends FlowListWidget.Entry {
    private final ButtonWidget button;

    public Entry(Text text, ButtonWidget.PressAction action, int index, int x, int y, int width) {
      super(index, x, y, width, BUTTON_HEIGHT);
      this.button = ButtonWidget.builder(text, action)
          .position(this.getButtonX(), this.getButtonY())
          .size(BUTTON_WIDTH, BUTTON_HEIGHT)
          .build();
      this.addDrawableChild(this.button);
    }

    @Override
    public void refreshPositions() {
      this.button.setPosition(this.getButtonX(), this.getButtonY());
    }

    private int getButtonX() {
      return this.getContentCenterX() - BUTTON_WIDTH / 2;
    }

    private int getButtonY() {
      return this.getContentCenterY() - BUTTON_HEIGHT / 2;
    }
  }
}
