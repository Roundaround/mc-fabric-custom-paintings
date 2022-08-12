package me.roundaround.custompaintings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
  @Inject(method = "onPlayerConnect", at = @At(value = "HEAD"))
  public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
    CustomPaintingsMod.playersUsingMod.remove(player.getUuid());
  }
}
