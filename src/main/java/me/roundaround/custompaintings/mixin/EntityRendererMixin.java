package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PaintingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
  @Shadow
  @Final
  protected EntityRenderManager dispatcher;

  @Shadow
  public abstract TextRenderer getTextRenderer();

  @WrapMethod(method = "hasLabel")
  public boolean hasLabelForPaintings(Entity entity, double squaredDistanceToCamera, Operation<Boolean> original) {
    if (!(entity instanceof PaintingEntity)) {
      return original.call(entity, squaredDistanceToCamera);
    }
    return entity == this.dispatcher.targetedEntity && entity.isCustomNameVisible();
  }

  @WrapMethod(method = "renderLabelIfPresent")
  public void renderLabelIfPresentForPaintings(
      EntityRenderState rawState,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      CameraRenderState cameraState,
      Operation<Void> original
  ) {
    if (!(rawState instanceof PaintingEntityRenderState state)) {
      original.call(rawState, matrices, queue, cameraState);
      return;
    }

    if (state.displayName == null) {
      return;
    }

    List<Text> label = state.custompaintings$getLabel();
    if (label == null || label.isEmpty()) {
      original.call(rawState, matrices, queue, cameraState);
      return;
    }

    PaintingData data = state.custompaintings$getData();
    TextRenderer textRenderer = this.getTextRenderer();

    matrices.push();
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - state.facing.getPositiveHorizontalDegrees()));
    matrices.translate(0f, -data.height() / 2f, -0.125f);
    matrices.scale(-0.025f, -0.025f, 0.025f);

    int textColor = GuiUtil.LABEL_COLOR;
    int seeThroughColor = GuiUtil.genColorInt(1f, 1f, 1f, 0.75f);
    int bgColor = GuiUtil.BACKGROUND_COLOR;

    float y = -(textRenderer.fontHeight + 3) / 2f;
    for (Text line : label) {
      for (OrderedText wrappedLine : textRenderer.wrapLines(line, Integer.MAX_VALUE)) {
        float x = -textRenderer.getWidth(wrappedLine) / 2f;

        queue.submitText(
            matrices,
            x,
            y,
            wrappedLine,
            false,
            TextRenderer.TextLayerType.SEE_THROUGH,
            state.light,
            seeThroughColor,
            bgColor,
            0
        );
        queue.submitText(
            matrices,
            x,
            y,
            wrappedLine,
            false,
            TextRenderer.TextLayerType.NORMAL,
            state.light,
            textColor,
            0,
            0
        );

        y += textRenderer.fontHeight + 1;
      }
    }

    matrices.pop();
  }
}
