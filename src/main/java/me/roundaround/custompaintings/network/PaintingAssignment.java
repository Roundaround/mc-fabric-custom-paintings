package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.function.Function;

public class PaintingAssignment {
  public static final PacketCodec<PacketByteBuf, PaintingAssignment> PACKET_CODEC = PacketCodec.of(
      PaintingAssignment::write, PaintingAssignment::read);

  private final int paintingId;
  private final CustomId dataId;
  private final PaintingData data;

  private PaintingAssignment(int paintingId, CustomId dataId, PaintingData data) {
    this.paintingId = paintingId;
    this.dataId = dataId;
    this.data = data;
  }

  public static PaintingAssignment from(int paintingId, PaintingData data, Function<CustomId, Boolean> lookup) {
    if (data.unknown()) {
      return new PaintingAssignment(paintingId, null, data);
    } else if (!lookup.apply(data.id())) {
      return new PaintingAssignment(paintingId, null, data.markUnknown());
    }
    return new PaintingAssignment(paintingId, data.id(), null);
  }

  public boolean isKnown() {
    return this.dataId != null;
  }

  public int getPaintingId() {
    return this.paintingId;
  }

  public CustomId getDataId() {
    return this.dataId;
  }

  public PaintingData getData() {
    return this.data;
  }

  private static PaintingAssignment read(PacketByteBuf buf) {
    int paintingId = buf.readInt();
    CustomId dataId = null;
    PaintingData data = null;
    if (buf.readBoolean()) {
      dataId = CustomId.read(buf);
    } else {
      data = PaintingData.read(buf);
    }
    return new PaintingAssignment(paintingId, dataId, data);
  }

  private void write(PacketByteBuf buf) {
    buf.writeInt(this.paintingId);
    if (this.dataId != null) {
      buf.writeBoolean(true);
      this.dataId.write(buf);
    } else {
      buf.writeBoolean(false);
      this.data.write(buf);
    }
  }
}
