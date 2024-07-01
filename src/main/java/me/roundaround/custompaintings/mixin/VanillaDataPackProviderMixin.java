package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.server.event.InitialDataPackLoadEvent;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VanillaDataPackProvider.class)
public abstract class VanillaDataPackProviderMixin {
  @Inject(
      method = "createManager(Lnet/minecraft/world/level/storage/LevelStorage$Session;)" +
          "Lnet/minecraft/resource/ResourcePackManager;", at = @At("HEAD")
  )
  private static void beforeInitVanillaDataPacks(
      LevelStorage.Session session, CallbackInfoReturnable<ResourcePackManager> cir
  ) {
    InitialDataPackLoadEvent.EVENT.invoker().handle(session);
  }
}
