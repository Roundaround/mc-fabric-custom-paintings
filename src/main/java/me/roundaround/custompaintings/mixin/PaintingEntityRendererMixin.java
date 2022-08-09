package me.roundaround.custompaintings.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import me.roundaround.custompaintings.entity.decoration.painting.HasCustomPaintingInfo;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.Identifier;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin {
  @Inject(method = "getTexture", at = @At(value = "RETURN"), cancellable = true)
  private void getTexture(PaintingEntity entity, CallbackInfoReturnable<Identifier> info) {
    CustomPaintingInfo customPaintingInfo = ((HasCustomPaintingInfo) entity).getCustomPaintingInfo();

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    if (customPaintingInfo.isEmpty()
        || !paintingManager.exists(customPaintingInfo.getId())) {
      return;
    }

    info.setReturnValue(CustomPaintingsClientMod.customPaintingManager.getAtlasId());
  }

  @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/render/entity/PaintingEntityRenderer.renderPainting(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/decoration/painting/PaintingEntity;IILnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/texture/Sprite;)V"))
  private void adjustRenderPaintingArgs(Args args) {
    PaintingEntity entity = args.get(2);
    CustomPaintingInfo customPaintingInfo = ((HasCustomPaintingInfo) entity).getCustomPaintingInfo();

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    if (customPaintingInfo.isEmpty()
        || !paintingManager.exists(customPaintingInfo.getId())) {
      return;
    }
    
    // 3 - width
    // 4 - height
    args.set(3, customPaintingInfo.getScaledWidth());
    args.set(4, customPaintingInfo.getScaledHeight());

    Identifier id = customPaintingInfo.getId();
    Optional<Sprite> maybeSprite = paintingManager.getPaintingSprite(id);
    maybeSprite.ifPresent((sprite) -> {
      // 5 - front sprite
      // 6 - back sprite
      args.set(5, sprite);
      args.set(6, paintingManager.getBackSprite());
    });
  }
}
