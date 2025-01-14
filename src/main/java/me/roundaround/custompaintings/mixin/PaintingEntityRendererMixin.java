package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.client.texture.PaintingManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin {
  @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;" +
               "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/texture/PaintingManager;getBackSprite()Lnet/minecraft/client/texture/Sprite;"
      )
  )
  private Sprite getAltBackSprite(Sprite original, @Local(argsOnly = true) PaintingEntityRenderState state) {
    PaintingData data = state.getCustomData();
    if (data.vanilla()) {
      return original;
    }
    return ClientPaintingRegistry.getInstance().getBackSprite();
  }

  @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;" +
               "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/texture/PaintingManager;getPaintingSprite" +
                   "(Lnet/minecraft/entity/decoration/painting/PaintingVariant;)Lnet/minecraft/client/texture/Sprite;"
      )
  )
  private Sprite getAltFrontSprite(
      PaintingManager instance,
      PaintingVariant variant,
      Operation<Sprite> original,
      @Local(argsOnly = true) PaintingEntityRenderState state
  ) {
    PaintingData data = state.getCustomData();
    if (data.vanilla()) {
      return original.call(instance, variant);
    }
    return ClientPaintingRegistry.getInstance().getSprite(data);
  }

  @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/decoration/painting/PaintingEntity;" +
               "Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;F)V", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/EntityRenderer;updateRenderState" +
               "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
      shift = At.Shift.AFTER
  )
  )
  private void setCustomDataToState(
      PaintingEntity painting, PaintingEntityRenderState state, float tickDelta, CallbackInfo ci
  ) {
    state.setCustomData(painting.getCustomData());
    state.setCustomName(painting.getCustomName());
  }

  @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/decoration/painting/PaintingEntity;" +
               "Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntry;value()Ljava/lang/Object;")
  )
  private Object getAltVariant(Object original, @Local(argsOnly = true) PaintingEntity painting) {
    PaintingData data = painting.getCustomData();
    if (data.isEmpty() || data.vanilla()) {
      return original;
    }
    return data.toVariant();
  }
}
