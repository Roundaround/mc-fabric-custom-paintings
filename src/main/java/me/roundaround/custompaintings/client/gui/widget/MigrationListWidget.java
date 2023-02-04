package me.roundaround.custompaintings.client.gui.widget;

import java.util.HashMap;

import me.roundaround.custompaintings.client.gui.screen.manage.MigrationsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.PaintingPacksTracker.MigrationGroup;
import me.roundaround.custompaintings.util.Migration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@Environment(value = EnvType.CLIENT)
public class MigrationListWidget
    extends AlwaysSelectedEntryListWidget<MigrationListWidget.Entry> {
  private static final int ITEM_HEIGHT = 25;

  private final MigrationsScreen parent;
  private final EmptyEntry emptyEntry;

  public MigrationListWidget(
      MigrationsScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.parent = parent;
    this.emptyEntry = new EmptyEntry(minecraftClient);

    addEntry(this.emptyEntry);
  }

  public void setMigrations(HashMap<String, MigrationGroup> migrations) {
    clearEntries();
    for (MigrationGroup group : migrations.values()) {
      addEntry(new PackTitleEntry(this.client, group.packId(), group.packName()));
      for (Migration migration : group.migrations()) {
        addEntry(new MigrationEntry(this.client, migration));
      }
    }

    if (this.getEntryCount() == 0) {
      addEntry(this.emptyEntry);
    }
  }

  @Override
  public void setSelected(Entry entry) {
    super.setSelected(entry);

    if (entry instanceof MigrationEntry) {
      this.parent.setSelectedMigration(((MigrationEntry) entry).getMigration());
    }
  }

  @Environment(value = EnvType.CLIENT)
  public abstract class Entry
      extends AlwaysSelectedEntryListWidget.Entry<Entry> {
  }

  @Environment(value = EnvType.CLIENT)
  public class EmptyEntry extends Entry {
    private static final Text EMPTY_LIST_TEXT = Text.translatable("custompaintings.migrations.empty");

    private final MinecraftClient client;

    public EmptyEntry(MinecraftClient client) {
      this.client = client;
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
          EMPTY_LIST_TEXT,
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return EMPTY_LIST_TEXT;
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class PackTitleEntry extends Entry {
    private final MinecraftClient client;
    private final String packId;
    private final String packName;

    public PackTitleEntry(MinecraftClient client, String packId, String packName) {
      this.client = client;
      this.packId = packId;
      this.packName = packName;
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
      drawCenteredTextWithShadow(
          matrixStack,
          this.client.textRenderer,
          Text.literal(this.packId).asOrderedText(),
          this.client.currentScreen.width / 2,
          y + MathHelper.ceil((entryHeight - this.client.textRenderer.fontHeight) / 2f),
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.packName);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public class MigrationEntry extends Entry {
    private final MinecraftClient client;
    private final Migration migration;

    private long time;

    public MigrationEntry(MinecraftClient client, Migration migration) {
      this.client = client;
      this.migration = migration;
    }

    public Migration getMigration() {
      return this.migration;
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
      drawCenteredTextWithShadow(
          matrixStack,
          this.client.textRenderer,
          Text.literal(this.migration.id().toString()).asOrderedText(),
          this.client.currentScreen.width / 2,
          y + 1,
          0xFFFFFF);
      drawCenteredTextWithShadow(
          matrixStack,
          this.client.textRenderer,
          Text.literal(this.migration.description()).asOrderedText(),
          this.client.currentScreen.width / 2,
          y + entryHeight - 1 - this.client.textRenderer.fontHeight,
          0xFFFFFF);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.migration.description());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      MigrationListWidget.this.setSelected(this);

      if (Util.getMeasuringTimeMs() - this.time < 250L) {
        MigrationListWidget.this.parent.confirmSelection();
        return true;
      }

      this.time = Util.getMeasuringTimeMs();
      return false;
    }
  }
}
