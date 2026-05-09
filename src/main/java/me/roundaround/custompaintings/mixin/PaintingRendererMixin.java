package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PaintingRenderer.class)
public abstract class PaintingRendererMixin extends EntityRenderer<Painting, PaintingRenderState> {
  public PaintingRendererMixin(EntityRendererProvider.Context context) {
    super(context);
  }

  @ModifyExpressionValue(
      method = "extractRenderState(Lnet/minecraft/world/entity/decoration/painting/Painting;" +
               "Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"
  )
  )
  private Object getAltVariant(
      Object original,
      final Painting entity,
      final PaintingRenderState state,
      final float partialTicks
  ) {
    PaintingData data = entity.custompaintings$getData();
    if (data.isEmpty() || data.vanilla()) {
      return original;
    }
    return data.toVariant();
  }

  @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/decoration/painting/Painting;" +
               "Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At("RETURN")
  )
  private void afterUpdateRenderState(Painting entity, PaintingRenderState state, float partialTicks, CallbackInfo ci) {
    state.custompaintings$setData(entity.custompaintings$getData());
    state.custompaintings$setLabel(this.getMultilineLabel(entity));
  }

  @WrapOperation(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;" +
               "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
               "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;atlasLocation()" +
               "Lnet/minecraft/resources/Identifier;"
  )
  )
  private Identifier swapAtlasId(
      TextureAtlasSprite instance,
      Operation<Identifier> original,
      final PaintingRenderState state
  ) {
    PaintingData data = state.custompaintings$getData();
    if (data.isEmpty() || data.vanilla()) {
      return original.call(instance);
    }
    return ClientPaintingRegistry.getInstance().getAtlasId();
  }

  @WrapOperation(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;" +
               "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
               "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/entity/PaintingRenderer;renderPainting" +
               "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
               "Lnet/minecraft/client/renderer/rendertype/RenderType;" +
               "[IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;" +
               "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
  )
  )
  private void modifyRenderPaintingArgs(
      PaintingRenderer instance,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      RenderType renderType,
      int[] lightCoordsMap,
      int width,
      int height,
      TextureAtlasSprite frontSprite,
      TextureAtlasSprite backSprite,
      Operation<Void> original,
      final PaintingRenderState state
  ) {
    PaintingData data = state.custompaintings$getData();
    if (!data.isEmpty() && !data.vanilla()) {
      ClientPaintingRegistry registry = ClientPaintingRegistry.getInstance();
      width = data.width();
      height = data.height();
      frontSprite = registry.getSprite(data);
      backSprite = registry.getBackSprite();
    }

    original.call(
        instance,
        poseStack,
        submitNodeCollector,
        renderType,
        lightCoordsMap,
        width,
        height,
        frontSprite,
        backSprite
    );
  }

  @Unique
  private List<Component> getMultilineLabel(Painting painting) {
    Component customName = painting.getCustomName();
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
