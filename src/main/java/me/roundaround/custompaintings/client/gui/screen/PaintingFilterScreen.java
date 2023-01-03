package me.roundaround.custompaintings.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PaintingFilterScreen extends Screen {
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 20;

  private final PaintingEditScreen parent;

  public PaintingFilterScreen(PaintingEditScreen parent) {
    super(Text.translatable("custompaintings.filter.title"));
    this.parent = parent;
  }

  @Override
  public void init() {
    ButtonWidget closeButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH / 2,
        height / 2 - BUTTON_HEIGHT / 2,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.filter.close"),
        (button) -> {
          client.setScreen(parent);
        });

    addDrawableChild(closeButton);
  }
}
