package me.roundaround.custompaintings.client.texture;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;

import java.io.InputStream;

@Environment(EnvType.CLIENT)
public class VanillaIconSprite {
  public static SpriteContents create(MinecraftClient client, Identifier spriteId, String profileId) {
    ResourcePackProfile profile = client.getResourcePackManager().getProfile(profileId);
    if (profile == null) {
      return MissingSprite.createSpriteContents();
    }

    try (ResourcePack resourcePack = profile.createResourcePack()) {
      InputSupplier<InputStream> inputSupplier = resourcePack.openRoot("pack.png");
      if (inputSupplier == null) {
        return MissingSprite.createSpriteContents();
      }

      try (InputStream inputStream = inputSupplier.get()) {
        NativeImage nativeImage = NativeImage.read(inputStream);
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        return new SpriteContents(spriteId, new SpriteDimensions(width, height), nativeImage,
            generateMetadata(width, height)
        );
      }
    } catch (Exception e) {
      return MissingSprite.createSpriteContents();
    }
  }

  private static ResourceMetadata generateMetadata(int width, int height) {
    return new ResourceMetadata.Builder().add(
        AnimationResourceMetadata.READER, generateAnimationMetadata(width, height)).build();
  }

  private static AnimationResourceMetadata generateAnimationMetadata(int width, int height) {
    return new AnimationResourceMetadata(
        ImmutableList.of(new AnimationFrameResourceMetadata(0, -1)), width, height, 1, false);
  }
}
