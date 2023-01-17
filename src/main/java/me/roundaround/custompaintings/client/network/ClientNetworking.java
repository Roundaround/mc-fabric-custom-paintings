package me.roundaround.custompaintings.client.network;

import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.GroupSelectScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingSelectScreen;
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

  public static void sendDeclareKnownPaintingsPacket(List<PaintingData> knownPaintings) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeInt(knownPaintings.size());
    for (PaintingData data : knownPaintings) {
      data.writeToPacketByteBuf(buf);
    }
    ClientPlayNetworking.send(
        NetworkPackets.DECLARE_KNOWN_PAINTINGS, buf);
  }

  private static void handleEditPaintingPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buffer,
      PacketSender responseSender) {
    UUID paintingUuid = buffer.readUuid();
    int paintingId = buffer.readInt();
    BlockPos pos = buffer.readBlockPos();
    Direction facing = Direction.byId(buffer.readInt());

    client.execute(() -> {
      PaintingEditState state = new PaintingEditState(
          client,
          paintingUuid,
          paintingId,
          pos,
          facing);

      PaintingEditScreen screen = state.hasMultipleGroups()
          ? new GroupSelectScreen(state)
          : new PaintingSelectScreen(state);

      client.setScreen(screen);
    });
  }
}
