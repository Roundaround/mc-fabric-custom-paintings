package me.roundaround.custompaintings.util;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;

public record Migration(String id, String description, String packId, int index, List<Pair<String, String>> pairs) {
  public static final PacketCodec<PacketByteBuf, Migration> PACKET_CODEC = PacketCodec.of(
      Migration::writeToPacketByteBuf, Migration::fromPacketByteBuf);

  public Migration(List<Pair<String, String>> pairs) {
    this("", "", "", 0, pairs);
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeInt(this.pairs.size());
    for (Pair<String, String> pair : this.pairs) {
      buf.writeString(pair.getLeft());
      buf.writeString(pair.getRight());
    }
  }

  public static Migration fromPacketByteBuf(PacketByteBuf buf) {
    int size = buf.readInt();
    List<Pair<String, String>> pairs = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      pairs.add(new Pair<>(buf.readString(), buf.readString()));
    }
    return new Migration(pairs);
  }
}
