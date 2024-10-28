package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public record MigrationData(Identifier id, String description, HashMap<Identifier, Identifier> pairs) {
  public static final PacketCodec<PacketByteBuf, MigrationData> PACKET_CODEC = PacketCodec.of(
      MigrationData::writeToPacketByteBuf, MigrationData::fromPacketByteBuf);

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeIdentifier(this.id);
    buf.writeString(this.description);
    buf.writeInt(this.pairs.size());
    this.pairs.forEach((from, to) -> {
      buf.writeIdentifier(from);
      buf.writeIdentifier(to);
    });
  }

  public static MigrationData fromPacketByteBuf(PacketByteBuf buf) {
    Identifier id = buf.readIdentifier();
    String description = buf.readString();
    int count = buf.readInt();
    HashMap<Identifier, Identifier> pairs = new HashMap<>(count);
    for (int i = 0; i < count; i++) {
      pairs.put(buf.readIdentifier(), buf.readIdentifier());
    }
    return new MigrationData(id, description, pairs);
  }
}
