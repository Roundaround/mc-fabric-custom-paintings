package me.roundaround.custompaintings.client.network;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.GroupSelectScreen;
import me.roundaround.custompaintings.client.gui.screen.edit.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.edit.PaintingSelectScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.ManagePaintingsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.OutdatedPaintingsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.UnknownPaintingsScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.NetworkPackets;
import me.roundaround.custompaintings.util.OutdatedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ClientNetworking {
  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.EDIT_PAINTING_PACKET,
        ClientNetworking::handleEditPaintingPacket);
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.OPEN_MANAGE_SCREEN_PACKET,
        ClientNetworking::handleOpenManageScreenPacket);
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.RESPOND_OUTDATED_PACKET,
        ClientNetworking::handleResponseOutdatedPacket);
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.RESPOND_UNKNOWN_PACKET,
        ClientNetworking::handleResponseUnknownPacket);
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
        NetworkPackets.DECLARE_KNOWN_PAINTINGS_PACKET, buf);
  }

  public static void sendRequestUnknownPacket() {
    ClientPlayNetworking.send(
        NetworkPackets.REQUEST_UNKNOWN_PACKET, new PacketByteBuf(Unpooled.buffer()));
  }

  public static void sendRequestOutdatedPacket() {
    ClientPlayNetworking.send(
        NetworkPackets.REQUEST_OUTDATED_PACKET, new PacketByteBuf(Unpooled.buffer()));
  }

  public static void sendReassignIdPacket(Identifier oldId, Identifier newId) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeIdentifier(oldId);
    buf.writeIdentifier(newId);
    buf.writeBoolean(true);
    ClientPlayNetworking.send(NetworkPackets.REASSIGN_ID_PACKET, buf);
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

  private static void handleOpenManageScreenPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buffer,
      PacketSender responseSender) {
    client.execute(() -> {
      client.setScreen(new ManagePaintingsScreen());
    });
  }

  private static void handleResponseOutdatedPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buffer,
      PacketSender responseSender) {
    int size = buffer.readInt();
    HashSet<OutdatedPainting> outdatedPaintings = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      UUID uuid = buffer.readUuid();
      PaintingData currentData = PaintingData.fromPacketByteBuf(buffer);
      PaintingData knownData = PaintingData.fromPacketByteBuf(buffer);
      outdatedPaintings.add(new OutdatedPainting(uuid, currentData, knownData));
    }

    client.execute(() -> {
      if (!(client.currentScreen instanceof OutdatedPaintingsScreen)) {
        return;
      }
      OutdatedPaintingsScreen screen = (OutdatedPaintingsScreen) client.currentScreen;
      screen.setOutdatedPaintings(outdatedPaintings);
    });
  }

  private static void handleResponseUnknownPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buffer,
      PacketSender responseSender) {
    int size = buffer.readInt();
    HashSet<UnknownPainting> unknownPaintings = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      Identifier id = buffer.readIdentifier();
      int count = buffer.readInt();
      Identifier autoFixIdentifier = buffer.readIdentifier();
      unknownPaintings.add(new UnknownPainting(id, count, autoFixIdentifier));
    }

    client.execute(() -> {
      if (!(client.currentScreen instanceof UnknownPaintingsScreen)) {
        return;
      }
      UnknownPaintingsScreen screen = (UnknownPaintingsScreen) client.currentScreen;
      screen.setUnknownPaintings(unknownPaintings);
    });
  }
}
