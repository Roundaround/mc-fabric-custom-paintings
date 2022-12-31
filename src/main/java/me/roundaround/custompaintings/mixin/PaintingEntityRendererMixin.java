package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.Identifier;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin extends EntityRenderer<PaintingEntity> {
  protected PaintingEntityRendererMixin(Context ctx) {
    super(ctx);
  }

  @Inject(method = "getTexture", at = @At(value = "RETURN"), cancellable = true)
  private void getTexture(PaintingEntity entity, CallbackInfoReturnable<Identifier> info) {
    PaintingData paintingData = ((ExpandedPaintingEntity) entity).getCustomData();

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    if (paintingData.isEmpty() || !paintingManager.exists(paintingData.id())) {
      return;
    }

    info.setReturnValue(CustomPaintingsClientMod.customPaintingManager.getAtlasId());
  }

  @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "net/minecraft/client/render/entity/PaintingEntityRenderer.renderPainting(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/decoration/painting/PaintingEntity;IILnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/texture/Sprite;)V"))
  private void adjustRenderPaintingArgs(Args args) {
    PaintingEntity entity = args.get(2);
    PaintingData paintingData = ((ExpandedPaintingEntity) entity).getCustomData();

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    if (paintingData.isEmpty() || paintingData.isVanilla()) {
      return;
    }

    // 3 - width
    args.set(3, paintingData.getScaledWidth());
    // 4 - height
    args.set(4, paintingData.getScaledHeight());
    // 5 - front sprite
    args.set(5, paintingManager.getPaintingSprite(paintingData));
    // 6 - back sprite
    args.set(6, paintingManager.getBackSprite());
  }
}
