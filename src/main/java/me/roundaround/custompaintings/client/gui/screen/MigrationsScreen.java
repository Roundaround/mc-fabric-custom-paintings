package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MigrationsScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;
  private static final Text LABEL_RUN = Text.translatable("custompaintings.migrate.entry.run");
  private static final Text LABEL_RE_RUN = Text.translatable("custompaintings.migrate.entry.reRun");
  private static final Text TOOLTIP_SUCCESS = Text.translatable("custompaintings.migrate.entry.success");
  private static final Text TOOLTIP_FAILURE = Text.translatable("custompaintings.migrate.entry.error");

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

    this.list = this.layout.addBody(
        new MigrationList(this.client, this.layout, ClientPaintingRegistry.getInstance().getMigrations().values(),
            ClientPaintingRegistry.getInstance().getFinishedMigrations(), this::runMigration
        ));

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

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

  public void onMigrationFinished(CustomId id, boolean succeeded) {
    this.list.markMigrationFinished(id, succeeded);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private void runMigration(MigrationList.MigrationEntry entry) {
    Util.getIoWorkerExecutor().execute(() -> ClientNetworking.sendRunMigrationPacket(entry.getMigrationId()));
    entry.markLoading();
  }

  private static class MigrationList extends ParentElementEntryListWidget<MigrationList.Entry> {
    public MigrationList(
        MinecraftClient client,
        ThreeSectionLayoutWidget layout,
        Collection<MigrationData> migrations,
        Map<CustomId, Boolean> finishedMigrations,
        Consumer<MigrationEntry> runMigration
    ) {
      super(client, layout);

      if (migrations.isEmpty()) {
        this.addEntry(EmptyEntry.factory(this.client.textRenderer));
        return;
      }

      for (MigrationData migration : migrations) {
        this.addEntry(MigrationEntry.factory(this.client.textRenderer, migration,
            Status.of(finishedMigrations.get(migration.id())), runMigration
        ));
      }

      this.refreshPositions();
    }

    public void markMigrationFinished(CustomId id, boolean succeeded) {
      for (Entry entry : this.entries) {
        if ((entry instanceof MigrationEntry migrationEntry) && migrationEntry.getMigrationId().equals(id)) {
          migrationEntry.markFinished(succeeded);
        }
      }
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
      private static final int RUN_BUTTON_SIZE = 80;
      private static final int STATUS_BUTTON_SIZE = 20;
      private static final Text LINE_SOURCE = Text.translatable("custompaintings.migrate.entry.source");
      private static final Text LINE_ID = Text.translatable("custompaintings.migrate.entry.id");
      private static final Text LINE_DESCRIPTION = Text.translatable("custompaintings.migrate.entry.desc");
      private static final Text LINE_PAIRS = Text.translatable("custompaintings.migrate.entry.pairs");
      private static final Text NONE_PLACEHOLDER = Text.translatable("custompaintings.migrate.entry.emptyField")
          .formatted(Formatting.ITALIC, Formatting.GRAY);

      private final MigrationData migration;
      private final LoadingButtonWidget runButton;
      private final IconButtonWidget statusButton;

      protected MigrationEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          MigrationData migration,
          Status initialStatus,
          Consumer<MigrationEntry> runMigration
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

        layout.add(SpriteWidget.create(
                ClientPaintingRegistry.getInstance().getSprite(PackIcons.customId(migration.id().pack()))),
            (parent, self) -> {
              self.setDimensions(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING);
        int headerWidth = Stream.of(LINE_SOURCE, LINE_ID, LINE_DESCRIPTION, LINE_PAIRS)
            .mapToInt(textRenderer::getWidth)
            .max()
            .orElse(1);
        PackData sourcePack = ClientPaintingRegistry.getInstance().getPacks().get(migration.id().pack());
        textSection.add(
            this.textLine(textRenderer, headerWidth, LINE_SOURCE, sourcePack == null ? null : sourcePack.name()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_ID, migration.id().resource()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_DESCRIPTION, migration.description()),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(this.textLine(textRenderer, headerWidth, LINE_PAIRS, String.valueOf(migration.pairs().size())),
            (parent, self) -> self.setWidth(parent.getWidth())
        );
        layout.add(textSection, (parent, self) -> {
          int textSectionWidth = this.getContentWidth();
          textSectionWidth -= (parent.getChildren().size() - 1) * parent.getSpacing();
          for (Widget widget : parent.getChildren()) {
            if (widget != self) {
              textSectionWidth -= widget.getWidth();
            }
          }
          self.setWidth(textSectionWidth);
        });

        layout.add(FillerWidget.empty());

        this.runButton = layout.add(
            new LoadingButtonWidget(0, 0, RUN_BUTTON_SIZE, ButtonWidget.DEFAULT_HEIGHT, initialStatus.getButtonLabel(),
                (button) -> runMigration.accept(this)
            ));

        this.statusButton = layout.add(IconButtonWidget.builder(initialStatus.getTexture(), IconButtonWidget.SIZE_L)
            .dimensions(STATUS_BUTTON_SIZE)
            .hideBackground()
            .disableIconDim()
            .tooltip(initialStatus.getTooltip())
            .build());
        this.statusButton.visible = initialStatus != Status.NONE;
        this.statusButton.active = false;

        layout.forEachChild(this::addDrawableChild);
      }

      private LinearLayoutWidget textLine(TextRenderer textRenderer, int headerWidth, Text header, String value) {
        LinearLayoutWidget line = LinearLayoutWidget.horizontal().spacing(2);
        Text valueText = value == null || value.isBlank() ? NONE_PLACEHOLDER : Text.of(value);

        line.add(
            LabelWidget.builder(textRenderer, header).hideBackground().showShadow().color(Colors.LIGHT_GRAY).build(),
            (parent, self) -> {
              self.setWidth(headerWidth);
            }
        );
        line.add(LabelWidget.builder(textRenderer, valueText)
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
          self.setWidth(parent.getWidth() - parent.getSpacing() - headerWidth);
        });

        return line;
      }

      public CustomId getMigrationId() {
        return this.migration.id();
      }

      public void markLoading() {
        this.runButton.setLoading(true);
      }

      public void markFinished(boolean succeeded) {
        this.runButton.setLoading(false);

        Status status = Status.of(succeeded);
        this.statusButton.visible = status != Status.NONE;
        this.statusButton.setTexture(status.getTexture());
        this.statusButton.setMessage(status.getButtonLabel());
        this.statusButton.setTooltip(Tooltip.of(status.getTooltip()));
      }

      public static FlowListWidget.EntryFactory<MigrationEntry> factory(
          TextRenderer textRenderer,
          MigrationData migration,
          Status initialStatus,
          Consumer<MigrationEntry> runMigration
      ) {
        return (index, left, top, width) -> new MigrationEntry(
            index, left, top, width, textRenderer, migration, initialStatus, runMigration);
      }
    }
  }

  private enum Status {
    NONE(LABEL_RUN, Text.empty(), null),
    SUCCESS(LABEL_RE_RUN, TOOLTIP_SUCCESS, new Identifier("pending_invite/accept")),
    FAILURE(LABEL_RUN, TOOLTIP_FAILURE, new Identifier("pending_invite/reject"));

    private final Text buttonLabel;
    private final Text tooltip;
    private final Identifier texture;

    Status(Text buttonLabel, Text tooltip, Identifier texture) {
      this.buttonLabel = buttonLabel;
      this.tooltip = tooltip;
      this.texture = texture;
    }

    public static Status of(Boolean succeeded) {
      if (succeeded == null) {
        return NONE;
      }
      return succeeded ? SUCCESS : FAILURE;
    }

    public Text getButtonLabel() {
      return this.buttonLabel;
    }

    // TODO: Tooltip based on actual error?
    public Text getTooltip() {
      return this.tooltip;
    }

    public Identifier getTexture() {
      return this.texture;
    }
  }
}
