package me.roundaround.custompaintings.util;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.UUID;

public record UnknownPainting(UUID uuid, PaintingEntity paintingRef, PaintingData currentData,
                              PaintingData suggestedData) {
  public static final PacketCodec<PacketByteBuf, UnknownPainting> PACKET_CODEC = PacketCodec.of(
      UnknownPainting::writeToPacketByteBuf, UnknownPainting::fromPacketByteBuf);

  public UnknownPainting(UUID uuid, PaintingData currentData, PaintingData suggestedData) {
    this(uuid, null, currentData, suggestedData);
  }

  public UnknownPainting(PaintingEntity paintingRef, PaintingData currentData, PaintingData suggestedData) {
    this(paintingRef.getUuid(), paintingRef, currentData, suggestedData);
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeUuid(this.uuid);
    this.currentData.writeToPacketByteBuf(buf);
    buf.writeBoolean(this.suggestedData != null);
    if (this.suggestedData != null) {
      this.suggestedData.writeToPacketByteBuf(buf);
    }
  }

  public static UnknownPainting fromPacketByteBuf(PacketByteBuf buf) {
    UUID uuid = buf.readUuid();
    PaintingData currentData = PaintingData.fromPacketByteBuf(buf);
    PaintingData suggestedData = null;
    if (buf.readBoolean()) {
      suggestedData = PaintingData.fromPacketByteBuf(buf);
    }
    return new UnknownPainting(uuid, currentData, suggestedData);
  }
}
