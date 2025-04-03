package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
  @Shadow
  @Final
  protected EntityRenderDispatcher dispatcher;

  @Shadow
  public abstract TextRenderer getTextRenderer();

  @WrapMethod(method = "hasLabel")
  public boolean hasLabelForPaintings(Entity entity, double squaredDistanceToCamera, Operation<Boolean> original) {
    if (!(entity instanceof PaintingEntity)) {
      return original.call(entity, squaredDistanceToCamera);
    }
    return entity == this.dispatcher.targetedEntity && entity.isCustomNameVisible();
  }

  @WrapMethod(method = "getDisplayName")
  public @Nullable Text getDisplayNameForPaintings(Entity entity, Operation<Text> original) {
    if (!(entity instanceof PaintingEntity)) {
      return original.call(entity);
    }
    return Text.empty();
  }

  @WrapMethod(method = "renderLabelIfPresent")
  public void renderLabelIfPresentForPaintings(
      EntityRenderState rawState,
      Text text,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumers,
      int light,
      Operation<Void> original
  ) {
    if (!(rawState instanceof PaintingEntityRenderState state)) {
      original.call(rawState, text, matrixStack, vertexConsumers, light);
      return;
    }

    List<Text> label = state.custompaintings$getLabel();
    PaintingData data = state.custompaintings$getData();
    TextRenderer textRenderer = this.getTextRenderer();

    matrixStack.push();
    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.facing.getPositiveHorizontalDegrees()));
    matrixStack.translate(0, -data.height() / 2f, -0.125f);
    matrixStack.scale(-0.025f, -0.025f, 0.025f);

    Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
    float y = -(textRenderer.fontHeight + 3) / 2f;
    for (Text line : label) {
      float x = -textRenderer.getWidth(line) / 2f;

      textRenderer.draw(
          line,
          x,
          y,
          GuiUtil.genColorInt(1f, 1f, 1f, 0.75f),
          false,
          matrix4f,
          vertexConsumers,
          TextRenderer.TextLayerType.SEE_THROUGH,
          GuiUtil.BACKGROUND_COLOR,
          light
      );
      textRenderer.draw(
          line,
          x,
          y,
          GuiUtil.LABEL_COLOR,
          false,
          matrix4f,
          vertexConsumers,
          TextRenderer.TextLayerType.NORMAL,
          0,
          light
      );

      y += textRenderer.fontHeight + 1;
    }

    matrixStack.pop();
  }
}
