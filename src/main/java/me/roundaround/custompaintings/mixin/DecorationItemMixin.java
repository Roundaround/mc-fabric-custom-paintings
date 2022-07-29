package me.roundaround.custompaintings.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.network.EditPaintingPacket;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariants;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

@Mixin(DecorationItem.class)
public abstract class DecorationItemMixin {
  @Redirect(method = "useOnBlock", at = @At(value = "INVOKE", target = "net/minecraft/entity/decoration/painting/PaintingEntity.placePainting(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Ljava/util/Optional;"))
  private Optional<PaintingEntity> placePainting(
      World world,
      BlockPos pos,
      Direction facing,
      ItemUsageContext context) {
    if (!(context.getPlayer() instanceof ServerPlayerEntity)) {
      return PaintingEntity.placePainting(world, pos, facing);
    }

    PaintingEntity entity = new PaintingEntity(world, pos, facing,
        Registry.PAINTING_VARIANT.getEntry(PaintingVariants.KEBAB).get());

    if (!entity.canStayAttached()) {
      return Optional.empty();
    }

    ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
    ExpandedPaintingEntity painting = ((ExpandedPaintingEntity) entity);
    painting.setEditor(player.getUuid());

    EditPaintingPacket.sendToPlayer(player, entity.getUuid(), pos, facing);

    return Optional.of(entity);
  }
}
