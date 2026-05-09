package me.roundaround.custompaintings.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.resources.Identifier;

import java.io.InputStream;

@Environment(EnvType.CLIENT)
public class VanillaIconSprite {
  public static SpriteContents create(Minecraft client, Identifier spriteId, String profileId) {
    Pack profile = client.getResourcePackRepository().getPack(profileId);
    if (profile == null) {
      return MissingTextureAtlasSprite.create();
    }

    try (PackResources resourcePack = profile.open()) {
      IoSupplier<InputStream> inputSupplier = resourcePack.getRootResource("pack.png");
      if (inputSupplier == null) {
        return MissingTextureAtlasSprite.create();
      }

      try (InputStream inputStream = inputSupplier.get()) {
        NativeImage nativeImage = NativeImage.read(inputStream);
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        return new SpriteContents(spriteId, new FrameSize(width, height), nativeImage);
      }
    } catch (Exception e) {
      return MissingTextureAtlasSprite.create();
    }
  }
}
