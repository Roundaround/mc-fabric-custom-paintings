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
import me.roundaround.custompaintings.util.MismatchedPainting;
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
        NetworkPackets.LIST_OUTDATED_PAINTINGS_PACKET,
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

  public static void sendUpdatePaintingPacket(UUID paintingUuid) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    ClientPlayNetworking.send(NetworkPackets.UPDATE_PAINTING_PACKET, buf);
  }

  private static void handleEditPaintingPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();
    int paintingId = buf.readInt();
    BlockPos pos = buf.readBlockPos();
    Direction facing = Direction.byId(buf.readInt());

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
      PacketByteBuf buf,
      PacketSender responseSender) {
    client.execute(() -> {
      client.setScreen(new ManagePaintingsScreen());
    });
  }

  private static void handleResponseOutdatedPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    int size = buf.readInt();
    HashSet<MismatchedPainting> outdatedPaintings = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      UUID uuid = buf.readUuid();
      PaintingData currentData = PaintingData.fromPacketByteBuf(buf);
      PaintingData knownData = PaintingData.fromPacketByteBuf(buf);
      outdatedPaintings.add(new MismatchedPainting(uuid, currentData, knownData));
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
      PacketByteBuf buf,
      PacketSender responseSender) {
    int size = buf.readInt();
    HashSet<UnknownPainting> unknownPaintings = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      Identifier id = buf.readIdentifier();
      int count = buf.readInt();
      Identifier autoFixIdentifier = null;
      if (buf.readBoolean()) {
        autoFixIdentifier = buf.readIdentifier();
      }
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
