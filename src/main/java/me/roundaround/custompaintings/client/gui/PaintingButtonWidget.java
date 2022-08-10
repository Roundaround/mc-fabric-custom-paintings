package me.roundaround.custompaintings.client.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PaintingButtonWidget extends ButtonWidget {
  private Identifier id;

  public PaintingButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, Identifier paintingId) {
    super(x, y, width, height, message, onPress);
  }
}
