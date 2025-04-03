package me.roundaround.custompaintings.entity.decoration.painting;

import io.netty.buffer.ByteBuf;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.HashMap;

public record MigrationData(CustomId id, String description, HashMap<CustomId, CustomId> pairs) {
  public static final PacketCodec<ByteBuf, MigrationData> PACKET_CODEC = PacketCodec.tuple(
      CustomId.PACKET_CODEC,
      MigrationData::id,
      PacketCodecs.STRING,
      MigrationData::description,
      PacketCodecs.map(HashMap::new, CustomId.PACKET_CODEC, CustomId.PACKET_CODEC),
      MigrationData::pairs,
      MigrationData::new
  );
}
