package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.SpriteWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.toast.CustomSystemToasts;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.resource.ResourceUtil;
import me.roundaround.custompaintings.util.StringUtil;
import me.roundaround.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.Alignment;
import me.roundaround.roundalib.client.gui.util.Axis;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.util.Spacing;
import me.roundaround.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.DrawableWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.roundalib.util.PathAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

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
  private static final Component TITLE_MANAGE = Component.translatable("custompaintings.packs.manage");
  private static final Component TITLE_VIEW = Component.translatable("custompaintings.packs.view");
  private static final Component LIST_INACTIVE = Component.translatable("custompaintings.packs.inactive");
  private static final Component LIST_ACTIVE = Component.translatable("custompaintings.packs.active");
  private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
  private static final int BUTTON_WIDTH = Button.SMALL_WIDTH;
  private static final int LIST_WIDTH = 200;
  private static final Identifier SELECT_TEXTURE = Identifier.withDefaultNamespace("transferable_list/select");
  private static final Identifier SELECT_HIGHLIGHTED_TEXTURE = Identifier.withDefaultNamespace(
      "transferable_list/select_highlighted");
  private static final Identifier UNSELECT_TEXTURE = Identifier.withDefaultNamespace("transferable_list/unselect");
  private static final Identifier UNSELECT_HIGHLIGHTED_TEXTURE = Identifier.withDefaultNamespace(
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
    this.resetPacks();

    boolean inSinglePlayer = this.minecraft.isLocalServer();

    this.layout.addHeader(this.font, this.title);
    if (inSinglePlayer) {
      this.layout.addHeader(
          this.font,
          Component.translatable("custompaintings.packs.drop").withStyle(ChatFormatting.GRAY)
      );
    }

    if (this.editMode) {
      this.layout.getBody().flowAxis(Axis.HORIZONTAL).spacing(30);
      this.inactiveList = this.layout.addBody(
          new PackList(
              this.minecraft,
              LIST_WIDTH,
              this.layout.getBodyHeight(),
              LIST_INACTIVE,
              SELECT_TEXTURE,
              SELECT_HIGHLIGHTED_TEXTURE,
              this::activatePack,
              this.inactivePacks
          ), (parent, self) -> {
            self.setSize(LIST_WIDTH, parent.getHeight());
          }
      );
      this.activeList = this.layout.addBody(
          new PackList(
              this.minecraft,
              LIST_WIDTH,
              this.layout.getBodyHeight(),
              LIST_ACTIVE,
              UNSELECT_TEXTURE,
              UNSELECT_HIGHLIGHTED_TEXTURE,
              this::deactivatePack,
              this.activePacks
          ), (parent, self) -> {
            self.setSize(LIST_WIDTH, parent.getHeight());
          }
      );

      this.reloadButton = this.layout.addFooter(new LoadingButtonWidget(
          0,
          0,
          BUTTON_WIDTH,
          BUTTON_HEIGHT,
          Component.nullToEmpty("Reload Packs"),
          (b) -> this.reloadPacks()
      ));

      Button openDirButton = this.layout.addFooter(Button.builder(
          Component.translatable("custompaintings.packs.open"),
          (b) -> this.openPackDir()
      ).width(BUTTON_WIDTH).build());
      if (!inSinglePlayer) {
        openDirButton.active = false;
        openDirButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.packs.open.notInWorld")));
      }
    } else {
      this.activeList = this.layout.addBody(new PackList(this.minecraft, this.layout, LIST_ACTIVE, this.activePacks));
    }

    this.layout.addFooter(Button.builder(CommonComponents.GUI_DONE, (b) -> this.onClose()).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.font, this.layout);

    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  @Override
  protected void repositionElements() {
    this.layout.arrangeElements();
  }

  @Override
  public void onFilesDrop(@NotNull List<Path> paths) {
    if (!this.minecraft.isLocalServer()) {
      return;
    }

    List<Path> packPaths = paths.stream().filter(ResourceUtil::isPaintingPack).toList();

    if (packPaths.isEmpty()) {
      return;
    }

    Path packsDirectory = PathAccessor.getInstance().getPerWorldModDir(Constants.MOD_ID);
    String packList = packPaths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));

    this.minecraft.setScreen(new ConfirmScreen(
        (confirmed) -> {
          if (confirmed) {
            boolean allSuccessful = true;

            this.ensurePacksDirExists(packsDirectory);

            for (Path src : packPaths) {
              if (!ResourceUtil.isPaintingPack(src)) {
                CustomPaintingsMod.LOGGER.warn("Entry is not a painting pack; skipping: {}", src);
                allSuccessful = false;
                continue;
              }

              Path dest = packsDirectory.resolve(src.getFileName());
              try {
                Files.copy(src, dest);
              } catch (IOException e) {
                CustomPaintingsMod.LOGGER.warn(
                    String.format("Failed to copy painting pack from %s to %s", src, dest),
                    e
                );
                allSuccessful = false;
              }
            }

            if (!allSuccessful) {
              CustomSystemToasts.addPackCopyFailure(this.minecraft, packsDirectory.toString());
            }

            this.reloadPacks();
          }

          this.minecraft.setScreen(this);
        }, Component.translatable("custompaintings.packs.copyConfirm"), Component.nullToEmpty(packList)
    ));
  }

  @Override
  public void onClose() {
    if (!this.toActivate.isEmpty() || !this.toDeactivate.isEmpty()) {
      this.reloadPacks();
    }
    this.minecraft.setScreen(this.parent);
  }

  @Override
  public void onPacksLoaded() {
    if (this.reloadButton != null) {
      this.reloadButton.setLoading(false);
    }
    this.resetPacks();
  }

  @Override
  public void onPackTexturesInitialized() {
    if (this.inactiveList != null) {
      this.inactiveList.refresh();
    }
    if (this.activeList != null) {
      this.activeList.refresh();
    }
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
    Util.ioPool().execute(() -> {
      ClientNetworking.sendReloadPacket(List.copyOf(this.toActivate), List.copyOf(this.toDeactivate));
    });
  }

  private void openPackDir() {
    if (!this.minecraft.isLocalServer()) {
      return;
    }

    Path path = PathAccessor.getInstance().getPerWorldModDir(Constants.MOD_ID);
    this.ensurePacksDirExists(path);
    Util.getPlatform().openUri(path.toUri());
  }

  private void ensurePacksDirExists(Path path) {
    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      }
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn(String.format("Failed to create packs directory %s", path), e);
    }
  }

  private static class PackList extends NarratableEntryListWidget<PackList.Entry> {
    private final Component title;
    private final Identifier buttonTexture;
    private final Identifier highlightedButtonTexture;
    private final Consumer<PackData> transferAction;
    private final ArrayList<PackData> packs = new ArrayList<>();

    public PackList(
        Minecraft client,
        int width,
        int height,
        Component title,
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
      Spacing padding = this.contentPadding.expand(Spacing.of((int) (client.font.lineHeight * 1.5f), 0, 0, 0));
      this.setContentPadding(padding);

      this.init();
    }

    public PackList(Minecraft client, ThreeSectionLayoutWidget layout, Component title, Collection<PackData> packs) {
      super(client, layout);

      this.title = title;
      this.buttonTexture = null;
      this.highlightedButtonTexture = null;
      this.transferAction = null;
      this.packs.addAll(packs);

      this.setShouldHighlightHover(false);
      this.setShouldHighlightSelection(false);
      this.setAlternatingRowShading(true);
      Spacing padding = this.contentPadding.expand(Spacing.of((int) (client.font.lineHeight * 1.5f), 0, 0, 0));
      this.setContentPadding(padding);

      this.init();
    }

    @Override
    protected void renderEntries(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      Font textRenderer = this.client.font;
      Component tile = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
      int centerX = this.getX() + this.getWidth() / 2;
      int posY = (this.getY() + this.getContentTop() - textRenderer.lineHeight) / 2;
      GuiUtil.drawText(context, textRenderer, tile, centerX, posY, CommonColors.WHITE, false, 0, Alignment.CENTER);

      super.renderEntries(context, mouseX, mouseY, delta);
    }

    public void setPacks(Collection<PackData> packs) {
      this.packs.clear();
      this.packs.addAll(packs);
      this.init();
    }

    public void refresh() {
      this.init();
    }

    private void init() {
      this.clearEntries();

      for (PackData pack : this.packs) {
        this.addEntry(PackEntry.factory(
            this.client.font,
            pack,
            this.buttonTexture,
            this.highlightedButtonTexture,
            this.transferAction
        ));
      }

      this.arrangeElements();
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
          Font textRenderer,
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
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight()
              );
            }
        );

        this.icon = layout.add(
            SpriteWidget.create(ClientPaintingRegistry.getInstance().getSprite(PackIcons.customId(pack.id()))),
            (parent, self) -> {
              self.setSize(PACK_ICON_SIZE, PACK_ICON_SIZE);
            }
        );

        layout.add(FillerWidget.empty());

        LinearLayoutWidget textSection = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING / 2);
        textSection.add(
            LabelWidget.builder(textRenderer, Component.nullToEmpty(pack.name()))
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(
            LabelWidget.builder(textRenderer, Component.literal(pack.id()).withStyle(ChatFormatting.GRAY))
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(
            LabelWidget.builder(
                    textRenderer,
                    Component.translatable("custompaintings.packs.paintings", pack.paintings().size())
                )
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> self.setWidth(parent.getWidth())
        );
        textSection.add(
            LabelWidget.builder(textRenderer, Component.nullToEmpty(StringUtil.formatBytes(pack.fileSize())))
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> self.setWidth(parent.getWidth())
        );
        layout.add(
            textSection, (parent, self) -> {
              int textSectionWidth = this.getContentWidth();
              textSectionWidth -= (parent.getChildren().size() - 1) * parent.getSpacing();
              for (LayoutElement widget : parent.getChildren()) {
                if (widget != self) {
                  textSectionWidth -= widget.getWidth();
                }
              }
              self.setWidth(textSectionWidth);
            }
        );

        layout.visitWidgets(this::addDrawable);

        if (this.transferAction == null) {
          this.button = null;
          return;
        }

        this.button = this.addDrawable(new DrawableWidget() {
          @Override
          protected void extractWidgetRenderState(
              @NotNull GuiGraphicsExtractor context,
              int mouseX,
              int mouseY,
              float delta
          ) {
            PackEntry that = PackEntry.this;
            if (!that.isMouseOver(mouseX, mouseY) && !that.isFocused()) {
              return;
            }

            Identifier buttonTexture = this.isHovered() ? that.highlightedButtonTexture : that.buttonTexture;
            context.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), -1601138544);
            context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                buttonTexture,
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                -1
            );
          }

          @Override
          public void onClick(@NotNull MouseButtonEvent click, boolean doubled) {
            PackEntry.this.transferAction.accept(PackEntry.this.pack);
          }

          @Override
          protected boolean isValidClickButton(MouseButtonInfo input) {
            return input.button() == 0;
          }
        });
      }

      public static FlowListWidget.EntryFactory<PackEntry> factory(
          Font textRenderer,
          PackData pack,
          Identifier buttonTexture,
          Identifier highlightedButtonTexture,
          Consumer<PackData> transferAction
      ) {
        return (index, left, top, width) -> new PackEntry(
            index,
            left,
            top,
            width,
            textRenderer,
            pack,
            buttonTexture,
            highlightedButtonTexture,
            transferAction
        );
      }

      @Override
      public Component getNarration() {
        return Component.nullToEmpty(this.pack.name());
      }

      @Override
      public void arrangeElements() {
        super.arrangeElements();
        if (this.button != null) {
          this.button.setRectangle(this.icon.getWidth(), this.icon.getHeight(), this.icon.getX(), this.icon.getY());
        }
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.button != null) {
          this.button.mouseClicked(click, doubled);
        }
        return true;
      }
    }
  }
}
