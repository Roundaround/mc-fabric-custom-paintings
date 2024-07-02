package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;

public record PaintingPack(String id, String name, List<PaintingData> paintings) {
  public static final PacketCodec<PacketByteBuf, PaintingPack> PACKET_CODEC = PacketCodec.of(
      PaintingPack::writeToPacketByteBuf, PaintingPack::fromPacketByteBuf);

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeString(this.id);
    buf.writeString(this.name);
    buf.writeInt(this.paintings.size());
    this.paintings.forEach((painting) -> painting.writeToPacketByteBuf(buf));
  }

  public static PaintingPack fromPacketByteBuf(PacketByteBuf buf) {
    String id = buf.readString();
    String name = buf.readString();
    int count = buf.readInt();
    List<PaintingData> paintings = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      paintings.add(PaintingData.fromPacketByteBuf(buf));
    }
    return new PaintingPack(id, name, paintings);
  }
}
