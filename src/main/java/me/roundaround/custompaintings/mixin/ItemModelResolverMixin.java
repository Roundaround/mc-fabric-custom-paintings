package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
  @ModifyExpressionValue(
      method = "appendItemLayers", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)" +
               "Ljava/lang/Object;"
  )
  )
  private Object swapModelId(
      Object original,
      final ItemStackRenderState output,
      final ItemStack item,
      final ItemDisplayContext displayContext,
      @Nullable final Level level,
      @Nullable final ItemOwner owner,
      final int seed
  ) {
    if (!item.is(Items.PAINTING)) {
      return original;
    }

    CustomId paintingId = readPaintingId(item);
    if (paintingId == null) {
      return original;
    }

    PaintingData data = ClientPaintingRegistry.getInstance().get(paintingId);
    if (data == null || data.isEmpty()) {
      return original;
    }

    CustomPaintingsConfig config = CustomPaintingsConfig.getInstance();
    boolean enabled = data.vanilla() ?
        config.renderVanillaArtworkOnItems.getPendingValue() :
        config.renderArtworkOnItems.getPendingValue();
    if (!enabled) {
      return original;
    }

    return ItemManager.getItemModelId(data.id());
  }

  @Unique
  private static CustomId readPaintingId(ItemStack stack) {
    CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    String customNbtId = nbt.isEmpty() ? null : nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (customNbtId != null) {
      return CustomId.parse(customNbtId);
    }

    Holder<PaintingVariant> variantEntry = stack.get(DataComponents.PAINTING_VARIANT);
    if (variantEntry == null) {
      return null;
    }
    return CustomId.from(variantEntry.value().assetId());
  }
}
