package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.client.event.MinecraftClientEvents;
import net.minecraft.client.render.debug.DebugRenderer;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
  @Inject(method = "<init>", at = @At(value = "TAIL"))
  private void constructor(CallbackInfo info) {
    MinecraftClientEvents.AFTER_INIT.invoker().interact();
  }
}
