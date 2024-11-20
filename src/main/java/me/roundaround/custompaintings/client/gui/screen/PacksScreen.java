package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.toast.CustomSystemToasts;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.util.StringUtil;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.util.Axis;
import me.roundaround.roundalib.client.gui.util.Spacing;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.roundalib.util.PathAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PacksScreen extends Screen implements PacksLoadedListener {
  private static final Text TITLE_MANAGE = Text.translatable("custompaintings.packs.manage");
  private static final Text TITLE_VIEW = Text.translatable("custompaintings.packs.view");
  private static final Text LIST_INACTIVE = Text.translatable("custompaintings.packs.inactive");
  private static final Text LIST_ACTIVE = Text.translatable("custompaintings.packs.active");
  private static final int BUTTON_HEIGHT = ButtonWidget.DEFAULT_HEIGHT;
  private static final int BUTTON_WIDTH = ButtonWidget.DEFAULT_WIDTH_SMALL;
  private static final int LIST_WIDTH = 200;
  private static final Identifier SELECT_TEXTURE = new Identifier("transferable_list/select");
  private static final Identifier SELECT_HIGHLIGHTED_TEXTURE = new Identifier("transferable_list/select_highlighted");
  private static final Identifier UNSELECT_TEXTURE = new Identifier("transferable_list/unselect");
  private static final Identifier UNSELECT_HIGHLIGHTED_TEXTURE = new Identifier(
      "transferable_list/unselect_highlighted");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;
  private final boolean editMode;
  private final ArrayList<PackData> inactivePacks = new ArrayList<>();
  private final ArrayList<PackData> activePacks = new ArrayList<>();
  private final HashSet<String> toActivate = new HashSet<>();
  private final HashSet<String> toDeactivate = new HashSet<>();

  private PackList inactiveList;
  private PackList activeList;
  private LoadingButtonWidget reloadButton;

  public PacksScreen(Screen parent, boolean editMode) {
    super(editMode ? TITLE_MANAGE : TITLE_VIEW);
    this.parent = parent;
    this.editMode = editMode;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.resetPacks();

    boolean inSinglePlayer = this.client.isInSingleplayer();

    this.layout.addHeader(this.textRenderer, this.title);
    if (inSinglePlayer) {
      this.layout.addHeader(this.textRenderer,
          Text.translatable("custompaintings.packs.drag").formatted(Formatting.GRAY)
      );
    }

    if (this.editMode) {
      this.layout.getBody().flowAxis(Axis.HORIZONTAL).spacing(30);
      this.inactiveList = this.layout.addBody(
          new PackList(this.client, LIST_WIDTH, this.layout.getBodyHeight(), LIST_INACTIVE, SELECT_TEXTURE,
              SELECT_HIGHLIGHTED_TEXTURE, this::activatePack, this.inactivePacks
          ), (parent, self) -> {
            self.setDimensions(LIST_WIDTH, parent.getHeight());
          });
      this.activeList = this.layout.addBody(
          new PackList(this.client, LIST_WIDTH, this.layout.getBodyHeight(), LIST_ACTIVE, UNSELECT_TEXTURE,
              UNSELECT_HIGHLIGHTED_TEXTURE, this::deactivatePack, this.activePacks
          ), (parent, self) -> {
            self.setDimensions(LIST_WIDTH, parent.getHeight());
          });

      this.reloadButton = this.layout.addFooter(
          new LoadingButtonWidget(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Text.of("Reload Packs"),
              (b) -> this.reloadPacks()
          ));

      ButtonWidget openDirButton = this.layout.addFooter(
          ButtonWidget.builder(Text.translatable("custompaintings.packs.open"), (b) -> this.openPackDir())
              .width(BUTTON_WIDTH)
              .build());
      if (!inSinglePlayer) {
        openDirButton.active = false;
        openDirButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.packs.open.notInWorld")));
      }
    } else {
      this.activeList = this.layout.addBody(new PackList(this.client, this.layout, LIST_ACTIVE, this.activePacks));
    }

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (b) -> this.close()).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void filesDragged(List<Path> paths) {
    assert this.client != null;

    if (!this.client.isInSingleplayer()) {
      return;
    }

    Path packsDirectory = PathAccessor.getInstance().getPerWorldModDir(CustomPaintingsMod.MOD_ID);
    List<Path> packPaths = paths.stream().filter(ResourceUtil::isPaintingPack).toList();

    if (packPaths.isEmpty()) {
      return;
    }

    String packList = packPaths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));

    this.client.setScreen(new ConfirmScreen((confirmed) -> {
      if (confirmed) {
        boolean allSuccessful = true;

        for (Path src : packPaths) {
          Path dest = packsDirectory.resolve(src.getFileName());
          // TODO: Validate packs before copy
          try {
            Files.copy(src, dest);
          } catch (IOException e) {
            CustomPaintingsMod.LOGGER.warn("Failed to copy painting pack from {} to {}", src, dest);
            allSuccessful = false;
            break;
          }
        }

        if (!allSuccessful) {
          CustomSystemToasts.addPackCopyFailure(this.client, packsDirectory.toString());
        }

        this.reloadPacks();
      }

      this.client.setScreen(this);
    }, Text.translatable("custompaintings.packs.copyConfirm"), Text.of(packList)));
  }

  @Override
  public void close() {
    assert this.client != null;

    if (!this.toActivate.isEmpty() || !this.toDeactivate.isEmpty()) {
      this.reloadPacks();
    }
    this.client.setScreen(this.parent);
  }

  @Override
  public void onPacksLoaded() {
    if (this.reloadButton != null) {
      this.reloadButton.setLoading(false);
    }
    this.resetPacks();
  }

  private void resetPacks() {
    this.inactivePacks.clear();
    this.activePacks.clear();
    this.toActivate.clear();
    this.toDeactivate.clear();

    ClientPaintingRegistry registry = ClientPaintingRegistry.getInstance();
    this.inactivePacks.addAll(registry.getInactivePacks());
    this.activePacks.addAll(registry.getActivePacks());
    this.resetLists();
  }

  private void resetLists() {
    if (this.inactiveList != null) {
      this.inactiveList.setPacks(this.inactivePacks);
    }
    if (this.activeList != null) {
      this.activeList.setPacks(this.activePacks);
    }
  }

  private void activatePack(PackData pack) {
    if (!this.editMode) {
      return;
    }

    if (this.inactivePacks.remove(pack)) {
      this.activePacks.add(pack);

      String packFileUid = pack.packFileUid();
      this.toDeactivate.remove(packFileUid);
      this.toActivate.add(packFileUid);

      this.resetLists();
    }
  }

  private void deactivatePack(PackData pack) {
    if (!this.editMode) {
      return;
    }

    if (this.activePacks.remove(pack)) {
      this.inactivePacks.add(pack);

      String packFileUid = pack.packFileUid();
      this.toActivate.remove(packFileUid);
      this.toDeactivate.add(packFileUid);

      this.resetLists();
    }
  }

  private void reloadPacks() {
    if (this.reloadButton != null) {
      this.reloadButton.setLoading(true);
    }
    Util.getIoWorkerExecutor().execute(() -> {
      ClientNetworking.sendReloadPacket(List.copyOf(this.toActivate), List.copyOf(this.toDeactivate));
    });
  }

  private void openPackDir() {
    assert this.client != null;
    if (!this.client.isInSingleplayer()) {
      return;
    }

    Path path = PathAccessor.getInstance().getPerWorldModDir(CustomPaintingsMod.MOD_ID);
    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      }
      Util.getOperatingSystem().open(path.toUri());
    } catch (IOException e) {
      // TODO: Handle exception
      CustomPaintingsMod.LOGGER.warn(e);
    }
  }

  private static class PackList extends NarratableEntryListWidget<PackList.Entry> {
    private final Text title;
    private final Identifier buttonTexture;
    private final Identifier highlightedButtonTexture;
    private final Consumer<PackData> transferAction;
    private final ArrayList<PackData> packs = new ArrayList<>();

    public PackList(
        MinecraftClient client,
        int width,
        int height,
        Text title,
        Identifier buttonTexture,
        Identifier highlightedButtonTexture,
        Consumer<PackData> transferAction,
        Collection<PackData> packs
    ) {
      super(client, 0, 0, width, height);

      this.title = title;
      this.buttonTexture = buttonTexture;
      this.highlightedButtonTexture = highlightedButtonTexture;
      this.transferAction = transferAction;
      this.packs.addAll(packs);

      this.setShouldHighlightSelectionDuringHover(true);
      Spacing padding = this.contentPadding.expand(Spacing.of((int) (client.textRenderer.fontHeight * 1.5f), 0, 0, 0));
      this.setContentPadding(padding);

      this.init();
    }

    public PackList(
        MinecraftClient client, ThreeSectionLayoutWidget layout, Text title, Collection<PackData> packs
    ) {
      super(client, layout);

      this.title = title;
      this.buttonTexture = null;
      this.highlightedButtonTexture = null;
      this.transferAction = null;
      this.packs.addAll(packs);

      this.setShouldHighlightHover(false);
      this.setShouldHighlightSelection(false);
      this.setAlternatingRowShading(true);
      Spacing padding = this.contentPadding.expand(Spacing.of((int) (client.textRenderer.fontHeight * 1.5f), 0, 0, 0));
      this.setContentPadding(padding);

      this.init();
    }

    @Override
    protected void renderEntries(DrawContext context, int mouseX, int mouseY, float delta) {
      TextRenderer textRenderer = this.client.textRenderer;
      Text tile = Text.empty().append(this.title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
      int centerX = this.getX() + this.getWidth() / 2;
      int posY = (this.getY() + this.getContentTop() - textRenderer.fontHeight) / 2;
      GuiUtil.drawText(context, textRenderer, tile, centerX, posY, Colors.WHITE, false, 0, Alignment.CENTER);

      super.renderEntries(context, mouseX, mouseY, delta);
    }

    public void setPacks(Collection<PackData> packs) {
      this.packs.clear();
      this.packs.addAll(packs);
      this.init();
    }

    private void init() {
      this.clearEntries();

      for (PackData pack : this.packs) {
        this.addEntry(
            PackEntry.factory(this.client.textRenderer, pack, this.buttonTexture, this.highlightedButtonTexture,
                this.transferAction
            ));
      }

      this.refreshPositions();
    }

    private static abstract class Entry extends NarratableEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }

    private static class PackEntry extends Entry {
      private static final int HEIGHT = 40;
      private static final int PACK_ICON_SIZE = 32;

      private final PackData pack;
      private final Identifier buttonTexture;
      private final Identifier highlightedButtonTexture;
      private final Consumer<PackData> transferAction;
      private final SpriteWidget icon;
      private final DrawableWidget button;

      protected PackEntry(
          int index,
          int left,
          int top,
          int width,
          TextRenderer textRenderer,
          PackData pack,
          Identifier buttonTexture,
          Identifier highlightedButtonTexture,
          Consumer<PackData> transferAction
      ) {
        super(index, left, top, width, HEIGHT);

        this.pack = pack;
        this.buttonTexture = buttonTexture;
        this.highlightedButtonTexture = highlightedButtonTexture;
        this.transferAction = transferAction;

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlign(Alignment.CENTER),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(), this.getContentTop(), this.getContentWidth(), this.getContentHeight());
            }
        );

        this.icon = layout.add(
            SpriteWidget.create(ClientPaintingRegistry.getInstance().getSprite(PackIcons.customId(pack.id()))),
            (parent, self) -> {
              self.setDimensions(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING / 2);
        textSection.add(LabelWidget.builder(textRenderer, Text.of(pack.name()))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> self.setWidth(parent.getWidth()));
        textSection.add(LabelWidget.builder(textRenderer, Text.literal(pack.id()).formatted(Formatting.GRAY))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> self.setWidth(parent.getWidth()));
        textSection.add(LabelWidget.builder(textRenderer,
                Text.translatable("custompaintings.packs.paintings", pack.paintings().size())
            )
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> self.setWidth(parent.getWidth()));
        textSection.add(LabelWidget.builder(textRenderer, Text.of(StringUtil.formatBytes(pack.fileSize())))
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> self.setWidth(parent.getWidth()));
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

        layout.forEachChild(this::addDrawable);

        if (this.transferAction == null) {
          this.button = null;
          return;
        }

        this.button = this.addDrawable(new DrawableWidget() {
          @Override
          protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            PackEntry that = PackEntry.this;
            if (!that.isMouseOver(mouseX, mouseY) && !that.isFocused()) {
              return;
            }

            Identifier buttonTexture = this.isHovered() ? that.highlightedButtonTexture : that.buttonTexture;
            context.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 100, -1601138544);
            context.drawGuiTexture(buttonTexture, this.getX(), this.getY(), 101, this.getWidth(), this.getHeight());
          }

          @Override
          public void onClick(double mouseX, double mouseY) {
            PackEntry.this.transferAction.accept(PackEntry.this.pack);
          }

          @Override
          protected boolean isValidClickButton(int button) {
            return button == 0;
          }
        });
      }

      public static FlowListWidget.EntryFactory<PackEntry> factory(
          TextRenderer textRenderer,
          PackData pack,
          Identifier buttonTexture,
          Identifier highlightedButtonTexture,
          Consumer<PackData> transferAction
      ) {
        return (index, left, top, width) -> new PackEntry(
            index, left, top, width, textRenderer, pack, buttonTexture, highlightedButtonTexture, transferAction);
      }

      @Override
      public Text getNarration() {
        return Text.of(this.pack.name());
      }

      @Override
      public void refreshPositions() {
        super.refreshPositions();
        if (this.button != null) {
          this.button.setDimensionsAndPosition(this.icon.getWidth(), this.icon.getHeight(), this.icon.getX(),
              this.icon.getY()
          );
        }
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.button != null) {
          this.button.mouseClicked(mouseX, mouseY, button);
        }
        return true;
      }
    }
  }
}
