package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.network.Networking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public abstract class ThreadedAnvilChunkStorageEntityTrackerMixin {
  @ModifyArg(
      method = "sendToOtherNearbyPlayers", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/server/network/PlayerAssociatedNetworkHandler;sendPacket" +
          "(Lnet/minecraft/network/packet/Packet;)V"
  )
  )
  private Packet<?> modifyPacketBeforeSending(Packet<?> original, @Local PlayerAssociatedNetworkHandler handler) {
    if (!(original instanceof EntityTrackerUpdateS2CPacket packet) ||
        ServerPlayNetworking.canSend(handler.getPlayer(), Networking.SummaryS2C.ID)) {
      return original;
    }
    return new EntityTrackerUpdateS2CPacket(packet.id(), packet.trackedValues()
        .stream()
        .filter((entry) -> !(entry.handler() == CustomPaintingsMod.CUSTOM_PAINTING_DATA_HANDLER))
        .toList());
  }
}
