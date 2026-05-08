package me.roundaround.custompaintings.mixin;

import java.util.List;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.roundaround.custompaintings.client.registry.ItemManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Mixin(BasicItemModel.class)
public abstract class BasicItemModelMixin {
  @Unique
  private static final Function<ItemStack, RenderLayer> CUSTOM_PAINTINGS_RENDER_LAYER_GETTER =
      (stack) -> RenderLayers.itemEntityTranslucentCull(ItemManager.PAINTING_ITEM_TEXTURE_ID);

  @Inject(method = "findRenderLayerGetter", at = @At("HEAD"), cancellable = true)
  private static void useCustomPaintingsAtlasGetter(
      List<BakedQuad> quads,
      CallbackInfoReturnable<Function<ItemStack, RenderLayer>> cir) {
    if (quads.isEmpty()) {
      return;
    }
    Identifier atlasId = quads.iterator().next().sprite().getAtlasId();
    if (ItemManager.PAINTING_ITEM_TEXTURE_ID.equals(atlasId)) {
      cir.setReturnValue(CUSTOM_PAINTINGS_RENDER_LAYER_GETTER);
    }
  }
}
