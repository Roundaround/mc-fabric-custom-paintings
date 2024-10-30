package me.roundaround.custompaintings.mixin;

import com.mojang.datafixers.util.Pair;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.server.SaveLoading;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SaveLoading.DataPacks.class)
public abstract class SaveLoadingDataPacksMixin {
  @Shadow
  @Final
  private boolean safeMode;

  @Inject(method = "load", at = @At(value = "RETURN"))
  private void onLoad(CallbackInfoReturnable<Pair<DataConfiguration, LifecycledResourceManager>> cir) {
    if (this.safeMode) {
      return;
    }
    ServerPaintingRegistry.getInstance().firstLoadPaintingPacks();
  }
}
