package me.roundaround.custompaintings.client.gui.screen.editor.view;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.gui.widget.ClickableWidget;

public class ViewManager implements Closeable {
  private final Consumer<View> onTransition;
  private final Consumer<ClickableWidget> adder;
  private final Consumer<ClickableWidget> remover;

  @Nullable
  private View current;

  public ViewManager(
      Consumer<View> onTransition,
      Consumer<ClickableWidget> adder,
      Consumer<ClickableWidget> remover) {
    this.onTransition = onTransition;
    this.adder = adder;
    this.remover = remover;
  }

  public View getCurrent() {
    return this.current;
  }

  public void init(View view) {
    this.current = view;
    view.forEachChild(this.adder);
    view.refreshPositions();
  }

  public void set(View view, boolean playSound) {
    if (Objects.equals(this.current, view)) {
      return;
    }

    boolean transitioned = false;

    if (this.current != null) {
      this.current.forEachChild(this.remover);
      try {
        this.current.close();
      } catch (IOException e) {
        // TODO: Proper handling
        CustomPaintingsMod.LOGGER.error("Error closing view", e);
      }
      transitioned = true;
    }

    this.current = view;
    view.forEachChild(this.adder);
    view.refreshPositions();

    if (playSound) {
      GuiUtil.playClickSound();
    }

    if (transitioned) {
      this.onTransition.accept(view);
    }
  }

  public void refreshPositions() {
    if (this.current != null) {
      this.current.refreshPositions();
    }
  }

  @Override
  public void close() throws IOException {
    if (this.current != null) {
      this.current.close();
    }
  }
}
