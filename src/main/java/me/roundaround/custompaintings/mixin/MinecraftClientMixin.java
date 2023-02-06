package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.client.event.MinecraftClientEvents;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
  @Inject(method = "close", at = @At(value = "INVOKE", target = "net/minecraft/client/resource/PeriodicNotificationManager.close()V"))
  private void close(CallbackInfo info) {
    MinecraftClientEvents.ON_CLOSE.invoker().interact();
  }

  @Inject(method = "handleInputEvents", at = @At(value = "HEAD"))
  public void handleInputEvents(CallbackInfo info) {
    MinecraftClientEvents.ON_INPUT.invoker().interact();
  }
}
