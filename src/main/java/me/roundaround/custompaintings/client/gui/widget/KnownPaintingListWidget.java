package me.roundaround.custompaintings.client.gui.widget;

import java.util.Collection;

import me.roundaround.custompaintings.client.gui.screen.manage.ReassignScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class KnownPaintingListWidget
    extends AlwaysSelectedEntryListWidget<KnownPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;

  private final ReassignScreen parent;

  public KnownPaintingListWidget(
      ReassignScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.parent = parent;
  }

  public void setPaintings(Collection<PaintingData> paintings) {
    clearEntries();
    for (PaintingData painting : paintings) {
      addEntry(new Entry(this.client, painting));
    }
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);
    this.parent.setSelectedId(entry.getPainting().id());
  }

  @Environment(value = EnvType.CLIENT)
  public class Entry
      extends AlwaysSelectedEntryListWidget.Entry<Entry> {
    private final MinecraftClient client;
    private final PaintingData painting;

    private long time;

    public Entry(MinecraftClient client, PaintingData painting) {
      this.client = client;
      this.painting = painting;
    }

    public PaintingData getPainting() {
      return this.painting;
    }

    @Override
    public void render(
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawCenteredTextWithShadow(
          matrixStack,
          this.client.textRenderer,
          Text.literal(this.painting.id().toString()).asOrderedText(),
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.painting.id().toString());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      KnownPaintingListWidget.this.setSelected(this);

      if (Util.getMeasuringTimeMs() - this.time < 250L) {
        KnownPaintingListWidget.this.parent.confirmSelection();
        return true;
      }

      this.time = Util.getMeasuringTimeMs();
      return false;
    }
  }
}
