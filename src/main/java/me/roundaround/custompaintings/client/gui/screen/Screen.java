package me.roundaround.custompaintings.client.gui.screen;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class Screen extends net.minecraft.client.gui.screen.Screen {
  protected final @NotNull Parent parent;
  protected final @NotNull MinecraftClient client;

  public Screen(@NotNull Text title, @NotNull Parent parent, @NotNull MinecraftClient client) {
    super(title);
    this.parent = parent;
    this.client = client;
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent.get());
  }

  protected void navigateTo(Screen screen) {
    this.client.setScreen(screen);
  }

  protected void done(ButtonWidget button) {
    this.close();
  }
}
