package me.roundaround.custompaintings.client.network;

import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.NetworkPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ClientNetworking {
  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.EDIT_PAINTING_PACKET,
        ClientNetworking::handleEditPaintingPacket);
  }

  public static void sendSetPaintingPacket(UUID paintingUuid, PaintingData customPaintingInfo) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    customPaintingInfo.writeToPacketByteBuf(buf);
    ClientPlayNetworking.send(NetworkPackets.SET_PAINTING_PACKET, buf);
  }

  public static void sendDeclareCustomPaintingUserPacket() {
    ClientPlayNetworking.send(
        NetworkPackets.DECLARE_CUSTOM_PAINTING_USER_PACKET,
        new PacketByteBuf(Unpooled.buffer()));
  }

  private static void handleEditPaintingPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buffer,
      PacketSender responseSender) {
    UUID paintingUuid = buffer.readUuid();
    BlockPos pos = buffer.readBlockPos();
    Direction facing = Direction.byId(buffer.readInt());

    client.execute(() -> {
      client.setScreen(new PaintingEditScreen(paintingUuid, pos, facing));
    });
  }
}
