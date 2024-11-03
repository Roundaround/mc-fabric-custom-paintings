package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin extends EntityRenderer<PaintingEntity> {
  protected PaintingEntityRendererMixin(Context ctx) {
    super(ctx);
  }

  @Inject(
      method = "getTexture(Lnet/minecraft/entity/decoration/painting/PaintingEntity;)Lnet/minecraft/util/Identifier;",
      at = @At(value = "RETURN"),
      cancellable = true
  )
  private void getTexture(PaintingEntity painting, CallbackInfoReturnable<Identifier> info) {
    PaintingData paintingData = painting.getCustomData();
    if (paintingData.vanilla()) {
      return;
    }

    info.setReturnValue(ClientPaintingRegistry.getInstance().getAtlasId());
  }

  @ModifyArgs(
      method = "render(Lnet/minecraft/entity/decoration/painting/PaintingEntity;" +
          "FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
          value = "INVOKE",
          target = "net/minecraft/client/render/entity/PaintingEntityRenderer.renderPainting" +
              "(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;" +
              "Lnet/minecraft/entity/decoration/painting/PaintingEntity;IILnet/minecraft/client/texture/Sprite;" +
              "Lnet/minecraft/client/texture/Sprite;)V"
      )
  )
  private void adjustRenderPaintingArgs(Args args) {
    PaintingEntity entity = args.get(2);
    PaintingData paintingData = entity.getCustomData();

    if (paintingData.vanilla()) {
      return;
    }

    ClientPaintingRegistry registry = ClientPaintingRegistry.getInstance();

    // 3 - width
    args.set(3, paintingData.getScaledWidth());
    // 4 - height
    args.set(4, paintingData.getScaledHeight());

    // 5 - front sprite
    args.set(5, registry.getSprite(paintingData));
    // 6 - back sprite
    args.set(6, registry.getBackSprite());
  }
}
