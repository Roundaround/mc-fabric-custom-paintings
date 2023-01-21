package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.util.Identifier;

@Environment(value = EnvType.CLIENT)
@Mixin(SpriteAtlasHolder.class)
public interface SpriteAtlasHolderAccessor {
  @Invoker("getSprite")
  public Sprite invokeGetSprite(Identifier id);
}
