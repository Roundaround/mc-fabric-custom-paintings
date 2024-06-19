package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Redirect(
      method = "updateCrosshairTarget", at = @At(
      value = "FIELD",
      target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;",
      opcode = Opcodes.PUTFIELD
  )
  )
  private void updateTargetedEntity(MinecraftClient client, HitResult hitResult) {
    client.crosshairTarget = hitResult;

    if (!(hitResult instanceof EntityHitResult)) {
      return;
    }

    Entity entity = ((EntityHitResult) hitResult).getEntity();
    if (entity instanceof ExpandedPaintingEntity) {
      client.targetedEntity = entity;
    }
  }
}
