package me.roundaround.custompaintings.client.gui.screen.editor.view;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import net.minecraft.client.gui.widget.ClickableWidget;

public abstract class View implements Closeable {
  public void init() {
  }

  @Override
  public void close() throws IOException {
  }

  public void refreshPositions() {
  }

  public abstract void forEachChild(Consumer<ClickableWidget> consumer);
}
