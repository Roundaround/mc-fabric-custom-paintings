package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
  @Unique
  private Entity self() {
    return (Entity) (Object) this;
  }

  @Inject(method = "shouldRenderAtSqrDistance(D)Z", at = @At(value = "HEAD"), cancellable = true)
  private void altShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
    if (!CustomPaintingsConfig.getInstance().overrideRenderDistance.getValue() ||
        !(this.self() instanceof Painting)) {
      return;
    }

    double scaled =
        CustomPaintingsConfig.getInstance().renderDistanceScale.getValue() * 64 * Entity.getViewScale();
    cir.setReturnValue(distance < scaled * scaled);
  }
}
