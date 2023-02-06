package me.roundaround.custompaintings.client.gui.widget;

import java.util.function.Consumer;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class IntSliderWidget extends SliderWidget {
  private final int min;
  private final int max;
  private final Function<Integer, Text> messageFactory;
  private final Consumer<Integer> valueConsumer;

  public IntSliderWidget(
      int x,
      int y,
      int width,
      int height,
      int value,
      int min,
      int max,
      Function<Integer, Text> messageFactory,
      Consumer<Integer> valueConsumer) {
    super(x, y, width, height, Text.empty(), getDoubleValue(value, min, max));
    this.min = min;
    this.max = max;
    this.messageFactory = messageFactory;
    this.valueConsumer = valueConsumer;

    updateMessage();
  }

  @Override
  protected void updateMessage() {
    this.setMessage(this.messageFactory.apply(this.getIntValue()));
  }

  @Override
  protected void applyValue() {
    this.valueConsumer.accept(this.getIntValue());
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_LEFT) {
      this.setValue(this.getIntValue() - 1);
      return true;
    } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
      this.setValue(this.getIntValue() + 1);
      return true;
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  public void setValue(int value) {
    double previousValue = this.value;
    this.value = getDoubleValue(value, this.min, this.max);

    if (previousValue != this.value) {
      applyValue();
    }

    updateMessage();
  }

  public int getIntValue() {
    return getIntValue(this.value, this.min, this.max);
  }

  private static int getIntValue(double value, int min, int max) {
    return (int) (value * (max - min) + min);
  }

  private static double getDoubleValue(int value, int min, int max) {
    return MathHelper.clamp((double) (value - min) / (double) (max - min), 0.0, 1.0);
  }
}
