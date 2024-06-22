package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
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

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
  @Unique
  private static final MinecraftClient MINECRAFT = MinecraftClient.getInstance();

  @Final
  @Shadow
  protected EntityRenderDispatcher dispatcher;

  @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
  private void renderLabelIfPresent(
      T entity,
      Text text,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      float tickDelta,
      CallbackInfo info
  ) {
    if (!(entity instanceof PaintingEntity painting)) {
      return;
    }
    if (!(entity instanceof ExpandedPaintingEntity expandedPainting)) {
      return;
    }

    info.cancel();

    PaintingData paintingData = expandedPainting.getCustomData();

    if (!paintingData.hasLabel()) {
      return;
    }

    if (this.dispatcher.getSquaredDistanceToCamera(painting) > 4096) {
      return;
    }

    if (MINECRAFT.targetedEntity != painting) {
      return;
    }

    TextRenderer textRenderer = MINECRAFT.textRenderer;

    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - painting.getHorizontalFacing().asRotation()));
    matrices.translate(0, 0.5f - paintingData.height() / 2f, -0.125f);
    matrices.scale(-0.025f, -0.025f, 0.025f);

    Matrix4f matrix4f = matrices.peek().getPositionMatrix();
    float opacity = MINECRAFT.options.getTextBackgroundOpacity(0.25f);
    int color = (int) (opacity * 255f) << 24;
    float posX = -textRenderer.getWidth(text) / 2f;
    textRenderer.draw(text, posX, 0, 0x20FFFFFF, false, matrix4f, vertexConsumers,
        TextRenderer.TextLayerType.SEE_THROUGH, color, light
    );
    textRenderer.draw(text, posX, 0, -1, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);

    matrices.pop();
  }
}
