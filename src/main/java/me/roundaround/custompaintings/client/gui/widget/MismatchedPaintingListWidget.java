package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableList;

import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
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

  private final MismatchedPaintingsScreen parent;
  private final LoadingEntry loadingEntry;
  private final EmptyEntry emptyEntry;

  public MismatchedPaintingListWidget(
      MismatchedPaintingsScreen parent,
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
    ClientNetworking.sendRequestMismatchedPacket();
    clearEntries();
    addEntry(this.loadingEntry);
  }

  public void receiveData(HashSet<MismatchedPainting> data) {
    clearEntries();
    for (MismatchedPainting mismatchedPainting : data) {
      this.addEntry(new MismatchedPaintingEntry(this.client, mismatchedPainting));
    }
    if (data.isEmpty()) {
      this.addEntry(this.emptyEntry);
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
          Text.translatable("custompaintings.mismatched.loading"),
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
  public class EmptyEntry extends Entry {
    private final MinecraftClient client;

    public EmptyEntry(MinecraftClient client) {
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
      drawCenteredText(
          matrixStack,
          this.client.textRenderer,
          Text.translatable("custompaintings.mismatched.empty"),
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class MismatchedPaintingEntry extends Entry {
    private final MinecraftClient client;
    private final MismatchedPainting mismatchedPainting;
    private final IconButtonWidget fixButton;

    public MismatchedPaintingEntry(
        MinecraftClient client,
        MismatchedPainting mismatchedPainting) {
      this.client = client;
      this.mismatchedPainting = mismatchedPainting;

      this.fixButton = new IconButtonWidget(
          this.client,
          MismatchedPaintingListWidget.this.getRowRight() - IconButtonWidget.WIDTH - 4,
          0,
          IconButtonWidget.WRENCH_ICON,
          Text.translatable("custompaintings.mismatched.fix"),
          (button) -> {
            ClientNetworking.sendUpdatePaintingPacket(this.mismatchedPainting.uuid());
          });
    }

    public MismatchedPainting getMismatchedPainting() {
      return mismatchedPainting;
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
          Text.literal(this.mismatchedPainting.uuid().toString()),
          x + 4,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);

      this.fixButton.y = y + (entryHeight - IconButtonWidget.HEIGHT) / 2;
      this.fixButton.render(matrixStack, mouseX, mouseY, partialTicks);
    }
  }
}
