package me.roundaround.custompaintings.mixin;

import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public interface SpriteLoaderAccessor {
  @Invoker("stitch")
  SpriteLoader.StitchResult invokeStitch(List<SpriteContents> sprites, int mipLevel, Executor executor);
}
