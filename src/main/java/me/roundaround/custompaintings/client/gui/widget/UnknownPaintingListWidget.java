package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashSet;
import java.util.stream.Collectors;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class UnknownPaintingListWidget
    extends AlwaysSelectedEntryListWidget<UnknownPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;

  private final UnknownPaintingsScreen parent;
  private final LoadingEntry loadingEntry;

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

    // Get list of known painting ids
    CustomPaintingsClientMod.customPaintingManager
        .getEntries()
        .stream()
        .map(PaintingData::id)
        .collect(Collectors.toList());

    narrateScreenIfNarrationEnabled();
  }

  private void narrateScreenIfNarrationEnabled() {
    this.parent.narrateScreenIfNarrationEnabled(true);
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);

    if (entry instanceof UnknownPaintingEntry) {
      this.parent.setSelectedId(((UnknownPaintingEntry) entry).getId());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
  }

  @Environment(value = EnvType.CLIENT)
  public class LoadingEntry extends Entry {
    private static final Text LOADING_LIST_TEXT = Text.translatable("custompaintings.outdated.loading");

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

    public Identifier getId() {
      return this.unknownPainting.id();
    }

    public boolean canBeAutoFixed() {
      return this.unknownPainting.autoFixId() != null;
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
          Text.literal(this.unknownPainting.id().toString() + " (" + this.unknownPainting.count() + ")")
              .asOrderedText(),
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.unknownPainting.id().toString());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      UnknownPaintingListWidget.this.setSelected(this);

      if (Util.getMeasuringTimeMs() - this.time < 250L) {
        UnknownPaintingListWidget.this.parent.confirmSelection();
        return true;
      }

      this.time = Util.getMeasuringTimeMs();
      return false;
    }
  }
}
