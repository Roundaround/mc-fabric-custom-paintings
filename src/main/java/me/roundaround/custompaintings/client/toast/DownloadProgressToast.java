package me.roundaround.custompaintings.client.toast;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class DownloadProgressToast implements Toast {
  public static final Type TYPE = new Type();

  private static final long DURATION = 2000L;
  private static final Identifier TEXTURE = Identifier.withDefaultNamespace("toast/system");
  private static final Component TITLE = Component.translatable("custompaintings.toasts.download.title");
  private static final int TEXT_LEFT = 18;
  private static final int TITLE_Y = 7;
  private static final int DESCRIPTION_Y = 18;
  private static final int BAR_LEFT = 3;
  private static final int BAR_WIDTH = 154;
  private static final int BAR_RIGHT = BAR_LEFT + BAR_WIDTH;
  private static final int BAR_TOP = 28;
  private static final int BAR_HEIGHT = 1;
  private static final int BAR_BOTTOM = BAR_TOP + BAR_HEIGHT;
  private static final int BAR_COLOR_INCREASING = GuiUtil.genColorInt(0, 85, 0);
  private static final int BAR_COLOR_DECREASING = GuiUtil.genColorInt(85, 0, 0);

  private final int imagesExpected;
  private final int bytesExpected;

  private Component description;
  private int imagesReceived = 0;
  private Visibility visibility = Visibility.SHOW;
  private long lastTime;
  private float lastProgress;
  private float progress;
  private long finishTime;

  private DownloadProgressToast(int imagesExpected, int bytesExpected) {
    this.imagesExpected = imagesExpected;
    this.bytesExpected = bytesExpected;
    this.description = this.getDescription();
  }

  public static void add(ToastManager manager, int imagesExpected, int bytesExpected) {
    hide(manager);
    manager.addToast(new DownloadProgressToast(imagesExpected, bytesExpected));
  }

  public static DownloadProgressToast get(ToastManager manager) {
    return manager.getToast(DownloadProgressToast.class, TYPE);
  }

  public static void hide(ToastManager manager) {
    DownloadProgressToast toast = get(manager);
    if (toast != null) {
      toast.hide();
    }
  }

  @Override
  @NotNull
  public Object getToken() {
    return TYPE;
  }

  @Override
  @NotNull
  public Visibility getWantedVisibility() {
    return this.visibility;
  }

  @Override
  public void update(ToastManager manager, long time) {
    double displayDuration = DURATION * manager.getNotificationDisplayTimeMultiplier();
    if (this.progress >= 1f && time - this.finishTime > displayDuration) {
      this.hide();
    }
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor context, @NotNull Font textRenderer, long time) {
    context.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, this.width(), this.height());
    this.drawText(context, textRenderer);
    this.drawProgressBar(context, time);
  }

  private void drawText(GuiGraphicsExtractor context, Font textRenderer) {
    context.text(textRenderer, TITLE, TEXT_LEFT, TITLE_Y, CommonColors.YELLOW, false);
    context.text(textRenderer, this.description, TEXT_LEFT, DESCRIPTION_Y, CommonColors.YELLOW, false);
  }

  private void drawProgressBar(GuiGraphicsExtractor context, long time) {
    float lerpedProgress = Mth.clampedLerp(this.lastProgress, this.progress, (time - this.lastTime) / 100f);
    int color = this.progress >= this.lastProgress ? BAR_COLOR_INCREASING : BAR_COLOR_DECREASING;

    context.fill(BAR_LEFT, BAR_TOP, BAR_RIGHT, BAR_BOTTOM, CommonColors.WHITE);
    context.fill(BAR_LEFT, BAR_TOP, BAR_LEFT + (int) (BAR_WIDTH * lerpedProgress), BAR_BOTTOM, color);

    this.lastProgress = lerpedProgress;
    this.lastTime = time;
  }

  public void setReceived(int images, int bytes) {
    this.imagesReceived = images;

    if (bytes >= this.bytesExpected) {
      this.progress = 1f;
      this.finishTime = this.lastTime;
    } else {
      this.progress = Math.clamp((float) bytes / this.bytesExpected, 0, 1);
    }

    this.description = this.getDescription();
  }

  public void hide() {
    this.visibility = Visibility.HIDE;
  }

  private Component getDescription() {
    return Component.translatable(
        "custompaintings.toasts.download.body",
        this.imagesReceived,
        this.imagesExpected,
        Math.clamp(Math.round(100f * this.progress), 0, 100)
    );
  }

  public record Type() {
  }
}
