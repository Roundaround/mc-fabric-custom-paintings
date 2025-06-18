package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.client.gui.screen.editor.ZeroScreen;
import me.roundaround.custompaintings.client.gui.widget.LoadingButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ConfigScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class MainMenuScreen extends BaseScreen implements PacksLoadedListener {
  private static final int BUTTON_HEIGHT = ButtonWidget.DEFAULT_HEIGHT;
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);

  private LoadingButtonWidget reloadButton;

  public MainMenuScreen(Screen parent) {
    super(
        Text.translatable("custompaintings.main.title"),
        new ScreenParent(parent),
        MinecraftClient.getInstance());
  }

  @Override
  protected void init() {
    boolean inWorld = this.client.world != null;
    boolean inSinglePlayer = this.client.isInSingleplayer();
    boolean hasOp = this.client.player != null && this.client.player.hasPermissionLevel(3);
    boolean canEdit = inSinglePlayer || hasOp;

    this.layout.addHeader(this.textRenderer, this.title);

    this.layout.addBody(ButtonWidget.builder(Text.translatable("custompaintings.main.config"), this::navigateConfig)
        .width(BUTTON_WIDTH)
        .build());

    this.layout.addBody(ButtonWidget.builder(Text.translatable("custompaintings.main.editor"), this::navigateEditor)
        .width(BUTTON_WIDTH)
        .build());

    this.layout.addBody(ButtonWidget.builder(Text.translatable("custompaintings.main.cache"), this::navigateCache)
        .width(BUTTON_WIDTH)
        .build());

    Text packsLabel = canEdit ? Text.translatable("custompaintings.main.packs.manage")
        : Text.translatable("custompaintings.main.packs.view");
    ButtonWidget packsButton = this.layout.addBody(ButtonWidget.builder(packsLabel, this::navigatePacks)
        .width(BUTTON_WIDTH)
        .build());

    this.reloadButton = this.layout.addBody(new LoadingButtonWidget(
        0,
        0,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.main.reload"),
        (b) -> this.reloadPacks()));

    ButtonWidget migrationsButton = this.layout.addBody(ButtonWidget.builder(
        Text.translatable("custompaintings.main.migrate"), this::navigateMigrate).width(BUTTON_WIDTH).build());

    ButtonWidget legacyButton = this.layout.addBody(ButtonWidget.builder(
        Text.translatable("custompaintings.main.legacy"),
        this::navigateConvert).width(BUTTON_WIDTH).build());

    if (!inWorld) {
      packsButton.active = false;
      packsButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.packs.notInWorld")));

      this.reloadButton.active = false;
      this.reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notInWorld")));

      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.migrate.notInWorld")));
    } else if (!canEdit) {
      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.migrate.notOp")));

      this.reloadButton.active = false;
      this.reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notOp")));
    }

    if (inWorld && !inSinglePlayer) {
      legacyButton.active = false;
      legacyButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.legacy.multiplayer")));
    }

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, (b) -> this.close()).width(BUTTON_WIDTH).build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.refreshWidgetPositions();
  }

  @Override
  protected void refreshWidgetPositions() {
    this.layout.refreshPositions();
  }

  @Override
  public void onPacksLoaded() {
    if (this.reloadButton.isLoading()) {
      this.reloadButton.setLoading(false);
    }
  }

  private void navigateConfig(ButtonWidget button) {
    this.client.setScreen(new ConfigScreen(
        this,
        Constants.MOD_ID,
        CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()));
  }

  private void navigateEditor(ButtonWidget button) {
    this.client.setScreen(new ZeroScreen(new ScreenParent(this), this.client));
  }

  private void navigateCache(ButtonWidget button) {
    this.client.setScreen(new CacheScreen(this));
  }

  private void navigatePacks(ButtonWidget button) {
    boolean inSinglePlayer = this.client.isInSingleplayer();
    boolean hasOps = this.client.player != null && this.client.player.hasPermissionLevel(3);
    this.client.setScreen(new PacksScreen(this, inSinglePlayer || hasOps));
  }

  private void navigateConvert(ButtonWidget button) {
    if (this.client.world != null && !this.client.isInSingleplayer()) {
      return;
    }

    this.client.setScreen(new LegacyConvertScreen(this.client, this));
  }

  private void navigateMigrate(ButtonWidget button) {
    this.client.setScreen(new MigrationsScreen(this));
  }

  private void reloadPacks() {
    if (this.client.player == null || this.client.world == null || !this.client.player.hasPermissionLevel(3)) {
      return;
    }

    this.reloadButton.setLoading(true);
    Util.getIoWorkerExecutor().execute(ClientNetworking::sendReloadPacket);
  }
}
