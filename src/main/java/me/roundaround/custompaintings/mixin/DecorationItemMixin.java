package me.roundaround.custompaintings.mixin;

import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.decoration.painting.PaintingVariants;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(DecorationItem.class)
public abstract class DecorationItemMixin {
  @WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;placePainting"
      +
      "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)" +
      "Ljava/util/Optional;"))
  private Optional<PaintingEntity> wrapPlacePainting(
      World world,
      BlockPos pos,
      Direction facing,
      Operation<Optional<PaintingEntity>> original,
      @Local(argsOnly = true) ItemUsageContext context,
      @Local ItemStack stack) {
    if (!(context.getPlayer() instanceof ServerPlayerEntity player) ||
        !ServerPlayNetworking.canSend(player, Networking.EditPaintingS2C.ID)) {
      return original.call(world, pos, facing);
    }

    Optional<RegistryEntry.Reference<PaintingVariant>> placeholderVariant = world.getRegistryManager()
        .getOrThrow(RegistryKeys.PAINTING_VARIANT)
        .getOptional(PaintingVariants.KEBAB);
    if (placeholderVariant.isEmpty()) {
      return Optional.empty();
    }

    PaintingEntity painting = new PaintingEntity(world, pos, facing, placeholderVariant.get());

    PaintingData data = getStackPainting(stack);
    if (data != null && !data.isEmpty()) {
      painting.custompaintings$setData(data);
      return Optional.of(painting);
    }

    if (!painting.canStayAttached()) {
      return Optional.empty();
    }

    painting.custompaintings$setEditor(player.getUuid());

    ServerNetworking.sendEditPaintingPacket(player, painting.getUuid(), painting.getId(), pos, facing);

    return Optional.of(painting);
  }

  @Inject(method = "appendTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"), cancellable = true)
  private void appendCustomTooltip(
      ItemStack stack,
      Item.TooltipContext context,
      TooltipDisplayComponent displayComponent,
      Consumer<Text> textConsumer,
      TooltipType type,
      CallbackInfo ci) {
    PaintingData painting = getStackPainting(stack);
    if (painting == null || painting.isEmpty()) {
      return;
    }

    textConsumer.accept(painting.getTooltipNameText());
    painting.getTooltipArtistText().ifPresent(textConsumer);

    getStackPackName(stack).ifPresent((pack) -> {
      textConsumer.accept(Text.literal(pack).formatted(Formatting.AQUA));
    });

    textConsumer.accept(painting.getTooltipDimensionsText());
    ci.cancel();
  }

  @Unique
  private static PaintingData getStackPainting(ItemStack stack) {
    NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
    if (component == null || component.isEmpty()) {
      return null;
    }

    NbtCompound nbt = component.copyNbt();
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
    NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
    if (component == null || component.isEmpty()) {
      return null;
    }

    NbtCompound nbt = component.copyNbt();
    return nbt.getString(PaintingData.PACK_NBT_KEY);
  }
}
