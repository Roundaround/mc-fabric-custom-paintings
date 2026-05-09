package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
  @Shadow
  @Final
  protected EntityRenderDispatcher entityRenderDispatcher;

  @Shadow
  public abstract Font getFont();

  @WrapMethod(method = "shouldShowName")
  public boolean shouldShowNameForPaintings(Entity entity, double distanceToCameraSq, Operation<Boolean> original) {
    if (!(entity instanceof Painting)) {
      return original.call(entity, distanceToCameraSq);
    }
    return entity == this.entityRenderDispatcher.crosshairPickEntity && entity.isCustomNameVisible();
  }

  @WrapMethod(
      method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;" +
               "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
               "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"
  )
  public void renderLabelIfPresentForPaintings(
      EntityRenderState rawState,
      PoseStack matrices,
      SubmitNodeCollector queue,
      CameraRenderState cameraState,
      Operation<Void> original
  ) {
    if (!(rawState instanceof PaintingRenderState state)) {
      original.call(rawState, matrices, queue, cameraState);
      return;
    }

    if (state.nameTag == null) {
      return;
    }

    List<Component> label = state.custompaintings$getLabel();
    if (label == null || label.isEmpty()) {
      original.call(rawState, matrices, queue, cameraState);
      return;
    }

    PaintingData data = state.custompaintings$getData();
    Font textRenderer = this.getFont();

    matrices.pushPose();
    matrices.mulPose(Axis.YP.rotationDegrees(180f - state.direction.toYRot()));
    matrices.translate(0f, -data.height() / 2f, -0.125f);
    matrices.scale(-0.025f, -0.025f, 0.025f);

    int textColor = GuiUtil.LABEL_COLOR;
    int seeThroughColor = GuiUtil.genColorInt(1f, 1f, 1f, 0.75f);
    int bgColor = GuiUtil.BACKGROUND_COLOR;

    float y = -(textRenderer.lineHeight + 3) / 2f;
    for (Component line : label) {
      for (FormattedCharSequence wrappedLine : textRenderer.split(line, Integer.MAX_VALUE)) {
        float x = -textRenderer.width(wrappedLine) / 2f;

        queue.submitText(
            matrices,
            x,
            y,
            wrappedLine,
            false,
            Font.DisplayMode.SEE_THROUGH,
            state.lightCoords,
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
            Font.DisplayMode.NORMAL,
            state.lightCoords,
            textColor,
            0,
            0
        );

        y += textRenderer.lineHeight + 1;
      }
    }

    matrices.popPose();
  }
}
