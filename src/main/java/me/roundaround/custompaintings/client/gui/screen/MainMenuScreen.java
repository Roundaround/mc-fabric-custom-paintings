package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  public MainMenuScreen(Screen parent) {
    super(Text.translatable("custompaintings.main.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.layout.addBody(ButtonWidget.builder(Text.translatable("custompaintings.main.config"), this::navigateConfig)
        .width(BUTTON_WIDTH)
        .build());
    this.layout.addBody(ButtonWidget.builder(Text.translatable("custompaintings.main.legacy"), this::navigateConvert)
        .width(BUTTON_WIDTH)
        .build());

    ButtonWidget migrationsButton = this.layout.addBody(
        ButtonWidget.builder(Text.translatable("custompaintings.main.migrate"), this::navigateMigrate)
            .width(BUTTON_WIDTH)
            .build());
    ButtonWidget reloadButton = this.layout.addBody(
        ButtonWidget.builder(Text.translatable("custompaintings.main.reload"), this::reloadPacks)
            .width(BUTTON_WIDTH)
            .build());

    if (this.client.world == null) {
      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.migrate.notInWorld")));

      reloadButton.active = false;
      reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notInWorld")));
    } else if (this.client.player != null && !this.client.player.hasPermissionLevel(2)) {
      migrationsButton.active = false;
      migrationsButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.migrate.notOp")));

      reloadButton.active = false;
      reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notOp")));
    }

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
    if (this.client == null) {
      return;
    }
    this.client.setScreen(this.parent);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private void navigateConfig(ButtonWidget button) {
    assert this.client != null;
    this.client.setScreen(new ConfigScreen(this, CustomPaintingsMod.MOD_ID, CustomPaintingsConfig.getInstance(),
        CustomPaintingsPerWorldConfig.getInstance()
    ));
  }

  private void navigateConvert(ButtonWidget button) {
    assert this.client != null;
    this.client.setScreen(new LegacyConvertScreen(this.client, this));
  }

  private void navigateMigrate(ButtonWidget button) {
    assert this.client != null;
    this.client.setScreen(new MigrationsScreen(this));
  }

  private void reloadPacks(ButtonWidget button) {
    assert this.client != null;
    if (this.client.player == null || this.client.world == null || !this.client.player.hasPermissionLevel(2)) {
      return;
    }

    this.client.setScreen(null);
    this.client.player.sendMessage(Text.translatable("custompaintings.main.reloadingMessage"));
    ClientNetworking.sendReloadPacket();
  }
}
