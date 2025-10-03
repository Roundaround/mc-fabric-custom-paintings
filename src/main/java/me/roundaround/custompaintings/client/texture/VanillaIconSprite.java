package me.roundaround.custompaintings.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
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
        return new SpriteContents(spriteId, new SpriteDimensions(width, height), nativeImage);
      }
    } catch (Exception e) {
      return MissingSprite.createSpriteContents();
    }
  }
}
