package me.roundaround.custompaintings.mixin;

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
import net.minecraft.util.Colors;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

  @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
  protected void renderLabelIfPresent(
      Entity entity,
      Text text,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumers,
      int light,
      float tickDelta,
      CallbackInfo ci
  ) {
    if (!(entity instanceof PaintingEntity painting)) {
      return;
    }

    ci.cancel();

    if (painting != this.dispatcher.targetedEntity || this.dispatcher.getSquaredDistanceToCamera(painting) > 4096) {
      return;
    }

    PaintingData paintingData = painting.getCustomData();
    List<Text> lines = paintingData.hasLabel() ? paintingData.getLabelAsLines() : List.of(text);
    TextRenderer textRenderer = this.textRenderer;

    float bgOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f);
    int bgColor = (int) (bgOpacity * 255f) << 24;

    matrixStack.push();
    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - painting.getHorizontalFacing().asRotation()));
    matrixStack.translate(0, 0.5f - paintingData.height() / 2f, -0.125f);
    matrixStack.scale(-0.025f, -0.025f, 0.025f);

    Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
    float y = 0;
    for (Text line : lines) {
      float x = -textRenderer.getWidth(line) / 2f;

      textRenderer.draw(line, x, y, 0x20FFFFFF, false, matrix4f, vertexConsumers,
          TextRenderer.TextLayerType.SEE_THROUGH, bgColor, light
      );
      textRenderer.draw(line, x, y, Colors.WHITE, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL,
          0, light
      );

      y += textRenderer.fontHeight + 1;
    }

    matrixStack.pop();
  }
}
