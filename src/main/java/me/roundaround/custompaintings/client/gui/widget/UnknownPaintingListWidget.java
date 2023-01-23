package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.manage.ReassignScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.UnknownPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class UnknownPaintingListWidget extends ElementListWidget<UnknownPaintingListWidget.Entry> {
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

  public void receiveData(HashMap<Identifier, Integer> data) {
    clearEntries();
    for (Identifier id : data.keySet()) {
      addEntry(new UnknownPaintingEntry(this.client, id, data.get(id)));
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

  @Environment(value = EnvType.CLIENT)
  public abstract class Entry extends ElementListWidget.Entry<Entry> {
  }

  @Environment(value = EnvType.CLIENT)
  public class LoadingEntry extends Entry {
    private static final Text LOADING_LIST_TEXT = Text.translatable("custompaintings.outdated.loading");

    private final MinecraftClient client;

    public LoadingEntry(MinecraftClient client) {
      this.client = client;
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of();
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of();
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
  }

  @Environment(value = EnvType.CLIENT)
  public class UnknownPaintingEntry extends Entry {
    private final MinecraftClient client;
    private final Identifier id;
    private final int count;
    private final ButtonWidget button;

    public UnknownPaintingEntry(
        MinecraftClient client,
        Identifier id,
        int count) {
      this.client = client;
      this.id = id;
      this.count = count;
      this.button = new ButtonWidget(
          0,
          0,
          UnknownPaintingListWidget.this.getRowWidth(),
          20,
          Text.literal(this.id.toString() + " (" + this.count + ")"),
          (button) -> {
            this.client.setScreen(new ReassignScreen(UnknownPaintingListWidget.this.parent, this.id));
          });
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of(this.button);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of(this.button);
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
      this.button.y = y + (entryHeight - 20) / 2;
      this.button.render(matrixStack, mouseX, mouseY, partialTicks);
    }
  }
}
