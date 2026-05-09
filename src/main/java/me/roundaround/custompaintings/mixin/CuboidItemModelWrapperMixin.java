package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CuboidItemModelWrapper.class)
public abstract class CuboidItemModelWrapperMixin {
  @Inject(method = "validateAtlasUsage", at = @At("HEAD"), cancellable = true)
  private static void skipValidationForCustomPaintingsAtlas(List<BakedQuad> quads, CallbackInfo ci) {
    if (quads.isEmpty()) {
      return;
    }
    Identifier atlasId = quads.iterator().next().materialInfo().sprite().atlasLocation();
    if (ItemManager.PAINTING_ITEM_TEXTURE_ID.equals(atlasId)) {
      ci.cancel();
    }
  }
}
