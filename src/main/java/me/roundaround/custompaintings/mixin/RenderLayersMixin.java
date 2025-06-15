package me.roundaround.custompaintings.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

@Mixin(RenderLayers.class)
public abstract class RenderLayersMixin {
  @Unique
  private static final RenderLayer CUSTOM_ITEM_LAYER = RenderLayer
      .getItemEntityTranslucentCull(ClientPaintingRegistry.CUSTOM_PAINTING_TEXTURE_ID);

  @WrapMethod(method = "getItemLayer")
  private static RenderLayer wrapGetItemLayer(ItemStack stack, Operation<RenderLayer> original) {
    Supplier<RenderLayer> callOriginal = () -> original.call(stack);

    if (!stack.isOf(Items.PAINTING)) {
      return callOriginal.get();
    }

    if (!CustomPaintingsConfig.getInstance().renderArtworkOnItems.getPendingValue()) {
      return callOriginal.get();
    }

    NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    if (nbt.isEmpty()) {
      return callOriginal.get();
    }

    String paintingId = nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (paintingId == null) {
      return callOriginal.get();
    }

    PaintingData data = ClientPaintingRegistry.getInstance().get(CustomId.parse(paintingId));
    if (data == null || data.isEmpty()) {
      return callOriginal.get();
    }

    return CUSTOM_ITEM_LAYER;
  }
}
