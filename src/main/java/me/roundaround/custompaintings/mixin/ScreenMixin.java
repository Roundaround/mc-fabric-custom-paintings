package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.LegacyConvertScreen;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
  @Inject(
      method = "handleTextClick", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/text/Style;getClickEvent()Lnet/minecraft/text/ClickEvent;",
      shift = At.Shift.AFTER
  ), cancellable = true
  )
  public void interceptTextClick(Style style, CallbackInfoReturnable<Boolean> cir) {
    ClickEvent clickEvent = style.getClickEvent();
    if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND) {
      return;
    }

    switch (clickEvent.getValue()) {
      case CustomPaintingsMod.MSG_CMD_IGNORE -> this.onPress(this::ignore, cir);
      case CustomPaintingsMod.MSG_CMD_OPEN_CONVERT_SCREEN -> this.onPress(this::openConvertScreen, cir);
    }
  }

  @Unique
  private void onPress(Runnable action, CallbackInfoReturnable<Boolean> cir) {
    GuiUtil.playClickSound();
    action.run();
    cir.setReturnValue(true);
  }

  @Unique
  private void ignore() {
    CustomPaintingsPerWorldConfig.getInstance().silenceConvertPrompt.setValue(true);
    CustomPaintingsPerWorldConfig.getInstance().writeToStore();

    MinecraftClient client = GuiUtil.getClient();
    if (client.currentScreen instanceof ChatScreen) {
      client.setScreen(null);
    }
  }

  @Unique
  private void openConvertScreen() {
    MinecraftClient client = GuiUtil.getClient();
    client.setScreen(new LegacyConvertScreen(client, null));
  }
}
