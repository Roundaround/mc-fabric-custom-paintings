package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
  @Final
  @Shadow
  protected EntityRenderDispatcher dispatcher;

  @Final
  @Shadow
  private TextRenderer textRenderer;

  @WrapOperation(
      method = "updateRenderState", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/EntityRenderer;hasLabel(Lnet/minecraft/entity/Entity;D)Z"
  )
  )
  private boolean wrapHasLabel(
      EntityRenderer<Entity, EntityRenderState> instance,
      Entity entity,
      double squaredDistanceToCamera,
      Operation<Boolean> original,
      @Local(argsOnly = true) EntityRenderState state
  ) {
    if (!(entity instanceof PaintingEntity)) {
      return original.call(instance, entity, squaredDistanceToCamera);
    }
    return entity == this.dispatcher.targetedEntity && entity.isCustomNameVisible();
  }

  @WrapOperation(
      method = "updateRenderState", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/EntityRenderer;getDisplayName(Lnet/minecraft/entity/Entity;)" +
               "Lnet/minecraft/text/Text;"
  )
  )
  private Text wrapGetDisplayName(
      EntityRenderer<Entity, EntityRenderState> instance, Entity entity, Operation<Text> original
  ) {
    if (entity instanceof PaintingEntity) {
      return Text.empty();
    }
    return original.call(instance, entity);
  }

  @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
  protected void renderLabelIfPresent(
      EntityRenderState rawState,
      Text displayName,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci
  ) {
    if (!(rawState instanceof PaintingEntityRenderState state)) {
      return;
    }

    ci.cancel();

    PaintingData data = state.getCustomData();
    TextRenderer textRenderer = this.textRenderer;

    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.facing.asRotation()));
    matrices.translate(0, -data.height() / 2f, -0.125f);
    matrices.scale(-0.025f, -0.025f, 0.025f);

    Matrix4f matrix4f = matrices.peek().getPositionMatrix();
    float y = -(textRenderer.fontHeight + 3) / 2f;
    for (Text line : this.getEntityLabel(state)) {
      float x = -textRenderer.getWidth(line) / 2f;

      textRenderer.draw(line, x, y, GuiUtil.genColorInt(1f, 1f, 1f, 0.75f), false, matrix4f, vertexConsumers,
          TextRenderer.TextLayerType.SEE_THROUGH, GuiUtil.BACKGROUND_COLOR, light
      );
      textRenderer.draw(line, x, y, GuiUtil.LABEL_COLOR, false, matrix4f, vertexConsumers,
          TextRenderer.TextLayerType.NORMAL, 0, light
      );

      y += textRenderer.fontHeight + 1;
    }

    matrices.pop();
  }

  @Unique
  private List<Text> getEntityLabel(PaintingEntityRenderState state) {
    Text customName = state.getCustomName();
    if (customName != null) {
      return List.of(customName);
    }

    PaintingData data = state.getCustomData();
    if (data.hasLabel()) {
      return data.getLabelAsLines();
    }

    return List.of(data.getIdText());
  }
}
