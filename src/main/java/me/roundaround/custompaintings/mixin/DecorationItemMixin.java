package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariants;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(DecorationItem.class)
public abstract class DecorationItemMixin {
  @Redirect(
      method = "useOnBlock", at = @At(
      value = "INVOKE",
      target = "net/minecraft/entity/decoration/painting/PaintingEntity.placePainting(Lnet/minecraft/world/World;" +
          "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Ljava/util/Optional;"
  )
  )
  private Optional<PaintingEntity> placePainting(
      World world, BlockPos pos, Direction facing, ItemUsageContext context
  ) {
    if (!(context.getPlayer() instanceof ServerPlayerEntity player) ||
        !CustomPaintingsMod.knownPaintings.containsKey(context.getPlayer().getUuid())) {
      return PaintingEntity.placePainting(world, pos, facing);
    }

    PaintingEntity painting = new PaintingEntity(world, pos, facing,
        Registries.PAINTING_VARIANT.getEntry(PaintingVariants.KEBAB).get()
    );

    if (!painting.canStayAttached()) {
      return Optional.empty();
    }

    painting.setEditor(player.getUuid());

    ServerNetworking.sendEditPaintingPacket(player, painting.getUuid(), painting.getId(), pos, facing);

    return Optional.of(painting);
  }
}
