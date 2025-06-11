package me.roundaround.custompaintings.client.gui.screen.editor.image;

import java.util.function.Consumer;

import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public abstract class ImageTab implements Tab {
  protected final MinecraftClient client;
  protected final TextRenderer textRenderer;
  protected final Text title;
  protected final LinearLayoutWidget layout = LinearLayoutWidget.vertical()
      .spacing(GuiUtil.PADDING)
      .padding(GuiUtil.PADDING);

  public ImageTab(MinecraftClient client, Text title) {
    this.client = client;
    this.textRenderer = client.textRenderer;
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
        tabArea.getLeft(),
        tabArea.getTop(),
        tabArea.width(),
        tabArea.height());
    this.layout.refreshPositions();
  }
}
