package me.roundaround.custompaintings.network;

import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ServerNetworking {
  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.SET_PAINTING_PACKET,
        ServerNetworking::handleSetPaintingPacket);
    ServerPlayNetworking.registerGlobalReceiver(
        NetworkPackets.DECLARE_CUSTOM_PAINTING_USER_PACKET,
        ServerNetworking::handleDeclareCustomPaintingUserPacket);
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player,
      UUID paintingUuid,
      BlockPos pos,
      Direction facing) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    buf.writeBlockPos(pos);
    buf.writeInt(facing.getId());
    ServerPlayNetworking.send(player, NetworkPackets.EDIT_PAINTING_PACKET, buf);
  }

  private static void handleSetPaintingPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    UUID paintingUuid = buf.readUuid();
    PaintingData paintingData = PaintingData.fromPacketByteBuf(buf);

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
      painting.setCustomData(PaintingData.EMPTY);
    } else {
      painting.setCustomData(paintingData);
    }

    if (!basePainting.canStayAttached()) {
      basePainting.damage(DamageSource.player(player), 0f);
    }
  }

  public static void handleDeclareCustomPaintingUserPacket(
      MinecraftServer server,
      ServerPlayerEntity player,
      ServerPlayNetworkHandler handler,
      PacketByteBuf buf,
      PacketSender responseSender) {
    CustomPaintingsMod.playersUsingMod.add(player.getUuid());
  }
}
