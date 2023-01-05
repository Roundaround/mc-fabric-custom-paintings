package me.roundaround.custompaintings.client.gui.widget;

import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.page.PaintingSelectPage;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(value = EnvType.CLIENT)
public class PaintingListWidget
    extends AlwaysSelectedEntryListWidget<PaintingListWidget.PaintingEntry> {
  private final PaintingSelectPage page;
  private final PaintingEditScreen parent;

  public PaintingListWidget(
      PaintingSelectPage page,
      PaintingEditScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, 36);
    this.page = page;
    this.parent = parent;

    this.parent.getCurrentGroup().paintings().forEach((paintingData) -> {
      PaintingEntry entry = new PaintingEntry(this.getEntryCount(), paintingData);
      int i = this.addEntry(entry);
      if (this.parent.getCurrentPainting() == i) {
        this.setSelected(entry);
      }
    });
    setScrollAmount(this.page.getScrollAmount());
  }

  @Override
  protected boolean isFocused() {
    return parent.getFocused() == this;
  }

  @Override
  public void setSelected(PaintingEntry entry) {
    super.setSelected(entry);
    this.parent.setCurrentPainting(entry.index);
  }

  @Override
  public void setScrollAmount(double amount) {
    super.setScrollAmount(amount);
    this.page.setScrollAmount(amount);
  }

  @Override
  protected int getScrollbarPositionX() {
    return this.width - 6;
  }

  @Override
  public int getRowWidth() {
    return this.width - (Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)) > 0 ? 18 : 12);
  }

  @Override
  public int getRowLeft() {
    return this.left + 4;
  }

  @Override
  protected int getMaxPosition() {
    return super.getMaxPosition() + 4;
  }

  @Environment(value = EnvType.CLIENT)
  public class PaintingEntry extends AlwaysSelectedEntryListWidget.Entry<PaintingEntry> {
    private final int index;
    private final PaintingData paintingData;

    private Sprite sprite;

    public PaintingEntry(int index, PaintingData paintingData) {
      this.index = index;
      this.paintingData = paintingData;
      this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
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
      int maxHeight = PaintingListWidget.this.itemHeight - 4;
      int maxWidth = maxHeight;

      int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
      int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

      RenderSystem.setShader(GameRenderer::getPositionTexShader);

      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
      RenderSystem.setShaderTexture(0, this.sprite.getAtlas().getId());
      drawSprite(matrixStack, x + 4 + (maxWidth - scaledWidth) / 2, y + (entryHeight - scaledHeight) / 2, 1,
          scaledWidth, scaledHeight, sprite);

      TextRenderer textRenderer = PaintingListWidget.this.client.textRenderer;
      int posX = x + maxWidth + 4 + 4;
      int posY = y + (entryHeight - 3 * textRenderer.fontHeight - 2 * 2) / 2;

      if (paintingData.hasName() || paintingData.hasArtist()) {
        ArrayList<OrderedText> parts = new ArrayList<>();
        if (paintingData.hasName()) {
          parts.add(Text.literal("\"" + paintingData.name() + "\"").asOrderedText());
        }
        if (paintingData.hasName() && paintingData.hasArtist()) {
          parts.add(Text.of(" - ").asOrderedText());
        }
        if (paintingData.hasArtist()) {
          parts.add(OrderedText.styledForwardsVisitedString(
              paintingData.artist(),
              Style.EMPTY.withItalic(true)));
        }

        drawWithShadow(
            matrixStack,
            textRenderer,
            OrderedText.concat(parts),
            posX,
            posY,
            0xFFFFFFFF);

        posY += textRenderer.fontHeight + 2;
      }

      drawTextWithShadow(
          matrixStack,
          textRenderer,
          Text.literal("(" + paintingData.id().toString() + ")")
              .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
          posX,
          posY,
          0xFFFFFFFF);

      posY += textRenderer.fontHeight + 2;

      drawTextWithShadow(
          matrixStack,
          textRenderer,
          Text.translatable(
              "custompaintings.painting.dimensions",
              paintingData.width(),
              paintingData.height()),
          posX,
          posY,
          0xFFFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal("placeholder");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      PaintingListWidget.this.setSelected(this);
      return true;
    }
  }
}
