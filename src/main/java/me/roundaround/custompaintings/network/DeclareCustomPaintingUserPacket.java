package me.roundaround.custompaintings.network;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class DeclareCustomPaintingUserPacket {
  private static final Identifier DECLARE_CUSTOM_PAINTING_USER_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "declare_custom_painting_user");

  public static void registerReceive() {
    ServerPlayNetworking.registerGlobalReceiver(
        DECLARE_CUSTOM_PAINTING_USER_PACKET,
        (server, player, handler, buf, responseSender) -> {
          CustomPaintingsMod.playersUsingMod.add(player.getUuid());
        });
  }

  public static void sendToServer() {
    ClientPlayNetworking.send(
        DECLARE_CUSTOM_PAINTING_USER_PACKET,
        new PacketByteBuf(Unpooled.buffer()));
  }
}
