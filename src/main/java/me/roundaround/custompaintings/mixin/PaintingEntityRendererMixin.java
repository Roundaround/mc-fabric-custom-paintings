package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import me.roundaround.custompaintings.entity.decoration.painting.HasCustomPaintingInfo;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin {
  @Inject(method = "render", at = @At(value = "HEAD"))
  private void render(PaintingEntity entity, float f, float g, MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
    CustomPaintingInfo customPaintingInfo = ((HasCustomPaintingInfo) (Object) entity).getCustomPaintingInfo();
    CustomPaintingsMod.LOGGER.info("CustomPaintingInfo: "
        + (customPaintingInfo.isEmpty() ? "(EMPTY)" : customPaintingInfo.getName()));
  }
}
