package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.screen.manage.PaintingPacksTracker.MigrationGroup;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.Coords;
import me.roundaround.roundalib.client.gui.widget.AlwaysSelectedFlowListWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import me.roundaround.roundalib.client.gui.widget.LinearLayoutWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class MigrationListWidget extends AlwaysSelectedFlowListWidget<MigrationListWidget.Entry> {
  private final Consumer<Migration> onSelect;
  private final Consumer<Migration> onConfirm;

  private Migration clickedMigration = null;
  private long clickedTime = 0L;

  public MigrationListWidget(
      MinecraftClient client, ThreePartsLayoutWidget layout, Consumer<Migration> onSelect, Consumer<Migration> onConfirm
  ) {
    super(client, layout.getX(), layout.getHeaderHeight(), layout.getWidth(), layout.getContentHeight());

    this.onSelect = onSelect;
    this.onConfirm = onConfirm;

    this.addEmptyEntry();
  }

  public void setMigrations(HashMap<String, MigrationGroup> migrations) {
    this.clearEntries();
    for (MigrationGroup group : migrations.values()) {
      this.addEntry((index, left, top, width) -> {
        return new PackTitleEntry(this.client.textRenderer, group.packId(), group.packName(), index, left, top, width);
      });
      for (Migration migration : group.migrations()) {
        this.addEntry((index, left, top, width) -> {
          return new MigrationEntry(this.client.textRenderer, migration, index, left, top, width);
        });
      }
    }

    if (this.getEntryCount() == 0) {
      this.addEmptyEntry();
    }
  }

  private void addEmptyEntry() {
    this.addEntry((index, left, top, width) -> {
      return new EmptyEntry(this.client.textRenderer, index, left, top, width);
    });
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);
    this.onSelect.accept(entry.getMigration());
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    Entry entry = this.getEntryAtPosition(mouseX, mouseY);
    if (entry != null && entry.getMigration() != null) {
      Migration migration = entry.getMigration();
      if (migration == this.clickedMigration && Util.getMeasuringTimeMs() - this.clickedTime < 250L) {
        this.onConfirm.accept(migration);
        return true;
      }

      this.clickedMigration = migration;
      this.clickedTime = Util.getMeasuringTimeMs();
    }

    super.mouseClicked(mouseX, mouseY, button);
    return entry != null;
  }

  @Override
  protected Entry getNeighboringEntry(NavigationDirection direction) {
    return this.getNeighboringEntry(direction, (entry) -> entry.getMigration() != null);
  }

  @Override
  protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, Entry entry) {
    if (!this.isEntryVisible(entry)) {
      return;
    }

    if (entry.renderRowShade()) {
      this.renderRowShade(context, entry);
    }

    entry.render(context, mouseX, mouseY, delta);
  }

  @Environment(value = EnvType.CLIENT)
  public abstract static class Entry extends AlwaysSelectedFlowListWidget.Entry {
    protected Entry(int index, int left, int top, int width, int contentHeight) {
      super(index, left, top, width, contentHeight);
    }

    public Migration getMigration() {
      return null;
    }

    public boolean renderRowShade() {
      return false;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class EmptyEntry extends Entry {
    private final LabelWidget label;

    public EmptyEntry(TextRenderer textRenderer, int index, int left, int top, int width) {
      super(index, left, top, width, 36);

      this.label = this.addDrawableAndSelectableChild(
          LabelWidget.builder(textRenderer, Text.translatable("custompaintings.migrations.empty"))
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

    @Override
    public Text getNarration() {
      return this.label.getText();
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class PackTitleEntry extends Entry {
    private final LinearLayoutWidget layout;
    private final LabelWidget nameLabel;
    private final LabelWidget idLabel;

    public PackTitleEntry(
        TextRenderer textRenderer, String packId, String packName, int index, int left, int top, int width
    ) {
      super(index, left, top, width, 30);

      this.layout = LinearLayoutWidget.vertical((self) -> Coords.of(this.getContentWidth(), this.getContentHeight()))
          .spacing(GuiUtil.PADDING / 2);
      this.layout.getMainPositioner().alignHorizontalCenter();

      boolean hasName = packName != null && !packName.isEmpty();

      if (hasName) {
        this.nameLabel = this.layout.add(LabelWidget.builder(textRenderer, Text.literal(packName))
            .overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE)
            .build(), (parent, self) -> {
          self.setDimensions(this.getContentWidth(), LabelWidget.getDefaultSingleLineHeight(textRenderer));
        });
      } else {
        this.nameLabel = null;
      }

      MutableText idText = Text.literal(packId);
      if (hasName) {
        idText = idText.setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY));
      }
      this.idLabel = this.layout.add(
          LabelWidget.builder(textRenderer, idText).overflowBehavior(LabelWidget.OverflowBehavior.TRUNCATE).build(),
          (parent, self) -> {
            self.setDimensions(this.getContentWidth(), LabelWidget.getDefaultSingleLineHeight(textRenderer));
          }
      );

      this.layout.forEachChild(this::addDrawableChild);
    }

    @Override
    public boolean renderRowShade() {
      return true;
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();
      this.layout.refreshPositions();
    }

    @Override
    public Text getNarration() {
      LabelWidget sourceLabel = this.nameLabel != null ? this.nameLabel : this.idLabel;
      return sourceLabel.getText();
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class MigrationEntry extends Entry {
    private final Migration migration;
    private final LinearLayoutWidget layout;

    public MigrationEntry(
        TextRenderer textRenderer, Migration migration, int index, int left, int top, int width
    ) {
      super(index, left, top, width, 36);

      this.migration = migration;

      this.layout = LinearLayoutWidget.vertical((self) -> Coords.of(this.getContentWidth(), this.getContentHeight()))
          .spacing(GuiUtil.PADDING / 2)
          .centered();
      this.layout.getMainPositioner().alignHorizontalCenter();

      LabelWidget nameLabel = LabelWidget.builder(textRenderer, Text.literal(this.migration.description()))
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .build();
      this.layout.add(nameLabel, (parent, self) -> {
        self.setDimensions(this.getContentWidth(), LabelWidget.getDefaultSingleLineHeight(textRenderer));
      });

      LabelWidget dateLabel = LabelWidget.builder(
          textRenderer,
          Text.literal(this.migration.id()).setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.GRAY))
      ).overflowBehavior(LabelWidget.OverflowBehavior.SCROLL).build();
      this.layout.add(dateLabel, (parent, self) -> {
        self.setDimensions(this.getContentWidth(), LabelWidget.getDefaultSingleLineHeight(textRenderer));
      });

      LabelWidget changesLabel = LabelWidget.builder(
          textRenderer,
          Text.translatable("custompaintings.migrations.count", String.valueOf(this.migration.pairs().size()))
      ).overflowBehavior(LabelWidget.OverflowBehavior.SCROLL).build();
      this.layout.add(changesLabel, (parent, self) -> {
        self.setDimensions(this.getContentWidth(), LabelWidget.getDefaultSingleLineHeight(textRenderer));
      });

      this.layout.forEachChild(this::addDrawableChild);
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();
      this.layout.refreshPositions();
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.migration.description());
    }

    @Override
    public Migration getMigration() {
      return this.migration;
    }
  }
}
