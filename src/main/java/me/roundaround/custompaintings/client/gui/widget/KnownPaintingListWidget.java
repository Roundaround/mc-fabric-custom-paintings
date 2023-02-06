package me.roundaround.custompaintings.client.gui.widget;

import java.util.Collection;
import java.util.HashSet;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.manage.ReassignScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class KnownPaintingListWidget
    extends AlwaysSelectedEntryListWidget<KnownPaintingListWidget.Entry> {
  private static final int ITEM_HEIGHT = 36;

  private final ReassignScreen parent;

  private Collection<PaintingData> paintings = new HashSet<>();
  private String filter = "";

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
    this.paintings = paintings;
    populateList();
  }

  public void setFilter(String filter) {
    this.filter = filter.toLowerCase();
    populateList();
  }

  private void populateList() {
    clearEntries();
    for (PaintingData painting : paintings) {
      if (painting.id().toString().toLowerCase().contains(filter)) {
        addEntry(new Entry(this.client, painting));
      }
    }
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);
    this.parent.setSelectedId(entry.getPaintingData().id());
  }

  @Environment(value = EnvType.CLIENT)
  public class Entry
      extends AlwaysSelectedEntryListWidget.Entry<Entry> {
    private final MinecraftClient client;
    private final PaintingData paintingData;
    private final Sprite sprite;

    private long time;

    public Entry(MinecraftClient client, PaintingData painting) {
      this.client = client;
      this.paintingData = painting;
      this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
    }

    public PaintingData getPaintingData() {
      return this.paintingData;
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
      int maxHeight = entryHeight - 4;
      int maxWidth = maxHeight;

      int scaledWidth = this.paintingData.getScaledWidth(maxWidth, maxHeight);
      int scaledHeight = this.paintingData.getScaledHeight(maxWidth, maxHeight);

      RenderSystem.setShader(GameRenderer::getPositionTexShader);

      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
      RenderSystem.setShaderTexture(0, this.sprite.getAtlas().getId());
      drawSprite(
          matrixStack,
          x + 4 + (maxWidth - scaledWidth) / 2,
          y + (entryHeight - scaledHeight) / 2,
          1,
          scaledWidth,
          scaledHeight,
          this.sprite);

      TextRenderer textRenderer = this.client.textRenderer;
      int textWidth = entryWidth - 4 - maxWidth - 4 - 8;
      int posX = x + maxWidth + 4 + 4;
      int posY = y + MathHelper.ceil((entryHeight - 3 * textRenderer.fontHeight - 2 * 2) / 2f);

      if (this.paintingData.hasLabel()) {
        StringVisitable label = this.paintingData.getLabel();
        if (textRenderer.getWidth(label) > textWidth) {
          Text ellipsis = Text.literal("...");
          label = StringVisitable.concat(
              textRenderer.trimToWidth(label, textWidth - textRenderer.getWidth(ellipsis)),
              ellipsis);
        }

        textRenderer.draw(
            matrixStack,
            Language.getInstance().reorder(label),
            posX,
            posY,
            0xFFFFFFFF);

        posY += textRenderer.fontHeight + 2;
      }

      StringVisitable id = Text.literal("(" + this.paintingData.id().toString() + ")")
          .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      if (textRenderer.getWidth(id) > textWidth) {
        Text ellipsis = Text.literal("...")
            .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
        id = StringVisitable.concat(
            textRenderer.trimToWidth(id, textWidth - textRenderer.getWidth(ellipsis)),
            ellipsis);
      }

      textRenderer.draw(
          matrixStack,
          Language.getInstance().reorder(id),
          posX,
          posY,
          0xFFFFFFFF);

      posY += textRenderer.fontHeight + 2;

      textRenderer.draw(
          matrixStack,
          Text.translatable(
              "custompaintings.painting.dimensions",
              this.paintingData.width(),
              this.paintingData.height()),
          posX,
          posY,
          0xFFFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.paintingData.id().toString());
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
