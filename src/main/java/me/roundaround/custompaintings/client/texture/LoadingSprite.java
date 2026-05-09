package me.roundaround.custompaintings.client.texture;

import me.roundaround.roundalib.client.gui.util.GuiUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class LoadingSprite {
  private static final int COLOR_1 = GuiUtil.genColorInt(0.2f, 0.2f, 0.3f);
  private static final int COLOR_2 = GuiUtil.genColorInt(0.3f, 0.3f, 0.4f);
  private static final int BORDER_COLOR = GuiUtil.genColorInt(0.1f, 0.1f, 0.1f);

  public static SpriteContents generate(Identifier id, int width, int height) {
    NativeImage nativeImage = createImage(width, height);
    AnimationMetadataSection metadata = generateAnimationMetadata(width, height);
    return new SpriteContents(
        id,
        new FrameSize(width, height),
        nativeImage,
        Optional.of(metadata),
        List.of(),
        Optional.empty()
    );
  }

  private static NativeImage createImage(int width, int height) {
    NativeImage nativeImage = new NativeImage(width, 2 * height, false);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < 2 * height; y++) {
        if (isAlongBorder(x, y, width, height)) {
          nativeImage.setPixel(x, y, BORDER_COLOR);
        } else {
          nativeImage.setPixel(x, y, y < height ? COLOR_1 : COLOR_2);
        }
      }
    }
    return nativeImage;
  }

  private static boolean isAlongBorder(int x, int y, int width, int height) {
    return x < 1 || (y % height) < 1 || x >= width - 1 || (y % height) >= height - 1;
  }

  private static AnimationMetadataSection generateAnimationMetadata(int width, int height) {
    return new AnimationMetadataSection(
        Optional.of(List.of(new AnimationFrame(0), new AnimationFrame(1))),
        Optional.of(width),
        Optional.of(height),
        60,
        true
    );
  }
}
