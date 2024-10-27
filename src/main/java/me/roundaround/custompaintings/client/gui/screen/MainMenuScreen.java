package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.screen.ConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
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

    this.layout.getBody().mainAxisContentAlignStart();
    this.layout.addBody(
        ButtonWidget.builder(Text.translatable("custompaintings.main.config"), this::navigateConfig).build());
    this.layout.addBody(
        ButtonWidget.builder(Text.translatable("custompaintings.main.legacy"), this::navigateConvert).build());

    ButtonWidget reloadButton = this.layout.addBody(
        ButtonWidget.builder(Text.translatable("custompaintings.main.reload"), this::reloadPacks).build());
    if (this.client.world == null) {
      reloadButton.active = false;
      reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notInWorld")));
    } else if (this.client.player != null && !this.client.player.hasPermissionLevel(2)) {
      reloadButton.active = false;
      reloadButton.setTooltip(Tooltip.of(Text.translatable("custompaintings.main.reload.notOp")));
    }

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).build());

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
