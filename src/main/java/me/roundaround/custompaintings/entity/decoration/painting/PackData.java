package me.roundaround.custompaintings.entity.decoration.painting;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public record PackData(String packFileUid, boolean disabled, String id, String name, String description,
                       String sourceLegacyPack, List<PaintingData> paintings, List<MigrationData> migrations) {
  public static final PacketCodec<PacketByteBuf, PackData> PACKET_CODEC = PacketCodec.of(
      PackData::writeToPacketByteBuf, PackData::fromPacketByteBuf);

  public static PackData virtual(String id, Text name, Text description, List<PaintingData> paintings) {
    return new PackData("", false, id, name.getString(), description.getString(), null, paintings, List.of());
  }

  public void writeToPacketByteBuf(PacketByteBuf buf) {
    buf.writeString(this.packFileUid);
    buf.writeBoolean(this.disabled);
    buf.writeString(this.id);
    buf.writeString(this.name);
    buf.writeString(this.description == null ? "" : this.description);
    buf.writeString(this.sourceLegacyPack == null ? "" : this.sourceLegacyPack);
    buf.writeInt(this.paintings.size());
    this.paintings.forEach((painting) -> painting.write(buf));
    buf.writeInt(this.migrations.size());
    this.migrations.forEach((migration) -> migration.writeToPacketByteBuf(buf));
  }

  public static PackData fromPacketByteBuf(PacketByteBuf buf) {
    String packFileUid = buf.readString();
    boolean disabled = buf.readBoolean();
    String id = buf.readString();
    String name = buf.readString();
    String description = buf.readString();
    String legacyPackId = buf.readString();
    int paintingCount = buf.readInt();
    List<PaintingData> paintings = new ArrayList<>(paintingCount);
    for (int i = 0; i < paintingCount; i++) {
      paintings.add(PaintingData.read(buf));
    }
    int migrationCount = buf.readInt();
    List<MigrationData> migrations = new ArrayList<>(migrationCount);
    for (int i = 0; i < migrationCount; i++) {
      migrations.add(MigrationData.fromPacketByteBuf(buf));
    }
    return new PackData(packFileUid, disabled, id, name, description, legacyPackId, paintings, migrations);
  }
}
