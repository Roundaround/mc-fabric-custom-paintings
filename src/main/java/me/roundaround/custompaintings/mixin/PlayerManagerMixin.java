package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
  @Inject(method = "onPlayerConnect", at = @At(value = "HEAD"))
  public void onPlayerConnect(
      ClientConnection connection,
      ServerPlayerEntity player,
      ConnectedClientData clientData,
      CallbackInfo info) {
    CustomPaintingsMod.knownPaintings.remove(player.getUuid());

    ServerPaintingManager.customifyVanillaPaintings(player);
  }
}
