package me.roundaround.custompaintings.entity.decoration.painting;

import io.netty.buffer.ByteBuf;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.HashMap;

public record MigrationData(CustomId id, String description, HashMap<CustomId, CustomId> pairs) {
  public static final StreamCodec<ByteBuf, MigrationData> PACKET_CODEC = StreamCodec.composite(
      CustomId.PACKET_CODEC,
      MigrationData::id,
      ByteBufCodecs.STRING_UTF8,
      MigrationData::description,
      ByteBufCodecs.map(HashMap::new, CustomId.PACKET_CODEC, CustomId.PACKET_CODEC),
      MigrationData::pairs,
      MigrationData::new
  );
}
