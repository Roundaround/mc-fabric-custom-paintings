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
import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.UnknownPaintingsScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.NetworkPackets;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
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
        NetworkPackets.LIST_UNKNOWN_PACKET,
        ClientNetworking::handleListUnknownPacket);
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkPackets.LIST_MISMATCHED_PACKET,
        ClientNetworking::handleListMismatchedPacket);
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

  public static void sendRequestMismatchedPacket() {
    ClientPlayNetworking.send(
        NetworkPackets.REQUEST_MISMATCHED_PACKET, new PacketByteBuf(Unpooled.buffer()));
  }

  public static void sendReassignIdPacket(UUID paintingUuid, Identifier id) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    buf.writeIdentifier(id);
    buf.writeBoolean(false);
    ClientPlayNetworking.send(NetworkPackets.REASSIGN_ID_PACKET, buf);
  }

  public static void sendReassignAllIdsPacket(Identifier from, Identifier to) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeIdentifier(from);
    buf.writeIdentifier(to);
    buf.writeBoolean(false);
    ClientPlayNetworking.send(NetworkPackets.REASSIGN_ALL_IDS_PACKET, buf);
  }

  public static void sendUpdatePaintingPacket(UUID paintingUuid) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    ClientPlayNetworking.send(NetworkPackets.UPDATE_PAINTING_PACKET, buf);
  }

  public static void sendRemovePaintingPacket(UUID paintingUuid) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    ClientPlayNetworking.send(NetworkPackets.REMOVE_PAINTING_PACKET, buf);
  }

  public static void sendRemoveAllPaintingsPacket(Identifier id) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeIdentifier(id);
    ClientPlayNetworking.send(NetworkPackets.REMOVE_ALL_PAINTINGS_PACKET, buf);
  }

  public static void sendApplyMigrationPacket(Migration migration) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeInt(migration.pairs().size());
    for (Pair<String, String> pair : migration.pairs()) {
      buf.writeString(pair.getLeft());
      buf.writeString(pair.getRight());
    }
    ClientPlayNetworking.send(NetworkPackets.APPLY_MIGRATION_PACKET, buf);
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

  private static void handleListUnknownPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    int size = buf.readInt();
    HashSet<UnknownPainting> unknownPaintings = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      UUID uuid = buf.readUuid();
      PaintingData currentData = PaintingData.fromPacketByteBuf(buf);
      PaintingData suggestedData = null;
      if (buf.readBoolean()) {
        suggestedData = PaintingData.fromPacketByteBuf(buf);
      }
      unknownPaintings.add(new UnknownPainting(
          uuid,
          currentData,
          suggestedData));
    }

    client.execute(() -> {
      if (!(client.currentScreen instanceof UnknownPaintingsScreen)) {
        return;
      }
      UnknownPaintingsScreen screen = (UnknownPaintingsScreen) client.currentScreen;
      screen.setUnknownPaintings(unknownPaintings);
    });
  }

  private static void handleListMismatchedPacket(
      MinecraftClient client,
      ClientPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    int size = buf.readInt();
    HashSet<MismatchedPainting> mismatched = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      UUID uuid = buf.readUuid();
      PaintingData currentData = PaintingData.fromPacketByteBuf(buf);
      PaintingData knownData = PaintingData.fromPacketByteBuf(buf);
      mismatched.add(new MismatchedPainting(
          uuid,
          currentData,
          knownData));
    }

    client.execute(() -> {
      if (!(client.currentScreen instanceof MismatchedPaintingsScreen)) {
        return;
      }
      MismatchedPaintingsScreen screen = (MismatchedPaintingsScreen) client.currentScreen;
      screen.setMismatchedPaintings(mismatched);
    });
  }
}
