package me.roundaround.custompaintings.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ServerNetworking {
  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.SET_PAINTING_PACKET,
        ServerNetworking::handleSetPaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.DECLARE_KNOWN_PAINTINGS_PACKET,
        ServerNetworking::handleDeclareKnownPaintingsPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.REQUEST_UNKNOWN_PACKET,
        ServerNetworking::handleRequestUnknownPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.REQUEST_MISMATCHED_PACKET,
        ServerNetworking::handleRequestMismatchedPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.REASSIGN_ID_PACKET,
        ServerNetworking::handleReassignIdPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.UPDATE_PAINTING_PACKET,
        ServerNetworking::handleUpdatePaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.APPLY_MIGRATION_PACKET,
        ServerNetworking::handleApplyMigrationPacket);
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player,
      UUID paintingUuid,
      int paintingId,
      BlockPos pos,
      Direction facing) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    buf.writeInt(paintingId);
    buf.writeBlockPos(pos);
    buf.writeInt(facing.getId());
    ServerPlayNetworking.send(player, NetworkPackets.EDIT_PAINTING_PACKET, buf);
  }

  public static void sendOpenManageScreenPacket(ServerPlayerEntity player) {
    ServerPlayNetworking.send(
        player,
        NetworkPackets.OPEN_MANAGE_SCREEN_PACKET,
        new PacketByteBuf(Unpooled.buffer()));
  }

  private static void sendListUnknownPacket(
      ServerPlayerEntity player,
      HashSet<UnknownPainting> unknownPaintings) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeInt(unknownPaintings.size());
    for (UnknownPainting entry : unknownPaintings) {
      buf.writeUuid(entry.uuid());
      entry.currentData().writeToPacketByteBuf(buf);

      PaintingData suggestedData = entry.suggestedData();
      buf.writeBoolean(suggestedData != null);
      if (suggestedData != null) {
        suggestedData.writeToPacketByteBuf(buf);
      }
    }
    ServerPlayNetworking.send(player, NetworkPackets.LIST_UNKNOWN_PACKET, buf);
  }

  private static void sendListMismatchedPacket(
      ServerPlayerEntity player,
      HashSet<MismatchedPainting> mismatched) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeInt(mismatched.size());
    for (MismatchedPainting entry : mismatched) {
      buf.writeUuid(entry.uuid());
      entry.currentData().writeToPacketByteBuf(buf);
      entry.knownData().writeToPacketByteBuf(buf);
    }
    ServerPlayNetworking.send(player, NetworkPackets.LIST_MISMATCHED_PACKET, buf);
  }

  private static void handleSetPaintingPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();
    PaintingData paintingData = PaintingData.fromPacketByteBuf(buf);

    server.execute(() -> {
      Entity entity = player.getWorld().getEntity(paintingUuid);
      if (entity == null || !(entity instanceof PaintingEntity)) {
        return;
      }

      ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
      if (painting.getEditor() == null || !painting.getEditor().equals(player.getUuid())) {
        return;
      }

      PaintingEntity basePainting = (PaintingEntity) entity;
      if (paintingData.isEmpty()) {
        entity.damage(DamageSource.player(player), 0f);
        return;
      }

      if (paintingData.isVanilla()) {
        painting.setVariant(paintingData.id());
      }

      painting.setCustomData(paintingData);

      if (!basePainting.canStayAttached()) {
        basePainting.damage(DamageSource.player(player), 0f);
      }
    });
  }

  private static void handleDeclareKnownPaintingsPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    CustomPaintingsMod.knownPaintings.put(player.getUuid(), new HashSet<>());

    int size = buf.readInt();
    for (int i = 0; i < size; i++) {
      CustomPaintingsMod.knownPaintings.get(player.getUuid()).add(PaintingData.fromPacketByteBuf(buf));
    }
  }

  private static void handleRequestUnknownPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    server.execute(() -> {
      sendListUnknownPacket(player, ServerPaintingManager.getUnknownPaintings(player));
    });
  }

  private static void handleRequestMismatchedPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    server.execute(() -> {
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
    });
  }

  private static void handleReassignIdPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();
    Identifier to = buf.readIdentifier();
    boolean fix = buf.readBoolean();

    server.execute(() -> {
      ServerPaintingManager.reassignId(player, paintingUuid, to, fix);
    });
  }

  private static void handleUpdatePaintingPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();

    server.execute(() -> {
      ServerPaintingManager.updatePainting(player, paintingUuid);
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
    });
  }

  private static void handleApplyMigrationPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    String id = buf.readString();
    String packId = buf.readString();
    int index = buf.readInt();
    int size = buf.readInt();
    List<Pair<String, String>> pairs = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      pairs.add(new Pair<>(buf.readString(), buf.readString()));
    }

    Migration migration = new Migration(id, packId, index, pairs);

    server.execute(() -> {
      ServerPaintingManager.applyMigration(player, migration);
    });
  }
}
