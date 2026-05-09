package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(HangingEntityItem.class)
public abstract class HangingEntityItemMixin {
  @WrapOperation(
      method = "useOn", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/entity/decoration/painting/Painting;create(Lnet/minecraft/world/level/Level;" +
               "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Ljava/util/Optional;"
  )
  )
  private Optional<Painting> wrapPaintingCreate(
      Level level,
      BlockPos pos,
      Direction direction,
      Operation<Optional<Painting>> original,
      UseOnContext context
  ) {
    if (!(context.getPlayer() instanceof ServerPlayer player) ||
        !ServerPlayNetworking.canSend(player, Networking.EditPaintingS2C.ID)) {
      return original.call(level, pos, direction);
    }

    Optional<Holder.Reference<PaintingVariant>> placeholderVariant = level.registryAccess()
        .lookupOrThrow(Registries.PAINTING_VARIANT)
        .get(PaintingVariants.KEBAB);
    if (placeholderVariant.isEmpty()) {
      return Optional.empty();
    }

    Painting painting = new Painting(level, pos, direction, placeholderVariant.get());

    PaintingData data = getStackPainting(context.getItemInHand());
    if (data == null) {
      Holder<PaintingVariant> variantEntry = context.getItemInHand().get(DataComponents.PAINTING_VARIANT);
      if (variantEntry != null) {
        data = ClientPaintingRegistry.getInstance().get(CustomId.from(variantEntry.value().assetId()));
      }
    }
    if (data != null && !data.isEmpty()) {
      if (data.vanilla()) {
        painting.custompaintings$setVariant(data.id());
      }
      painting.custompaintings$setData(data);
      return Optional.of(painting);
    }

    if (!painting.survives()) {
      return Optional.empty();
    }

    painting.custompaintings$setEditor(player.getUUID());

    ServerNetworking.sendEditPaintingPacket(player, painting.getUUID(), painting.getId(), pos, direction);

    return Optional.of(painting);
  }

  @Inject(
      method = "appendHoverText", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)" +
               "Ljava/lang/Object;"
  ), cancellable = true
  )
  private void appendCustomTooltip(
      ItemStack itemStack,
      Item.TooltipContext context,
      TooltipDisplay display,
      Consumer<Component> builder,
      TooltipFlag tooltipFlag,
      CallbackInfo ci
  ) {
    PaintingData painting = getStackPainting(itemStack);
    if (painting == null || painting.isEmpty()) {
      return;
    }

    builder.accept(painting.getTooltipNameText());
    painting.getTooltipArtistText().ifPresent(builder);

    getStackPackName(itemStack).ifPresent((pack) -> {
      builder.accept(Component.literal(pack).withStyle(ChatFormatting.AQUA));
    });

    builder.accept(painting.getTooltipDimensionsText());
    ci.cancel();
  }

  @Unique
  private static PaintingData getStackPainting(ItemStack stack) {
    CustomData component = stack.get(DataComponents.CUSTOM_DATA);
    if (component == null || component.isEmpty()) {
      return null;
    }

    CompoundTag nbt = component.copyTag();
    String id = nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (id == null || id.isEmpty()) {
      return null;
    }

    PaintingData painting = ClientPaintingRegistry.getInstance().get(CustomId.parse(id));
    if (painting == null || painting.isEmpty()) {
      return null;
    }

    return painting;
  }

  @Unique
  private static Optional<String> getStackPackName(ItemStack stack) {
    CustomData component = stack.get(DataComponents.CUSTOM_DATA);
    if (component == null || component.isEmpty()) {
      return Optional.empty();
    }

    CompoundTag nbt = component.copyTag();
    return nbt.getString(PaintingData.PACK_NBT_KEY);
  }
}
