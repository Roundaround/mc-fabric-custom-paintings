package me.roundaround.custompaintings.server.network;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.NetworkPackets;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ServerNetworking {
  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.SET_PAINTING_PACKET,
        ServerNetworking::handleSetPaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.DECLARE_KNOWN_PAINTINGS_PACKET,
        ServerNetworking::handleDeclareKnownPaintingsPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REQUEST_UNKNOWN_PACKET,
        ServerNetworking::handleRequestUnknownPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REQUEST_MISMATCHED_PACKET,
        ServerNetworking::handleRequestMismatchedPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REASSIGN_PACKET,
        ServerNetworking::handleReassignPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REASSIGN_ALL_PACKET,
        ServerNetworking::handleReassignAllPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.UPDATE_PAINTING_PACKET,
        ServerNetworking::handleUpdatePaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REMOVE_PAINTING_PACKET,
        ServerNetworking::handleRemovePaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.REMOVE_ALL_PAINTINGS_PACKET,
        ServerNetworking::handleRemoveAllPaintingsPacket);
    ServerPlayNetworking.registerGlobalReceiver(NetworkPackets.APPLY_MIGRATION_PACKET,
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
    ServerPlayNetworking.send(player,
        NetworkPackets.OPEN_MANAGE_SCREEN_PACKET,
        new PacketByteBuf(Unpooled.buffer()));
  }

  private static void sendListUnknownPacket(
      ServerPlayerEntity player, HashSet<UnknownPainting> unknownPaintings) {
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
      ServerPlayerEntity player, HashSet<MismatchedPainting> mismatched) {
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
      Entity entity = player.getServerWorld().getEntity(paintingUuid);
      if (entity == null || !(entity instanceof PaintingEntity)) {
        return;
      }

      ExpandedPaintingEntity painting = (ExpandedPaintingEntity) entity;
      if (painting.getEditor() == null || !painting.getEditor().equals(player.getUuid())) {
        return;
      }

      PaintingEntity basePainting = (PaintingEntity) entity;
      if (paintingData.isEmpty()) {
        entity.damage(player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      if (paintingData.isVanilla()) {
        painting.setVariant(paintingData.id());
      }

      painting.setCustomData(paintingData);

      if (!basePainting.canStayAttached()) {
        basePainting.damage(player.getDamageSources().playerAttack(player), 0f);
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
      CustomPaintingsMod.knownPaintings.get(player.getUuid())
          .add(PaintingData.fromPacketByteBuf(buf));
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

  private static void handleReassignPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();
    Identifier id = buf.readIdentifier();

    server.execute(() -> {
      ServerPaintingManager.reassign(player, paintingUuid, id);
      sendListUnknownPacket(player, ServerPaintingManager.getUnknownPaintings(player));
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
    });
  }

  private static void handleReassignAllPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    Identifier from = buf.readIdentifier();
    Identifier to = buf.readIdentifier();

    server.execute(() -> {
      ServerPaintingManager.reassign(player, from, to);
      sendListUnknownPacket(player, ServerPaintingManager.getUnknownPaintings(player));
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
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

  private static void handleRemovePaintingPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();

    server.execute(() -> {
      ServerPaintingManager.removePainting(player, paintingUuid);
      sendListUnknownPacket(player, ServerPaintingManager.getUnknownPaintings(player));
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
    });
  }

  private static void handleRemoveAllPaintingsPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    Identifier id = buf.readIdentifier();

    server.execute(() -> {
      ServerPaintingManager.removePaintings(player, id);
      sendListUnknownPacket(player, ServerPaintingManager.getUnknownPaintings(player));
      sendListMismatchedPacket(player, ServerPaintingManager.getMismatchedPaintings(player));
    });
  }

  private static void handleApplyMigrationPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    int size = buf.readInt();
    List<Pair<String, String>> pairs = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      pairs.add(new Pair<>(buf.readString(), buf.readString()));
    }

    server.execute(() -> {
      int updated = ServerPaintingManager.applyMigration(player, new Migration(pairs));
      if (updated == 0) {
        player.sendMessage(Text.translatable("custompaintings.migrations.none"), false);
      } else {
        player.sendMessage(Text.translatable("custompaintings.migrations.success", updated), false);
      }
    });
  }
}
