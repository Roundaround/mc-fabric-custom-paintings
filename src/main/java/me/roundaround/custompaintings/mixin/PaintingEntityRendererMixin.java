package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PaintingEntityRenderer.class)
public abstract class PaintingEntityRendererMixin extends EntityRenderer<PaintingEntity, PaintingEntityRenderState> {
  public PaintingEntityRendererMixin(EntityRendererFactory.Context context) {
    super(context);
  }

  @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/decoration/painting/PaintingEntity;" +
               "Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;F)V", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntry;value()Ljava/lang/Object;"
  )
  )
  private Object getAltVariant(Object original, @Local(argsOnly = true) PaintingEntity painting) {
    PaintingData data = painting.custompaintings$getData();
    if (data.isEmpty() || data.vanilla()) {
      return original;
    }
    return data.toVariant();
  }

  @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/decoration/painting/PaintingEntity;" +
               "Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;F)V", at = @At("RETURN")
  )
  private void afterUpdateRenderState(
      PaintingEntity painting,
      PaintingEntityRenderState renderState,
      float f,
      CallbackInfo ci
  ) {
    renderState.custompaintings$setData(painting.custompaintings$getData());
    renderState.custompaintings$setLabel(this.getMultilineLabel(painting));
  }

  @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;" +
               "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
          value = "INVOKE", target = "Lnet/minecraft/client/texture/Sprite;getAtlasId()Lnet/minecraft/util/Identifier;"
      )
  )
  private Identifier swapAtlasId(
      Sprite instance,
      Operation<Identifier> original,
      @Local(argsOnly = true) PaintingEntityRenderState renderState
  ) {
    PaintingData data = renderState.custompaintings$getData();
    if (data.vanilla()) {
      return original.call(instance);
    }
    return ClientPaintingRegistry.getInstance().getAtlasId();
  }

  @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/PaintingEntityRenderState;" +
               "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/render/entity/PaintingEntityRenderer;renderPainting" +
                   "(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;" +
                   "[IIILnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/texture/Sprite;)V"
      )
  )
  private void modifyRenderPaintingArgs(
      PaintingEntityRenderer instance,
      MatrixStack matrices,
      VertexConsumer vertexConsumer,
      int[] lightmapCoordinates,
      int width,
      int height,
      Sprite frontSprite,
      Sprite backSprite,
      Operation<Void> original,
      @Local(argsOnly = true) PaintingEntityRenderState renderState
  ) {
    PaintingData data = renderState.custompaintings$getData();
    if (!data.isEmpty() && !data.vanilla()) {
      ClientPaintingRegistry registry = ClientPaintingRegistry.getInstance();
      width = data.width();
      height = data.height();
      frontSprite = registry.getSprite(data);
      backSprite = registry.getBackSprite();
    }

    original.call(instance, matrices, vertexConsumer, lightmapCoordinates, width, height, frontSprite, backSprite);
  }

  @Unique
  private List<Text> getMultilineLabel(PaintingEntity painting) {
    Text customName = painting.getCustomName();
    if (customName != null) {
      return List.of(customName);
    }

    PaintingData data = painting.custompaintings$getData();
    if (data.isEmpty()) {
      return null;
    }

    if (data.hasLabel()) {
      return data.getLabelAsLines();
    }

    return List.of(data.getIdText());
  }
}
