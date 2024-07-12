package me.roundaround.custompaintings.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

public record PaintingIdPair(int paintingId, Identifier dataId) {
  public static final PacketCodec<PacketByteBuf, PaintingIdPair> PACKET_CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
      PaintingIdPair::paintingId, Identifier.PACKET_CODEC, PaintingIdPair::dataId, PaintingIdPair::new
  );
}
