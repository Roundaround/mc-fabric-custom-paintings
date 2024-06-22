package me.roundaround.custompaintings.util;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.UUID;

public record MismatchedPainting(UUID uuid, PaintingEntity paintingRef, PaintingData currentData,
                                 PaintingData knownData) {
  public static final PacketCodec<PacketByteBuf, MismatchedPainting> PACKET_CODEC = PacketCodec.of(
      MismatchedPainting::writeToPacketByteBuf, MismatchedPainting::fromPacketByteBuf);

  public MismatchedPainting(UUID uuid, PaintingData currentData, PaintingData knownData) {
    this(uuid, null, currentData, knownData);
  }

  public MismatchedPainting(PaintingEntity paintingRef, PaintingData currentData, PaintingData knownData) {
    this(paintingRef.getUuid(), paintingRef, currentData, knownData);
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeUuid(this.uuid);
    this.currentData.writeToPacketByteBuf(buf);
    this.knownData.writeToPacketByteBuf(buf);
  }

  public static MismatchedPainting fromPacketByteBuf(PacketByteBuf buf) {
    UUID uuid = buf.readUuid();
    PaintingData currentData = PaintingData.fromPacketByteBuf(buf);
    PaintingData knownData = PaintingData.fromPacketByteBuf(buf);
    return new MismatchedPainting(uuid, currentData, knownData);
  }
}
