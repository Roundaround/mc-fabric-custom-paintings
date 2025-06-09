package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.Axis;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public abstract class PackEditorTab implements Tab {
  protected static final int PREFERRED_WIDTH = 300;

  protected final @NotNull MinecraftClient client;
  protected final @NotNull State state;
  protected final @NotNull Text title;
  protected final LinearLayoutWidget layout = new LinearLayoutWidget(Axis.VERTICAL)
      .mainAxisContentAlignStart()
      .defaultOffAxisContentAlignCenter()
      .spacing(0);

  protected PackEditorTab(
      @NotNull MinecraftClient client,
      @NotNull State state,
      @NotNull Text title) {
    this.client = client;
    this.state = state;
    this.title = title;
  }

  @Override
  public Text getTitle() {
    return this.title;
  }

  @Override
  public void forEachChild(Consumer<ClickableWidget> consumer) {
    this.layout.forEachChild(consumer);
  }

  @Override
  public void refreshGrid(ScreenRect tabArea) {
    this.layout.setPositionAndDimensions(
        tabArea.getLeft() + GuiUtil.PADDING,
        tabArea.getTop() + GuiUtil.PADDING,
        tabArea.width() - 2 * GuiUtil.PADDING,
        tabArea.height() - 2 * GuiUtil.PADDING);
    this.layout.refreshPositions();
  }

  protected int getContentWidth() {
    return Math.min(PREFERRED_WIDTH, this.layout.getWidth() - 2 * GuiUtil.PADDING);
  }
}
