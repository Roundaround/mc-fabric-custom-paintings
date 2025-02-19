package me.roundaround.custompaintings.client.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.texture.SpriteOpener;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;

@Environment(EnvType.CLIENT)
public class BasicTextureSprite {
  public static SpriteContents fetch(MinecraftClient client, Identifier spriteId, Identifier textureId) {
    try {
      SpriteOpener opener = SpriteOpener.create(SpriteLoader.METADATA_SERIALIZERS);
      return opener.loadSprite(spriteId, client.getResourceManager().getResourceOrThrow(textureId));
    } catch (FileNotFoundException e) {
      return MissingSprite.createSpriteContents();
    }
  }
}
