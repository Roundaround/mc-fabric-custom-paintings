package me.roundaround.custompaintings.entity.decoration.painting;

import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.HashMap;

public record MigrationData(CustomId id, String description, HashMap<CustomId, CustomId> pairs) {
  public static final PacketCodec<PacketByteBuf, MigrationData> PACKET_CODEC = PacketCodec.of(
      MigrationData::write, MigrationData::read);

  public void write(PacketByteBuf buf) {
    this.id.write(buf);
    buf.writeString(this.description == null ? "" : this.description);
    buf.writeInt(this.pairs.size());
    this.pairs.forEach((from, to) -> {
      from.write(buf);
      to.write(buf);
    });
  }

  public static MigrationData read(PacketByteBuf buf) {
    CustomId id = CustomId.read(buf);
    String description = buf.readString();
    int count = buf.readInt();
    HashMap<CustomId, CustomId> pairs = new HashMap<>(count);
    for (int i = 0; i < count; i++) {
      pairs.put(CustomId.read(buf), CustomId.read(buf));
    }
    return new MigrationData(id, description, pairs);
  }
}
