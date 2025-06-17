package me.roundaround.custompaintings.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
  // Check if stack has custom painting data, skip model lookup and generate
  // custom/dynamic model?

  @Shadow
  @Final
  private Function<Identifier, ItemModel> modelGetter;

  @Shadow
  @Final
  private Function<Identifier, ItemAsset.Properties> propertiesGetter;

  @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"))
  private Object swapModelId(Object original, @Local(argsOnly = true) ItemStack stack) {
    if (!stack.isOf(Items.PAINTING)) {
      return original;
    }

    if (!CustomPaintingsConfig.getInstance().renderArtworkOnItems.getPendingValue()) {
      return original;
    }

    NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    if (nbt.isEmpty()) {
      return original;
    }

    String paintingId = nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (paintingId == null) {
      return original;
    }

    PaintingData data = ClientPaintingRegistry.getInstance().get(CustomId.parse(paintingId));
    if (data == null || data.isEmpty()) {
      return original;
    }

    return ItemManager.getItemModelId(data.id());
  }
}
