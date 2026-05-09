package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Transparency;
import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedQuad.MaterialInfo.class)
public abstract class BlockModelWrapperMixin {
  @ModifyReturnValue(
      method = "of", at = @At("RETURN")
  )
  private static BakedQuad.MaterialInfo useCustomPaintingsAtlas(
      BakedQuad.MaterialInfo original,
      final Material.Baked material,
      final Transparency transparency,
      final int tintIndex,
      final boolean shade,
      final int lightEmission
  ) {
    TextureAtlasSprite sprite = material.sprite();
    Identifier atlasId = sprite.atlasLocation();
    if (!ItemManager.PAINTING_ITEM_TEXTURE_ID.equals(atlasId)) {
      return original;
    }

    RenderType itemRenderType = transparency.hasTranslucent() ?
        RenderTypes.entityTranslucentCullItemTarget(atlasId) :
        RenderTypes.entityCutoutCull(atlasId);
    return new BakedQuad.MaterialInfo(
        sprite,
        ChunkSectionLayer.byTransparency(transparency),
        itemRenderType,
        tintIndex,
        shade,
        lightEmission
    );
  }
}
