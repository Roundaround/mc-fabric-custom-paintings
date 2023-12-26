package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.PaintingSelectScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Optional;

@Environment(value = EnvType.CLIENT)
public class PaintingListWidget
    extends AlwaysSelectedEntryListWidget<PaintingListWidget.PaintingEntry> {
  private static final int ITEM_HEIGHT = 36;

  private final PaintingSelectScreen parent;
  private final PaintingEditState state;

  public PaintingListWidget(
      PaintingSelectScreen parent,
      PaintingEditState state,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      ArrayList<PaintingData> paintings) {
    super(minecraftClient, width, height, top, ITEM_HEIGHT);
    this.parent = parent;
    this.state = state;

    setPaintings(paintings);
    setScrollAmount(this.parent.getScrollAmount());
  }

  public void setPaintings(ArrayList<PaintingData> paintings) {
    this.clearEntries();

    boolean selected = false;

    for (PaintingData paintingData : paintings) {
      PaintingEntry entry =
          paintingData.isEmpty() ? new EmptyPaintingEntry() : new PaintingEntry(paintingData);

      this.addEntry(entry);
      if (!paintingData.isEmpty() && this.state.getCurrentPainting().id() == paintingData.id()) {
        this.setSelected(entry);
        selected = true;
      }
    }

    if (!selected) {
      this.selectFirst();
    }
  }

  public Optional<PaintingData> getSelectedPainting() {
    PaintingEntry entry = this.getSelectedOrNull();
    if (entry == null) {
      return Optional.empty();
    }
    return Optional.of(entry.paintingData);
  }

  public void selectPainting(PaintingData paintingData) {
    Optional<PaintingData> selected = getSelectedPainting();
    if (selected.isPresent() && selected.get().id() == paintingData.id()) {
      return;
    }

    for (PaintingEntry entry : this.children()) {
      if (entry.paintingData.id() == paintingData.id()) {
        this.setSelected(entry);
        this.ensureVisible(entry);
        return;
      }
    }
  }

  public void selectFirst() {
    if (this.children().size() > 0) {
      this.setSelected(this.children().get(0));
    }
  }

  @Override
  public boolean isFocused() {
    return parent.getFocused() == this;
  }

  @Override
  public void setSelected(PaintingEntry entry) {
    super.setSelected(entry);
    this.state.setCurrentPainting(entry.paintingData);
  }

  @Override
  public void setScrollAmount(double amount) {
    super.setScrollAmount(amount);
    this.parent.setScrollAmount(amount);
  }

  @Override
  protected int getScrollbarPositionX() {
    return this.width - 6;
  }

  @Override
  public int getRowWidth() {
    return this.width -
        (Math.max(0, this.getMaxPosition() - (this.getBottom() - this.getY() - 4)) > 0 ? 18 : 12);
  }

  @Override
  protected PaintingEntry getNeighboringEntry(NavigationDirection direction) {
    return this.getNeighboringEntry(direction, (entry) -> !entry.paintingData.isEmpty());
  }

  @Environment(value = EnvType.CLIENT)
  public class PaintingEntry extends AlwaysSelectedEntryListWidget.Entry<PaintingEntry> {
    private final PaintingData paintingData;
    private final Sprite sprite;
    private final boolean canStay;

    private static Identifier clickedId;
    private static long time;

    public PaintingEntry(PaintingData paintingData) {
      this.paintingData = paintingData;
      this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(paintingData);
      this.canStay = PaintingListWidget.this.state.canStay(paintingData);
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
        float partialTicks) {
      int maxHeight = entryHeight - 4;
      int maxWidth = maxHeight;

      int scaledWidth = paintingData.getScaledWidth(maxWidth, maxHeight);
      int scaledHeight = paintingData.getScaledHeight(maxWidth, maxHeight);

      RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);

      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
      RenderSystem.setShaderTexture(0, this.sprite.getAtlasId());
      drawContext.drawSprite(x + 4 + (maxWidth - scaledWidth) / 2,
          y + (entryHeight - scaledHeight) / 2,
          1,
          scaledWidth,
          scaledHeight,
          sprite);

      TextRenderer textRenderer = PaintingListWidget.this.client.textRenderer;
      int textWidth = entryWidth - 4 - maxWidth - 4 - 8;
      int posX = x + maxWidth + 4 + 4;
      int posY = y + MathHelper.ceil((entryHeight - 3 * textRenderer.fontHeight - 2 * 2) / 2f);

      if (paintingData.hasLabel()) {
        StringVisitable label = paintingData.getLabel();
        if (textRenderer.getWidth(label) > textWidth) {
          Text ellipsis = Text.literal("...");
          label = StringVisitable.concat(textRenderer.trimToWidth(label,
              textWidth - textRenderer.getWidth(ellipsis)), ellipsis);
        }

        drawContext.drawText(textRenderer,
            Language.getInstance().reorder(label),
            posX,
            posY,
            0xFFFFFFFF,
            false);

        posY += textRenderer.fontHeight + 2;
      }

      StringVisitable id = Text.literal("(" + paintingData.id().toString() + ")")
          .setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      if (textRenderer.getWidth(id) > textWidth) {
        Text ellipsis =
            Text.literal("...").setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
        id = StringVisitable.concat(textRenderer.trimToWidth(id,
            textWidth - textRenderer.getWidth(ellipsis)), ellipsis);
      }

      drawContext.drawText(textRenderer,
          Language.getInstance().reorder(id),
          posX,
          posY,
          0xFFFFFFFF,
          false);

      posY += textRenderer.fontHeight + 2;

      drawContext.drawText(textRenderer,
          Text.translatable("custompaintings.painting.dimensions",
              paintingData.width(),
              paintingData.height()),
          posX,
          posY,
          0xFFFFFFFF,
          false);
    }

    @Override
    public Text getNarration() {
      return !this.paintingData.hasLabel()
          ? Text.literal(this.paintingData.id().toString())
          : this.paintingData.getLabel();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      PaintingListWidget.this.setSelected(this);

      if (this.canStay) {
        if (this.paintingData.id().equals(clickedId) && Util.getMeasuringTimeMs() - time < 250L) {
          PaintingListWidget.this.parent.playClickSound();
          PaintingListWidget.this.parent.saveSelection(this.paintingData);
          return true;
        }

        clickedId = this.paintingData.id();
        time = Util.getMeasuringTimeMs();
      }

      return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (PaintingListWidget.this.getSelectedOrNull() != this) {
        return false;
      }

      switch (keyCode) {
        case GLFW.GLFW_KEY_ENTER:
          if (this.canStay) {
            PaintingListWidget.this.parent.saveSelection(this.paintingData);
            return true;
          }
          break;
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class EmptyPaintingEntry extends PaintingEntry {
    private Text text = Text.translatable("custompaintings.painting.empty");

    public EmptyPaintingEntry() {
      super(PaintingData.EMPTY);
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
        float partialTicks) {
      TextRenderer textRenderer = PaintingListWidget.this.client.textRenderer;
      drawContext.drawCenteredTextWithShadow(textRenderer,
          text,
          x + entryWidth / 2,
          y + (entryHeight - textRenderer.fontHeight) / 2,
          0xFFFFFFFF);
    }

    @Override
    public Text getNarration() {
      return text;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return false;
    }
  }
}
