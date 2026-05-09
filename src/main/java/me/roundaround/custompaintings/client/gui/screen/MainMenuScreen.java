package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import me.roundaround.roundalib.client.gui.screen.ScreenParent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Util;

public class MainMenuScreen extends BaseScreen implements PacksLoadedListener {
  private static final int BUTTON_HEIGHT = Button.DEFAULT_HEIGHT;
  private static final int BUTTON_WIDTH = Button.BIG_WIDTH;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  private LoadingButtonWidget reloadButton;

  public MainMenuScreen(Screen parent) {
    super(Component.translatable("custompaintings.main.title"), new ScreenParent(parent), Minecraft.getInstance());
  }

  @Override
  protected void init() {
    boolean inWorld = this.minecraft.level != null;
    boolean inSinglePlayer = this.minecraft.isLocalServer();
    boolean hasOp = hasOps(this.minecraft.player);
    boolean canEdit = inSinglePlayer || hasOp;

    this.layout.addHeader(this.font, this.title);

    this.layout.addBody(Button.builder(Component.translatable("custompaintings.main.config"), this::navigateConfig)
        .width(BUTTON_WIDTH)
        .build());

    this.layout.addBody(Button.builder(Component.translatable("custompaintings.main.cache"), this::navigateCache)
        .width(BUTTON_WIDTH)
        .build());

    Component packsLabel = canEdit ?
        Component.translatable("custompaintings.main.packs.manage") :
        Component.translatable("custompaintings.main.packs.view");
    Button packsButton = this.layout.addBody(Button.builder(packsLabel, this::navigatePacks)
        .width(BUTTON_WIDTH)
        .build());

    this.reloadButton = this.layout.addBody(new LoadingButtonWidget(
        0,
        0,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Component.translatable("custompaintings.main.reload"),
        (b) -> this.reloadPacks()
    ));

    Button migrationsButton = this.layout.addBody(Button.builder(
        Component.translatable("custompaintings.main.migrate"),
        this::navigateMigrate
    ).width(BUTTON_WIDTH).build());

    Button legacyButton = this.layout.addBody(Button.builder(
        Component.translatable("custompaintings.main.legacy"),
        this::navigateConvert
    ).width(BUTTON_WIDTH).build());

    if (!inWorld) {
      packsButton.active = false;
      packsButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.packs.notInWorld")));

      this.reloadButton.active = false;
      this.reloadButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.reload.notInWorld")));

      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.migrate.notInWorld")));
    } else if (!canEdit) {
      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.migrate.notOp")));

      this.reloadButton.active = false;
      this.reloadButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.reload.notOp")));
    }

    if (inWorld && !inSinglePlayer) {
      legacyButton.active = false;
      legacyButton.setTooltip(Tooltip.create(Component.translatable("custompaintings.main.legacy.multiplayer")));
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
  public void onPacksLoaded() {
    if (this.reloadButton.isLoading()) {
      this.reloadButton.setLoading(false);
    }
  }

  private void navigateConfig(Button button) {
    this.minecraft.setScreen(new ConfigScreen(
        this,
        Constants.MOD_ID,
        CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()
    ));
  }

  private void navigateCache(Button button) {
    this.minecraft.setScreen(new CacheScreen(this));
  }

  private void navigatePacks(Button button) {
    boolean inSinglePlayer = this.minecraft.isLocalServer();
    boolean hasOps = hasOps(this.minecraft.player);
    this.minecraft.setScreen(new PacksScreen(this, inSinglePlayer || hasOps));
  }

  private void navigateConvert(Button button) {
    if (this.minecraft.level != null && !this.minecraft.isLocalServer()) {
      return;
    }

    this.minecraft.setScreen(new LegacyConvertScreen(this.minecraft, this));
  }

  private void navigateMigrate(Button button) {
    this.minecraft.setScreen(new MigrationsScreen(this));
  }

  private void reloadPacks() {
    if (this.minecraft.level == null || !hasOps(this.minecraft.player)) {
      return;
    }

    this.reloadButton.setLoading(true);
    Util.ioPool().execute(ClientNetworking::sendReloadPacket);
  }

  private static boolean hasOps(LocalPlayer player) {
    return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
  }
}
