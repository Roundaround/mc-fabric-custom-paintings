package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
  @Shadow
  private static double renderDistanceMultiplier;

  @Unique
  private Entity self() {
    return (Entity) (Object) this;
  }

  @Inject(method = "shouldRender(D)Z", at = @At(value = "HEAD"), cancellable = true)
  private void altShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
    if (!CustomPaintingsConfig.getInstance().overrideRenderDistance.getValue() ||
        !(this.self() instanceof PaintingEntity)) {
      return;
    }

    double scaled = CustomPaintingsConfig.getInstance().renderDistanceScale.getValue() * 64 * renderDistanceMultiplier;
    cir.setReturnValue(distance < scaled * scaled);
  }
}
