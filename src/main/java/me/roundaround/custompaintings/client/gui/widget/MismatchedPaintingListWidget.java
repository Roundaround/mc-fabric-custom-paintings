package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.Coords;
import me.roundaround.roundalib.client.gui.widget.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

@Environment(value = EnvType.CLIENT)
public class MismatchedPaintingListWidget extends FlowListWidget<MismatchedPaintingListWidget.Entry> {
  private final MismatchedPaintingsScreen parent;

  public MismatchedPaintingListWidget(
      MismatchedPaintingsScreen parent, MinecraftClient client, ThreePartsLayoutWidget layout
  ) {
    super(client, layout.getX(), layout.getHeaderHeight(), layout.getWidth(), layout.getContentHeight());

    this.parent = parent;

    this.loadData();
  }

  public void loadData() {
    ClientNetworking.sendRequestMismatchedPacket();
    this.clearEntries();
    this.addEntry((index, left, top, width) -> {
      return new LoadingEntry(this.client.textRenderer, index, left, top, width);
    });
  }

  public void receiveData(HashSet<MismatchedPainting> data) {
    this.clearEntries();
    for (MismatchedPainting mismatchedPainting : data) {
      this.addEntry((index, left, top, width) -> {
        return new MismatchedPaintingEntry(this.client.textRenderer, mismatchedPainting, index, left, top, width);
      });
    }
    if (data.isEmpty()) {
      this.addEntry((index, left, top, width) -> {
        return new EmptyEntry(this.client.textRenderer, index, left, top, width);
      });
    }
    this.narrateScreenIfNarrationEnabled();
  }

  private void narrateScreenIfNarrationEnabled() {
    this.parent.narrateScreenIfNarrationEnabled(true);
  }

  @Environment(value = EnvType.CLIENT)
  public abstract static class Entry extends FlowListWidget.Entry {
    protected static final int HEIGHT = 36;

    protected Entry(int index, int left, int top, int width, int contentHeight) {
      super(index, left, top, width, contentHeight);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class LoadingEntry extends Entry {
    private final LabelWidget label;

    public LoadingEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.label = this.addDrawableAndSelectableChild(
          LabelWidget.builder(textRenderer, Text.translatable("custompaintings.mismatched.loading"))
              .refPosition(this.getContentCenterX(), this.getContentCenterY())
              .dimensions(this.getContentWidth(), this.getContentHeight())
              .build());
      this.addDrawable(new DrawableWidget() {
        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
          context.drawCenteredTextWithShadow(textRenderer, LoadingDisplay.get(Util.getMeasuringTimeMs()),
              LoadingEntry.this.getContentCenterX(), LoadingEntry.this.label.getTextBounds().bottom(), Colors.GRAY
          );
        }
      });
    }

    @Override
    public void refreshPositions() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class EmptyEntry extends Entry {
    private final LabelWidget label;

    public EmptyEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.label = this.addDrawableAndSelectableChild(
          LabelWidget.builder(textRenderer, Text.translatable("custompaintings.mismatched.empty"))
              .refPosition(this.getContentCenterX(), this.getContentCenterY())
              .dimensions(this.getContentWidth(), this.getContentHeight())
              .build());
    }

    @Override
    public void refreshPositions() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class MismatchedPaintingEntry extends Entry {
    private final MismatchedPainting mismatchedPainting;
    private final Sprite sprite;

    public MismatchedPaintingEntry(
        TextRenderer textRenderer, MismatchedPainting mismatchedPainting, int index, int left, int top, int width
    ) {
      super(index, left, top, width, HEIGHT);

      this.mismatchedPainting = mismatchedPainting;
      this.sprite = CustomPaintingsClientMod.customPaintingManager.getPaintingSprite(mismatchedPainting.currentData());

      PaintingData paintingData = mismatchedPainting.currentData();

      LinearLayoutWidget layout = LinearLayoutWidget.horizontal(
          (self) -> Coords.of(this.getContentWidth(), this.getContentHeight())).spacing(GuiUtil.PADDING);
      layout.getMainPositioner().alignVerticalCenter();

      layout.add(new DrawableWidget() {
        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
          Sprite sprite = MismatchedPaintingEntry.this.sprite;
          int maxSize = MismatchedPaintingEntry.this.getContentHeight();
          int left = MismatchedPaintingEntry.this.getContentLeft();
          int top = MismatchedPaintingEntry.this.getContentTop();

          int scaledWidth = paintingData.getScaledWidth(maxSize, maxSize);
          int scaledHeight = paintingData.getScaledHeight(maxSize, maxSize);

          RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
          RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
          RenderSystem.setShaderTexture(0, sprite.getAtlasId());
          context.drawSprite(left + (maxSize - scaledWidth) / 2, top + (maxSize - scaledHeight) / 2, 1, scaledWidth,
              scaledHeight, sprite
          );
        }
      }, (parent, self) -> {
        self.setDimensions(this.getContentHeight(), this.getContentHeight());
      });

      LinearLayoutWidget infoColumn = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING / 2).centered();
      infoColumn.getMainPositioner().alignLeft();
      layout.add(infoColumn, (parent, self) -> {
        int height = this.getContentHeight();
        self.setDimensions(this.getContentWidth() - 2 * GuiUtil.PADDING - height, height);
      });

      if (paintingData.hasLabel()) {
        LabelWidget labelLabel = LabelWidget.builder(textRenderer, paintingData.getLabel())
            .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
            .build();
        infoColumn.add(labelLabel);
      }

      MutableText idText = Text.literal("(" + paintingData.id() + ")");
      if (paintingData.hasLabel()) {
        idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      }
      LabelWidget idLabel = LabelWidget.builder(textRenderer, idText)
          .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
          .build();
      infoColumn.add(idLabel);

      ArrayList<Text> outdated = new ArrayList<>();
      PaintingData knownData = this.mismatchedPainting.knownData();
      if (paintingData.isMismatched(knownData, MismatchedCategory.SIZE)) {
        outdated.add(Text.translatable("custompaintings.mismatched.outdated.size"));
      }
      if (paintingData.isMismatched(knownData, MismatchedCategory.INFO)) {
        outdated.add(Text.translatable("custompaintings.mismatched.outdated.info"));
      }

      String outdatedString = outdated.stream().map(Text::getString).collect(Collectors.joining(", "));
      LabelWidget outdatedLabel = LabelWidget.builder(
              textRenderer, Text.translatable("custompaintings.mismatched.outdated", outdatedString))
          .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
          .build();
      infoColumn.add(outdatedLabel);

      layout.add(IconButtonWidget.builder(IconButtonWidget.BuiltinIcon.FIX_18, CustomPaintingsMod.MOD_ID)
          .vanillaSize()
          .messageAndTooltip(Text.translatable("custompaintings.mismatched.fix"))
          .onPress((button) -> ClientNetworking.sendUpdatePaintingPacket(this.mismatchedPainting.uuid()))
          .build());

      layout.forEachChild(this::addDetectedCapabilityChild);
    }
  }
}
