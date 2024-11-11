package me.roundaround.custompaintings.client.toast;

import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class DownloadProgressToast implements Toast {
  private static final long DURATION = 2000L;
  private static final Identifier TEXTURE = new Identifier(Identifier.DEFAULT_NAMESPACE, "toast/system");
  // TODO: i18n
  private static final Text TITLE = Text.of("Downloading images");
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

  private Text description;
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

  public static DownloadProgressToast create(MinecraftClient client, int imagesExpected, int bytesExpected) {
    DownloadProgressToast toast = new DownloadProgressToast(imagesExpected, bytesExpected);
    client.getToastManager().add(toast);
    return toast;
  }

  @Override
  public Visibility draw(DrawContext context, ToastManager manager, long time) {
    context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());
    this.drawText(context, manager.getClient().textRenderer);
    this.drawProgressBar(context, time);

    double displayDuration = DURATION * manager.getNotificationDisplayTimeMultiplier();
    if (this.progress >= 1f && time - this.finishTime > displayDuration) {
      this.hide();
    }

    return this.visibility;
  }

  private void drawText(DrawContext context, TextRenderer textRenderer) {
    context.drawText(textRenderer, TITLE, TEXT_LEFT, TITLE_Y, Colors.YELLOW, false);
    context.drawText(textRenderer, this.description, TEXT_LEFT, DESCRIPTION_Y, Colors.YELLOW, false);
  }

  private void drawProgressBar(DrawContext context, long time) {
    float lerpedProgress = MathHelper.clampedLerp(this.lastProgress, this.progress, (time - this.lastTime) / 100f);
    int color = this.progress >= this.lastProgress ? BAR_COLOR_INCREASING : BAR_COLOR_DECREASING;

    context.fill(BAR_LEFT, BAR_TOP, BAR_RIGHT, BAR_BOTTOM, Colors.WHITE);
    context.fill(BAR_LEFT, BAR_TOP, BAR_LEFT + (int) (BAR_WIDTH * lerpedProgress), BAR_BOTTOM, color);

    this.lastProgress = lerpedProgress;
    this.lastTime = time;
  }

  public void setReceived(int images, int bytes) {
    this.imagesReceived = images;
    this.description = this.getDescription();
    this.progress = (float) bytes / this.bytesExpected;

    if (bytes >= this.bytesExpected) {
      this.finishTime = this.lastTime;
    }
  }

  public void hide() {
    this.visibility = Visibility.HIDE;
  }

  private Text getDescription() {
    // TODO: i18n
    return Text.of(String.format("%s/%s images (%s%%)", this.imagesReceived, this.imagesExpected,
        Math.clamp(Math.round(100f * this.progress), 0, 100)
    ));
  }
}
