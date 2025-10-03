package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {
  @Inject(method = "onReloadSuccess", at = @At("HEAD"))
  private void onResourcePackReload(CallbackInfo ci) {
    ItemManager.getInstance().rebuild();
  }
}
