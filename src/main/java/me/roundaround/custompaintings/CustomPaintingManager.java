package me.roundaround.custompaintings;

import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.network.PacketByteBuf;

public class CustomPaintingManager {
  private static CustomPaintingManager INSTANCE;

  public static final TrackedDataHandler<CustomPaintingInfo> CUSTOM_PAINTING_INFO_HANDLER = new TrackedDataHandler.ImmutableHandler<CustomPaintingInfo>() {
    @Override
    public void write(PacketByteBuf packetByteBuf, CustomPaintingInfo info) {
      if (info.isEmpty()) {
        packetByteBuf.writeBoolean(false);
        return;
      }
      packetByteBuf.writeBoolean(true);
      packetByteBuf.writeString(info.getName());
      packetByteBuf.writeInt(info.getWidth());
      packetByteBuf.writeInt(info.getHeight());
    }

    @Override
    public CustomPaintingInfo read(PacketByteBuf packetByteBuf) {
      if (!packetByteBuf.readBoolean()) {
        return CustomPaintingInfo.EMPTY;
      }
      String name = packetByteBuf.readString();
      int width = packetByteBuf.readInt();
      int height = packetByteBuf.readInt();
      return new CustomPaintingInfo(name, width, height);
    }
  };

  private CustomPaintingManager() {
    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_INFO_HANDLER);
  }

  public static CustomPaintingManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new CustomPaintingManager();
    }
    return INSTANCE;
  }

  public static void init() {
    if (INSTANCE == null) {
      INSTANCE = new CustomPaintingManager();
    }
  }
}
