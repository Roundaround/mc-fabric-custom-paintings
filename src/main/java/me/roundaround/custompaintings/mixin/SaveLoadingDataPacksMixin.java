package me.roundaround.custompaintings.mixin;

import com.mojang.datafixers.util.Pair;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldLoader.PackConfig.class)
public abstract class SaveLoadingDataPacksMixin {
  @Shadow
  @Final
  private boolean safeMode;

  @Inject(method = "createResourceManager", at = @At(value = "RETURN"))
  private void onLoad(CallbackInfoReturnable<Pair<WorldDataConfiguration, CloseableResourceManager>> cir) {
    ServerPaintingRegistry.getInstance().firstLoadPaintingPacks(this.safeMode);
  }
}
