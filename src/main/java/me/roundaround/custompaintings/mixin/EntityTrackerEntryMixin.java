package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.network.Networking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
  @Shadow
  @Final
  private Entity entity;

  @ModifyArg(
      method = "sendPackets", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket;<init>(ILjava/util/List;)V"
  )
  )
  private List<DataTracker.SerializedEntry<?>> forInitialEntityDataSync(
      List<DataTracker.SerializedEntry<?>> original, @Local(argsOnly = true) ServerPlayerEntity player
  ) {
    if (!(this.entity instanceof PaintingEntity) || ServerPlayNetworking.canSend(player, Networking.SummaryS2C.ID)) {
      return original;
    }
    return original.stream()
        .filter((entry) -> entry.handler() != CustomPaintingsMod.CUSTOM_PAINTING_DATA_HANDLER)
        .toList();
  }
}
