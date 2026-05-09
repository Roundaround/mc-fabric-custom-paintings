package me.roundaround.custompaintings.client.gui.widget;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

public class LoadingButtonWidget extends Button.Plain {
  private boolean loading = false;

  public LoadingButtonWidget(
      int x,
      int y,
      int width,
      int height,
      net.minecraft.network.chat.Component message,
      OnPress onPress
  ) {
    super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
  }

  public void setLoading(boolean loading) {
    this.loading = loading;
    this.active = !loading;
  }

  public boolean isLoading() {
    return this.loading;
  }

  @Override
  public void extractDefaultLabel(@NotNull ActiveTextCollector drawer) {
    if (!this.loading) {
      super.extractDefaultLabel(drawer);
      return;
    }

    String spinner = LoadingDotsText.get(Util.getMillis());
    this.extractScrollingStringOverContents(drawer, net.minecraft.network.chat.Component.literal(spinner), 2);
  }
}
