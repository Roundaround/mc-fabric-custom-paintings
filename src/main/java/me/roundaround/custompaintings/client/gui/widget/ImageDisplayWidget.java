package me.roundaround.custompaintings.client.gui.widget;

import java.util.function.Function;

import me.roundaround.custompaintings.resource.file.Image;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ImageDisplayWidget extends ImageButtonWidget {
  public ImageDisplayWidget(
      Function<Image, Identifier> getTextureId,
      Image image) {
    this(getTextureId, image, true);
  }

  public ImageDisplayWidget(
      Function<Image, Identifier> getTextureId,
      Image image,
      boolean immediatelyCalculateBounds) {
    super(
        Text.empty(),
        (button) -> {
        },
        getTextureId,
        image,
        immediatelyCalculateBounds);
    this.active = false;
  }
}
