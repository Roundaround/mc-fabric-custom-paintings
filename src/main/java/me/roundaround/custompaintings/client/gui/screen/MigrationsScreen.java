package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.resource.legacy.LegacyPackConverter;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

public class MigrationsScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  private MigrationList list;

  protected MigrationsScreen(Screen parent) {
    super(Text.translatable("custompaintings.migrate.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new MigrationList(this.client, this.layout));
    this.list.setMigrations(ClientPaintingRegistry.getInstance().getMigrations().values());

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).width(BUTTON_WIDTH).build());

    FabricLoader.getInstance().getModContainer(CustomPaintingsMod.MOD_ID).ifPresent((mod) -> {
      Text version = Text.of("v" + mod.getMetadata().getVersion().getFriendlyString());
      this.layout.addNonPositioned(LabelWidget.builder(this.textRenderer, version)
          .hideBackground()
          .showShadow()
          .alignSelfRight()
          .alignSelfBottom()
          .alignTextRight()
          .build(), (parent, self) -> self.setPosition(this.width - GuiUtil.PADDING, this.height - GuiUtil.PADDING));
    });

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private static class MigrationList extends ParentElementEntryListWidget<MigrationList.Entry> {
    public MigrationList(MinecraftClient client, ThreeSectionLayoutWidget layout) {
      super(client, layout);
    }

    public void setMigrations(Collection<MigrationData> migrations) {
      this.clearEntries();
      
      if (migrations.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.textRenderer));
        return;
      }

      for (MigrationData migration : migrations) {

      }

      this.refreshPositions();
    }

    private static abstract class Entry extends ParentElementEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }

    private static class EmptyEntry extends Entry {
      private static final int HEIGHT = 36;
      private static final Text MESSAGE = Text.translatable("custompaintings.migrate.none");

      private final LabelWidget label;

      protected EmptyEntry(int index, int left, int top, int width, TextRenderer textRenderer) {
        super(index, left, top, width, HEIGHT);

        this.label = LabelWidget.builder(textRenderer, MESSAGE)
            .position(this.getContentCenterX(), this.getContentCenterY())
            .dimensions(this.getContentWidth(), this.getContentHeight())
            .alignSelfCenterX()
            .alignSelfCenterY()
            .alignTextCenterX()
            .alignTextCenterY()
            .hideBackground()
            .showShadow()
            .build();

        this.addDrawable(this.label);
      }

      public static FlowListWidget.EntryFactory<EmptyEntry> factory(TextRenderer textRenderer) {
        return (index, left, top, width) -> new EmptyEntry(index, left, top, width, textRenderer);
      }

      @Override
      public void refreshPositions() {
        this.label.batchUpdates(() -> {
          this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
          this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
        });
      }
    }

    private static class MigrationEntry extends Entry {
      private static final int HEIGHT = 48;
      private static final int PACK_ICON_SIZE = 36;

      private final MigrationData migration;

      protected MigrationEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          MigrationData migration,
          Consumer<MigrationData> onSelect
      ) {
        super(index, left, top, width, HEIGHT);
        this.migration = migration;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        layout.add(
            SpriteWidget.create(LegacyPackConverter.getInstance().getSprite(this.migration.id().getNamespace())),
            (parent, self) -> {
              self.setDimensions(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());
      }
    }
  }
}
