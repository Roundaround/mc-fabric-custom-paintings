package me.roundaround.custompaintings.client.toast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.text.DecimalFormat;

public class DownloadProgressToast implements Toast {
  private static final Identifier TEXTURE = new Identifier(Identifier.DEFAULT_NAMESPACE, "toast/system");
  // TODO: i18n
  private static final Text TITLE = Text.of("Downloading images");

  private final int imagesExpected;
  private final int bytesExpected;

  private Text description;
  private int imagesReceived = 0;
  private Visibility visibility = Visibility.SHOW;
  private long lastTime;
  private float lastProgress;
  private float progress;

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
  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());
    context.drawText(manager.getClient().textRenderer, TITLE, 18, 7, Colors.YELLOW, false);
    context.drawText(manager.getClient().textRenderer, this.description, 18, 18, Colors.YELLOW, false);
    this.drawProgressBar(context, startTime);

    return this.visibility;
  }

  private void drawProgressBar(DrawContext context, long startTime) {
    context.fill(3, 28, 157, 29, -1);
    float f = MathHelper.clampedLerp(this.lastProgress, this.progress, (float) (startTime - this.lastTime) / 100.0F);
    int i;
    if (this.progress >= this.lastProgress) {
      i = -16755456;
    } else {
      i = -11206656;
    }

    context.fill(3, 28, (int) (3.0F + 154.0F * f), 29, i);
    this.lastProgress = f;
    this.lastTime = startTime;
  }

  public void setReceived(int images, int bytes) {
    this.imagesReceived = images;
    this.description = this.getDescription();
    this.progress = (float) bytes / this.bytesExpected;

    // TODO: After download finishes, keep around for another few seconds
    if (bytes >= this.bytesExpected) {
      this.hide();
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
