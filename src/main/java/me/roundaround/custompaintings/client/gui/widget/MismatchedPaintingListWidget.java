package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.client.gui.screen.manage.OutdatedPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.util.MismatchedPainting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class MismatchedPaintingListWidget extends ElementListWidget<MismatchedPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;

  private final OutdatedPaintingsScreen parent;
  private final LoadingEntry loadingEntry;

  public MismatchedPaintingListWidget(
      OutdatedPaintingsScreen parent,
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
    ClientNetworking.sendRequestMismatchedPacket();
    clearEntries();
    addEntry(this.loadingEntry);
  }

  public void receiveData(HashSet<MismatchedPainting> data) {
    clearEntries();
    for (MismatchedPainting outdatedPainting : data) {
      this.addEntry(new OutdatedPaintingEntry(this.client, outdatedPainting));
    }
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
  public class OutdatedPaintingEntry extends Entry {
    private final MinecraftClient client;
    private final MismatchedPainting outdatedPainting;
    private final IconButtonWidget fixButton;

    public OutdatedPaintingEntry(
        MinecraftClient client,
        MismatchedPainting outdatedPainting) {
      this.client = client;
      this.outdatedPainting = outdatedPainting;

      this.fixButton = new IconButtonWidget(
          this.client,
          MismatchedPaintingListWidget.this.getRowRight() - IconButtonWidget.WIDTH - 4,
          0,
          IconButtonWidget.RESET_ICON, // TODO: Swap to new icon when available
          Text.translatable("custompaintings.outdated.fix"),
          (button) -> {
            ClientNetworking.sendUpdatePaintingPacket(this.outdatedPainting.uuid());
          });
    }

    public MismatchedPainting getOutdatedPainting() {
      return outdatedPainting;
    }

    @Override
    public List<? extends Element> children() {
      return ImmutableList.of(this.fixButton);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
      return ImmutableList.of(this.fixButton);
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
      drawTextWithShadow(
          matrixStack,
          this.client.textRenderer,
          Text.literal(this.outdatedPainting.uuid().toString()),
          x + 4,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);

      this.fixButton.y = y + (entryHeight - IconButtonWidget.HEIGHT) / 2;
      this.fixButton.render(matrixStack, mouseX, mouseY, partialTicks);
    }
  }
}
