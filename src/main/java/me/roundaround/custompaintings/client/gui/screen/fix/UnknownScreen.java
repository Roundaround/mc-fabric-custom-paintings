package me.roundaround.custompaintings.client.gui.screen.fix;

import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.network.CustomId;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnknownScreen extends Screen implements ListUnknownListener {
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  private UnknownList list;

  public UnknownScreen(Screen parent) {
    super(Text.of("Paintings with unknown data"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new UnknownList(this.client, this.layout));
    ClientNetworking.sendListUnknownPacket();

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  @Override
  public void onListUnknownResponse(Map<CustomId, Integer> counts) {
    if (this.list == null) {
      return;
    }
    this.list.setUnknownCounts(counts);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private static class UnknownList extends NarratableEntryListWidget<UnknownList.Entry> {
    public UnknownList(MinecraftClient client, ThreeSectionLayoutWidget layout) {
      super(client, layout);
    }

    public void setUnknownCounts(Map<CustomId, Integer> counts) {
      // TODO: Implement
    }

    private static abstract class Entry extends NarratableEntryListWidget.Entry {
      public Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }
  }
}
