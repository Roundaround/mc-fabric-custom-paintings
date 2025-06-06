package me.roundaround.custompaintings.client.gui.screen;

import java.util.function.Supplier;

import net.minecraft.client.gui.screen.Screen;

public class Parent {
  private final boolean isDirect;
  private final Screen parent;
  private final Supplier<Screen> supplier;

  public Parent(Screen parent) {
    this.isDirect = true;
    this.parent = parent;
    this.supplier = null;
  }

  public Parent(Supplier<Screen> supplier) {
    this.isDirect = false;
    this.parent = null;
    this.supplier = supplier;
  }

  public Screen get() {
    return this.isDirect ? this.parent : this.supplier.get();
  }

  public boolean isDirect() {
    return this.isDirect;
  }
}
