package me.roundaround.custompaintings.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Environment(value = EnvType.CLIENT)
public class MismatchedPaintingListWidget extends ElementListWidget<MismatchedPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 36;

  private final MismatchedPaintingsScreen parent;
  private final LoadingEntry loadingEntry;
  private final EmptyEntry emptyEntry;

  public MismatchedPaintingListWidget(
      MismatchedPaintingsScreen parent, MinecraftClient minecraftClient, int width, int height, int y
  ) {
    super(minecraftClient, width, height, y, ITEM_HEIGHT);

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
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks
    ) {
      int yPos = y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f);

      drawContext.drawCenteredTextWithShadow(this.client.textRenderer,
          Text.translatable("custompaintings.mismatched.loading"), this.client.currentScreen.width / 2, yPos, 0xFFFFFF
      );

      drawContext.drawCenteredTextWithShadow(this.client.textRenderer, LoadingDisplay.get(Util.getMeasuringTimeMs()),
          this.client.currentScreen.width / 2, yPos + this.client.textRenderer.fontHeight, 0x808080
      );
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
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks
    ) {
      drawContext.drawCenteredTextWithShadow(this.client.textRenderer,
          Text.translatable("custompaintings.mismatched.empty"), this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f), 0xFFFFFF
      );
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class MismatchedPaintingEntry extends Entry {
    private final MinecraftClient client;
    private final MismatchedPainting mismatchedPainting;
    private final IconButtonWidget fixButton;
    private final Sprite sprite;

    public MismatchedPaintingEntry(MinecraftClient client, MismatchedPainting mismatchedPainting) {
      this.client = client;
      this.mismatchedPainting = mismatchedPainting;
      this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(mismatchedPainting.currentData());

      this.fixButton = IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.FIX_18, CustomPaintingsMod.MOD_ID)
          .vanillaSize()
          .messageAndTooltip(Text.translatable("custompaintings.mismatched.fix"))
          .onPress((button) -> {
            ClientNetworking.sendUpdatePaintingPacket(this.mismatchedPainting.uuid());
          })
          .position(MismatchedPaintingListWidget.this.getRowRight() - IconButtonWidget.SIZE_V - GuiUtil.PADDING, 0)
          .build();
    }

    public MismatchedPainting getMismatchedPainting() {
      return this.mismatchedPainting;
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
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks
    ) {
      PaintingData paintingData = this.mismatchedPainting.currentData();
      int maxHeight = entryHeight - 4;
      int maxWidth = maxHeight;

      int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
      int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
      RenderSystem.setShaderTexture(0, this.sprite.getAtlasId());
      drawContext.drawSprite(x + 4 + (maxWidth - scaledWidth) / 2, y + (entryHeight - scaledHeight) / 2, 1, scaledWidth,
          scaledHeight, this.sprite
      );

      TextRenderer textRenderer = this.client.textRenderer;
      int textWidth = entryWidth - maxWidth - IconButtonWidget.SIZE_V - 6 * GuiUtil.PADDING;
      int posX = x + maxWidth + 4 + 4;
      int posY = y + MathHelper.ceil((entryHeight - 3 * textRenderer.fontHeight - 2 * 2) / 2f);

      if (paintingData.hasLabel()) {
        StringVisitable label = paintingData.getLabel();
        if (textRenderer.getWidth(label) > textWidth) {
          Text ellipsis = Text.literal("...");
          label = StringVisitable.concat(
              textRenderer.trimToWidth(label, textWidth - textRenderer.getWidth(ellipsis)), ellipsis);
        }

        drawContext.drawText(textRenderer, Language.getInstance().reorder(label), posX, posY, 0xFFFFFFFF, false);

        posY += textRenderer.fontHeight + 2;
      }

      StringVisitable id = Text.literal("(" + paintingData.id().toString() + ")")
          .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      if (textRenderer.getWidth(id) > textWidth) {
        Text ellipsis = Text.literal("...").setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
        id = StringVisitable.concat(
            textRenderer.trimToWidth(id, textWidth - textRenderer.getWidth(ellipsis)), ellipsis);
      }

      drawContext.drawText(textRenderer, Language.getInstance().reorder(id), posX, posY, 0xFFFFFFFF, false);

      posY += textRenderer.fontHeight + 2;

      ArrayList<Text> outdated = new ArrayList<>();
      PaintingData knownData = this.mismatchedPainting.knownData();
      if (paintingData.isMismatched(knownData, MismatchedCategory.SIZE)) {
        outdated.add(Text.translatable("custompaintings.mismatched.outdated.size"));
      }
      if (paintingData.isMismatched(knownData, MismatchedCategory.INFO)) {
        outdated.add(Text.translatable("custompaintings.mismatched.outdated.info"));
      }

      String outdatedString = outdated.stream().map(Text::getString).collect(Collectors.joining(", "));

      drawContext.drawText(textRenderer, Text.translatable("custompaintings.mismatched.outdated", outdatedString), posX,
          posY, 0xFFFFFFFF, false
      );

      this.fixButton.setY(y + (entryHeight - IconButtonWidget.SIZE_V) / 2);
      this.fixButton.render(drawContext, mouseX, mouseY, partialTicks);
    }
  }
}
