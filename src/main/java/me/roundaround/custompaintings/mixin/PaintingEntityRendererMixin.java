package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
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
    if (paintingData.isVanilla()) {
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

    if (paintingData.isVanilla()) {
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

  @Override
  protected boolean hasLabel(PaintingEntity painting) {
    return (painting.isCustomNameVisible() && painting == this.dispatcher.targetedEntity) || super.hasLabel(painting);
  }

  @Override
  protected void renderLabelIfPresent(
      PaintingEntity painting,
      Text text,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumers,
      int light,
      float tickDelta
  ) {
    if (this.dispatcher.getSquaredDistanceToCamera(painting) > 4096) {
      return;
    }

    PaintingData paintingData = painting.getCustomData();
    Text textToRender = paintingData.hasLabel() ? paintingData.getLabel() : text;
    TextRenderer textRenderer = this.getTextRenderer();

    float bgOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f);
    int bgColor = (int) (bgOpacity * 255f) << 24;
    float x = -textRenderer.getWidth(text) / 2f;
    float y = 0;

    matrixStack.push();
    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - painting.getHorizontalFacing().asRotation()));
    matrixStack.translate(0, 0.5f - paintingData.height() / 2f, -0.125f);
    matrixStack.scale(-0.025f, -0.025f, 0.025f);

    Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
    textRenderer.draw(textToRender, x, y, 0x20FFFFFF, false, matrix4f, vertexConsumers,
        TextRenderer.TextLayerType.SEE_THROUGH, bgColor, light
    );
    textRenderer.draw(textToRender, x, y, Colors.WHITE, false, matrix4f, vertexConsumers,
        TextRenderer.TextLayerType.NORMAL, 0, light
    );

    matrixStack.pop();
  }
}
