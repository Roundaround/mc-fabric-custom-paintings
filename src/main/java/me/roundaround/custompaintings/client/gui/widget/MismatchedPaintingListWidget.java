package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData.MismatchedCategory;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
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
public class MismatchedPaintingListWidget extends ParentElementEntryListWidget<MismatchedPaintingListWidget.Entry> {
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
  public abstract static class Entry extends ParentElementEntryListWidget.Entry {
    protected static final int HEIGHT = 36;

    protected Entry(int index, int left, int top, int width, int contentHeight) {
      super(index, left, top, width, contentHeight);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class LoadingEntry extends Entry {
    private final LabelWidget label;
    private final DrawableWidget loader;

    public LoadingEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.label = this.addDrawableChild(
          LabelWidget.builder(textRenderer, Text.translatable("custompaintings.mismatched.loading"))
              .refPosition(this.getContentCenterX(), this.getContentCenterY())
              .dimensions(this.getContentWidth(), this.getContentHeight())
              .build());
      this.loader = this.addDrawable(new DrawableWidget() {
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
      super.refreshPositions();

      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });

      this.loader.setPosition(this.getContentCenterX(), this.getContentCenterY());
      this.loader.setDimensions(this.getContentWidth(), this.getContentHeight());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class EmptyEntry extends Entry {
    private final LabelWidget label;

    public EmptyEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, HEIGHT);

      this.label = this.addDrawableChild(
          LabelWidget.builder(textRenderer, Text.translatable("custompaintings.mismatched.empty"))
              .refPosition(this.getContentCenterX(), this.getContentCenterY())
              .dimensions(this.getContentWidth(), this.getContentHeight())
              .build());
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();

      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class MismatchedPaintingEntry extends Entry {
    private final MismatchedPainting mismatchedPainting;

    public MismatchedPaintingEntry(
        TextRenderer textRenderer, MismatchedPainting mismatchedPainting, int index, int left, int top, int width
    ) {
      super(index, left, top, width, HEIGHT);

      this.mismatchedPainting = mismatchedPainting;

      PaintingData paintingData = mismatchedPainting.currentData();

      LinearLayoutWidget layout = this.addLayout(LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING), (self) -> {
        self.setPosition(this.getContentLeft(), this.getContentTop());
        self.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
      layout.getMainPositioner().alignVerticalCenter();

      layout.add(new PaintingSpriteWidget(paintingData), (parent, self) -> {
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

      layout.forEachChild(this::addDrawableChild);
    }
  }
}
