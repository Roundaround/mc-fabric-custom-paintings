package me.roundaround.custompaintings.network;

import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SetPaintingPacket {
  private static final Identifier SET_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "set_painting_packet");

  public static void registerReceive() {
    ServerPlayNetworking.registerGlobalReceiver(SET_PAINTING_PACKET, (server, player, handler, buf, responseSender) -> {
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
    });
  }

  public static void sendToServer(UUID paintingUuid, PaintingData customPaintingInfo) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    customPaintingInfo.writeToPacketByteBuf(buf);
    ClientPlayNetworking.send(SET_PAINTING_PACKET, buf);
  }
}
