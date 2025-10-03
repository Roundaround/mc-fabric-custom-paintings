package me.roundaround.custompaintings.client.texture;

import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class LoadingSprite {
  private static final int COLOR_1 = GuiUtil.genColorInt(0.2f, 0.2f, 0.3f);
  private static final int COLOR_2 = GuiUtil.genColorInt(0.3f, 0.3f, 0.4f);
  private static final int BORDER_COLOR = GuiUtil.genColorInt(0.1f, 0.1f, 0.1f);

  public static SpriteContents generate(Identifier id, int width, int height) {
    NativeImage nativeImage = createImage(width, height);
    AnimationResourceMetadata metadata = generateAnimationMetadata(width, height);
    return new SpriteContents(id, new SpriteDimensions(width, height), nativeImage, Optional.of(metadata), List.of());
  }

  private static NativeImage createImage(int width, int height) {
    NativeImage nativeImage = new NativeImage(width, 2 * height, false);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < 2 * height; y++) {
        if (isAlongBorder(x, y, width, height)) {
          nativeImage.setColorArgb(x, y, BORDER_COLOR);
        } else {
          nativeImage.setColorArgb(x, y, y < height ? COLOR_1 : COLOR_2);
        }
      }
    }
    return nativeImage;
  }

  private static boolean isAlongBorder(int x, int y, int width, int height) {
    return x < 1 || (y % height) < 1 || x >= width - 1 || (y % height) >= height - 1;
  }

  private static AnimationResourceMetadata generateAnimationMetadata(int width, int height) {
    return new AnimationResourceMetadata(
        Optional.of(List.of(new AnimationFrameResourceMetadata(0), new AnimationFrameResourceMetadata(1))),
        Optional.of(width),
        Optional.of(height),
        60,
        true
    );
  }
}
