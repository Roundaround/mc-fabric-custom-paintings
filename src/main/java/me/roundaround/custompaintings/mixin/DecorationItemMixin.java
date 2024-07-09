package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.decoration.painting.PaintingVariants;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(DecorationItem.class)
public abstract class DecorationItemMixin {
  @WrapOperation(
      method = "useOnBlock", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;placePainting" +
          "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)" +
          "Ljava/util/Optional;"
  )
  )
  private Optional<PaintingEntity> wrapPlacePainting(
      World world,
      BlockPos pos,
      Direction facing,
      Operation<Optional<PaintingEntity>> original,
      @Local(argsOnly = true) ItemUsageContext context
  ) {
    if (!(context.getPlayer() instanceof ServerPlayerEntity player) ||
        !ServerPlayNetworking.canSend(player, Networking.EditPaintingS2C.ID)) {
      return original.call(world, pos, facing);
    }

    Optional<RegistryEntry.Reference<PaintingVariant>> placeholderVariant = Registries.PAINTING_VARIANT.getEntry(
        PaintingVariants.KEBAB);
    if (placeholderVariant.isEmpty()) {
      return Optional.empty();
    }

    PaintingEntity painting = new PaintingEntity(world, pos, facing, placeholderVariant.get());

    if (!painting.canStayAttached()) {
      return Optional.empty();
    }

    painting.setEditor(player.getUuid());

    ServerNetworking.sendEditPaintingPacket(player, painting.getUuid(), painting.getId(), pos, facing);

    return Optional.of(painting);
  }
}
