package me.roundaround.custompaintings.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.Widget;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class EmptyWidget implements Widget {
  private int x;
  private int y;
  private int width;
  private int height;

  public EmptyWidget(int width, int height) {
    this(0, 0, width, height);
  }

  public EmptyWidget(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public static EmptyWidget ofWidth(int width) {
    return new EmptyWidget(width, 0);
  }

  public static EmptyWidget ofHeight(int height) {
    return new EmptyWidget(0, height);
  }

  @Override
  public void setX(int x) {
    this.x = x;
  }

  @Override
  public void setY(int y) {
    this.y = y;
  }

  @Override
  public int getX() {
    return this.x;
  }

  @Override
  public int getY() {
    return this.y;
  }

  @Override
  public int getWidth() {
    return this.width;
  }

  @Override
  public int getHeight() {
    return this.height;
  }

  @Override
  public void forEachChild(Consumer<ClickableWidget> consumer) {
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setDimensions(int width, int height) {
    this.setWidth(width);
    this.setHeight(height);
  }

  public void setDimensionsAndPosition(int width, int height, int x, int y) {
    this.setDimensions(width, height);
    this.setPosition(x, y);
  }
}
