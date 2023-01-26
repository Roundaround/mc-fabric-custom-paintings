package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashSet;

import me.roundaround.custompaintings.client.gui.screen.manage.UnknownPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class UnknownPaintingListWidget
    extends AlwaysSelectedEntryListWidget<UnknownPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;

  private final UnknownPaintingsScreen parent;
  private final LoadingEntry loadingEntry;
  private final EmptyEntry emptyEntry;

  public UnknownPaintingListWidget(
      UnknownPaintingsScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);

    this.parent = parent;
    this.loadingEntry = new LoadingEntry(minecraftClient);
    this.emptyEntry = new EmptyEntry(minecraftClient);

    this.loadData();
  }

  public void loadData() {
    ClientNetworking.sendRequestUnknownPacket();
    clearEntries();
    addEntry(this.loadingEntry);
  }

  public void receiveData(HashSet<UnknownPainting> data) {
    clearEntries();
    for (UnknownPainting unknownPainting : data) {
      addEntry(new UnknownPaintingEntry(this.client, unknownPainting));
    }
    if (data.isEmpty()) {
      addEntry(this.emptyEntry);
    }
    narrateScreenIfNarrationEnabled();
  }

  private void narrateScreenIfNarrationEnabled() {
    this.parent.narrateScreenIfNarrationEnabled(true);
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);

    if (entry instanceof UnknownPaintingEntry) {
      this.parent.setSelected(((UnknownPaintingEntry) entry).getUnknownPainting());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
  }

  @Environment(value = EnvType.CLIENT)
  public class LoadingEntry extends Entry {
    private static final Text LOADING_LIST_TEXT = Text.translatable("custompaintings.unknown.loading");

    private final MinecraftClient client;

    public LoadingEntry(MinecraftClient client) {
      this.client = client;
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
      int yPos = y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f);

      drawCenteredText(
          matrixStack,
          this.client.textRenderer,
          LOADING_LIST_TEXT,
          this.client.currentScreen.width / 2,
          yPos,
          0xFFFFFF);

      drawCenteredText(
          matrixStack,
          this.client.textRenderer,
          LoadingDisplay.get(Util.getMeasuringTimeMs()),
          this.client.currentScreen.width / 2,
          yPos + this.client.textRenderer.fontHeight,
          0x808080);
    }

    @Override
    public Text getNarration() {
      return LOADING_LIST_TEXT;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class EmptyEntry extends Entry {
    private static final Text EMPTY_LIST_TEXT = Text.translatable("custompaintings.unknown.empty");

    private final MinecraftClient client;

    public EmptyEntry(MinecraftClient client) {
      this.client = client;
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
      drawCenteredText(
          matrixStack,
          this.client.textRenderer,
          EMPTY_LIST_TEXT,
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return EMPTY_LIST_TEXT;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class UnknownPaintingEntry extends Entry {
    private final MinecraftClient client;
    private final UnknownPainting unknownPainting;

    private long time;

    public UnknownPaintingEntry(
        MinecraftClient client,
        UnknownPainting unknownPainting) {
      this.client = client;
      this.unknownPainting = unknownPainting;
    }

    public UnknownPainting getUnknownPainting() {
      return this.unknownPainting;
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
      int centerY = y + entryHeight / 2;
      PaintingData currentData = this.unknownPainting.currentData();

      this.client.textRenderer.draw(
          matrixStack,
          Text.literal(currentData.id().toString()),
          x,
          centerY - this.client.textRenderer.fontHeight - 2,
          0xFF8080);

      if (currentData.hasLabel()) {
        this.client.textRenderer.draw(
            matrixStack,
            currentData.getLabel(),
            x,
            centerY + 2,
            0xFFFFFF);
      }
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.unknownPainting.currentData().id().toString());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      UnknownPaintingListWidget.this.setSelected(this);

      if (Util.getMeasuringTimeMs() - this.time < 250L) {
        UnknownPaintingListWidget.this.parent.reassignSelection();
        return true;
      }

      this.time = Util.getMeasuringTimeMs();
      return false;
    }
  }
}
